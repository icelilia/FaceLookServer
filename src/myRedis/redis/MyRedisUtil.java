package myRedis.redis;

import java.util.ArrayList;

import myRedis.entity.Content;

public abstract class MyRedisUtil {
	public static MyRedisUtil INSTANCE = new ContentRedisUtil();

	public abstract void store(String key, Object content);

	public abstract ArrayList<Content> fetch(String key);

	public abstract void delObject(String key);

	public abstract void delAll();
}
