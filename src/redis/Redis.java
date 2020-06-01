package redis;

import redis.entity.Content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import redis.redis.ContentRedisUtil;

import java.util.ArrayList;

public class Redis {
	static Logger logger;

	static {
		logger = LoggerFactory.getLogger(Redis.class);
	}

	/**
	 * 按会话编号，将聊天记录储存在redis中
	 * 
	 * @param sessionId   会话编号
	 * @param from        发送者用户名
	 * @param to          被@对象用户名，无则为空字符串
	 * @param time        发送时的客户端本地时间
	 * @param messageText 消息文本
	 */
	public static void send(int sessionId, String from, String to, String time, String messageText) {
		String sessionIdString = String.valueOf(sessionId);
		Content content = new Content(from, to, time, messageText);
		sendObject(content, sessionIdString);
	}

	/**
	 * 按会话编号，将聊天记录储存在redis中
	 * 
	 * @param sessionId 会话编号
	 * @param content   Content对象的引用
	 */
	public static void send(int sessionId, Content content) {
		String sessionIdString = String.valueOf(sessionId);
		sendObject(content, sessionIdString);
	}

	public static Object receive(int sessionId) {
		String sessionIdString = String.valueOf(sessionId);

		return getObject(sessionIdString);
	}

	private static void sendObject(Object obj, String uuid) {
		ContentRedisUtil.INSTANCE.store(uuid, obj);
		logger.info("send " + obj);
	}

	private static String getObject(String key) {
		ArrayList<Content> messageList = ContentRedisUtil.INSTANCE.fetch(key);

		JSONArray list = new JSONArray();

		for (Content content : messageList) {
			JSONObject json = new JSONObject();
			json.put("content", content.getContent());
			json.put("from", content.getFrom());
			json.put("time", content.getTime());
			json.put("to", content.getTo());
			list.add(json);
		}

		logger.info("get" + list.toJSONString());

		return list.toJSONString();
	}
}
