package redis;

import redis.activeMq.MyActiveMqConnection;

import javax.jms.JMSException;
import java.util.Date;
import java.util.UUID;

public class frontEnd {
	public static void main(String[] args) throws JMSException {
		String self = UUID.randomUUID().toString();
		int sessionID = 123456;
		String from = "lzx";
		String to = "xz";
		Date date = new Date();
		String message = "how dare you";
		Redis.send(self, sessionID, from, to, date, message);

		from = "wwk";
		to = "ay";
		date = new Date();
		message = "17 cards, can you win me";
		Redis.send(self, sessionID, from, to, date, message);

		MyActiveMqConnection.INSTANCE.getConnection().close();

//        System.out.println(Redis.receive(self, sessionID));
	}
}
