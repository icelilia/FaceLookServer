package redis.redis;

import redis.activeMq.MyActiveMqConnection;
import redis.clients.jedis.Jedis;
import redis.entity.Content;

import javax.jms.*;
import java.util.List;

public abstract class MyRedisUtil {
    public static MyRedisUtil INSTANCE = new ContentRedisUtil();

    public abstract void store(String key, Object content);
    public abstract List<Content> fetch(String key);
    //作用是确定key是否存在于redis
    public boolean fetchKey(String key){
        Jedis jedis = MyJedisPool.INSTANCE.getJedis();
        boolean ans = jedis.exists(key);
        jedis.close();
        return ans;
    }
    //作用是发送错误信息
    public void sendError(String content) throws JMSException {
        Connection connection = MyActiveMqConnection.INSTANCE.getConnection();
        Session session = connection
                .createSession(Boolean.FALSE, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue("error");
        MessageProducer producer = session.createProducer(destination);
        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        @SuppressWarnings("unused")
		TextMessage message = session.createTextMessage(content);
    }
    //作用是标记一个数据已写入
    public void mark(String key){
        Jedis jedis = MyJedisPool.INSTANCE.getJedis();
        jedis.set(key, "written");
        jedis.close();
    }
}
