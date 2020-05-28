package redis;

import redis.activeMq.ObjectToKey;
import redis.entity.Content;

import javax.jms.JMSException;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Redis {
	public static void send(String uuid, int sessionId, String from, String to, Date date, String message)
			throws JMSException {
		// String uuid = UUID.randomUUID().toString();
		final SimpleDateFormat dateForm = new SimpleDateFormat("yyyy-mm-dd-hh-mm-ss");
		String sessionIdString = String.valueOf(sessionId);
		Content raw = new Content(sessionIdString, from, to, dateForm.format(date), message);
		ObjectToKey.INSTANCE.sendObject(raw, sessionIdString);
	}

	public static String receive(String uuid, int sessionId) throws JMSException {
		String sessionIdString = String.valueOf(sessionId);
		return (String) ObjectToKey.INSTANCE.getObject(uuid, sessionIdString);
	}
}
