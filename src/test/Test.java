package test;

import java.util.Date;
import java.util.UUID;

import javax.jms.JMSException;

import redis.Redis;

public class Test {
	public static void main(String[] args) throws JMSException {
		String uuid = UUID.randomUUID().toString();
		int sessionId = 233;
		String from = "Andersen";
		String to = "Icelilia";
		Date date = new Date();
		String message = "how old are you?";
		Redis.send(uuid, sessionId, from, to, date, message);
		
		from = "Icelilia";
		to = "Andersen";
		date = new Date();
		message = "Yeah.";
		Redis.send(uuid, sessionId, from, to, date, message);
		
		System.out.println(Redis.receive(uuid, sessionId));
	}
}
