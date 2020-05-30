package redis;

import javax.jms.JMSException;
import java.util.UUID;

public class Receive {
	public static void main(String[] args) throws JMSException {
		String self = UUID.randomUUID().toString();
		int sessionID = 123456;
		System.out.println(Redis.receive(self, sessionID));
	}
}
