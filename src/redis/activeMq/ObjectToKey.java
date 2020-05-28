package redis.activeMq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.Serializable;

interface ControlType {
	int SEND = 0;
	int GET = 1;
}

//有两个控制队列，一个是sendControl，一个是getControl，发送和接收都需要发消息然后在对应队列发东西
//站在使用者的角度，发送一个object以及自带的uuid，这个object被存放于redis，使用者后续通过uuid得到object
//因此使用者作为producer发送object,而且需要额外告知接收方所在的队列
//使用者作为consumer发送uuid，
public class ObjectToKey {
	public static ObjectToKey INSTANCE;
	static Logger logger;

	static {
		try {
			INSTANCE = new ObjectToKey();
			logger = LoggerFactory.getLogger(ObjectToKey.class);
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	Connection connection;

	public ObjectToKey() throws JMSException {
		connection = MyActiveMqConnection.INSTANCE.getConnection();
	}

	public void sendObject(Serializable obj, String uuid) throws JMSException {
		Session session = connection.createSession(Boolean.FALSE, Session.AUTO_ACKNOWLEDGE);

		// 发送控制消息
		sendMessageToControlQueue(ControlType.SEND, uuid, session);

		// 发送数据
		Destination destination = session.createQueue("send" + uuid.toString());
		MessageProducer producer = session.createProducer(destination);
		producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		ObjectMessage objectMessage = session.createObjectMessage(obj);
		producer.send(objectMessage);

		logger.info("send" + obj);

//        connection.close();
	}

	// 前端请求数据时要把自己的uuid给出去，后端查找key值并返回前端
	public Object getObject(String uuid, String key) throws JMSException {
		Session session = connection.createSession(Boolean.FALSE, Session.AUTO_ACKNOWLEDGE);

		// 发送控制消息,额外携带key信息
		sendMessageToControlQueue(ControlType.GET, uuid + "€" + key, session);

		// 接收数据
		Object object = null;
		Destination destination = session.createQueue("get" + uuid);

		MessageConsumer consumer = Consumer.INSTANCE.getConsumer("get" + uuid);
		if (consumer == null) {
			consumer = session.createConsumer(destination);
			Consumer.INSTANCE.injectConsumer("get" + uuid, consumer);
		}

		ObjectMessage message = (ObjectMessage) consumer.receive();
		object = message.getObject();

		logger.info("get" + object);

//        connection.close();

		return object;
	}

	private void sendMessageToControlQueue(int type, String uuid, Session session) throws JMSException {
		Destination destination = null;
		destination = switchControlQueue(type, session);
		MessageProducer producer = session.createProducer(destination);
		producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		TextMessage message = session.createTextMessage(uuid.toString());
		producer.send(message);
	}

	private Destination switchControlQueue(int type, Session session) throws JMSException {
		switch (type) {
		case ControlType.SEND:
			return session.createQueue("sendControl");
		case ControlType.GET:
			return session.createQueue("getControl");
		}
		return null;
	}
}
