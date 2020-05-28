package redis;

import redis.activeMq.KeyToObject;

public class backEnd {
	public static void main(String[] args) {
		KeyToObject.INSTANCE.start();
	}
}
