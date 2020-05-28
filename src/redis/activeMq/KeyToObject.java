package redis.activeMq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.redis.ContentRedisUtil;
import redis.redis.MyRedisUtil;
import javax.jms.*;
import java.io.Serializable;

//通过queue队列得到放了东西的队列，然后取出来处理
public class KeyToObject {

    public static KeyToObject INSTANCE;

    static {
        try {
            INSTANCE = new KeyToObject();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    Connection connection;
    Logger logger;

    public KeyToObject() throws JMSException {
        connection = MyActiveMqConnection.INSTANCE.getConnection();
        logger = LoggerFactory.getLogger(KeyToObject.class);
    }

    //注意，只有同时满足两个条件才能去redis取数据
    //1、收到取的请求
    //2、数据已经写入redis
    //其中2用redis的的key-value来确保，只有一个key对应true才能取，否则应等待，等待最多5s
    public void waitForGet() throws JMSException {
        Session session = connection
                .createSession(Boolean.FALSE, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue("getControl");
        MessageConsumer consumer = session.createConsumer(destination);
        while (true){
            TextMessage message = (TextMessage) consumer.receive();
            String uuid = message.getText();

            logger.info("get command from " + uuid);

            try {
                sendObject(uuid, session);
            }
            catch (Exception e){
                MyRedisUtil.INSTANCE
                        .sendError(e.getMessage());
            }
        }
        //connection.close();
    }

    //写入redis之后要在redis内标记
    public void waitForSend() throws JMSException {
        Session session = connection
                .createSession(Boolean.FALSE, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue("sendControl");
        MessageConsumer consumer = session.createConsumer(destination);
        while (true){
            TextMessage message = (TextMessage) consumer.receive();
            String uuid = message.getText();

            logger.info("send command from " + uuid);

            getObject(uuid, session);
        }
        //connection.close();
    }

    //从redis拿到数据发出去
    //需要满足的条件是这个东西已经写入redis
    public void sendObject(String uuid, Session session) throws Exception {
        //判断是否写入过
        //五次失败即认为没有该数据

        String[] seg = uuid.split("€");
        uuid = seg[0];
        String key = seg[1];

        int cnt = 5;
        while(!ContentRedisUtil.INSTANCE.fetchKey("mark" + key)){
            Thread.sleep(1000);
            cnt -= 1;
            if(cnt <= 0){
                logger.info("no such key:" + key);
//                throw new Exception("no such key:" + uuid);
                return;
            }
        }

        Object object = ContentRedisUtil.INSTANCE.fetch(key);
        //发送指定uuid的数据
        Destination destination = session.createQueue("get" + uuid);
        MessageProducer producer = session.createProducer(destination);
        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        ObjectMessage message = session.createObjectMessage((Serializable) object);
        producer.send(message);

        logger.info("send object " + object.toString());

    }

    //把数据存放到redis
    //写完后需要标记
    //如果是同一个信道，要用同一个consumer接受，要不然分配时两个都分配到第一个去了，造成死锁
    public void getObject(String uuid, Session session) throws JMSException {
        //从指定队列拿到Object
        Destination destination = session.createQueue("send" + uuid);

        //先看是否创建过消费者
        MessageConsumer consumer = Consumer.INSTANCE.getConsumer("send" + uuid);
        if(consumer == null){
            consumer = session.createConsumer(destination);
            Consumer.INSTANCE.injectConsumer("send" + uuid, consumer);
        }

        ObjectMessage message = (ObjectMessage) consumer.receive();
        Object object = message.getObject();

        logger.info("get object " + object);

        //存放并标记
        ContentRedisUtil.INSTANCE.store(uuid, object);

        logger.info("store object " + object);
    }

    public void start(){
        //开启监听get信道的线程
        new Thread(new Runnable(){
            public void run() {
                try {
                    waitForGet();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();

        //开启监听send信道的线程
        new Thread(new Runnable(){
            public void run() {
                try {
                    waitForSend();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
