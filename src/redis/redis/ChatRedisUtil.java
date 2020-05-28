package redis.redis;

import redis.entity.Chat;
import redis.entity.ChatUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;

public class ChatRedisUtil extends RedisUtil{
    JedisPool pool;
    Logger logger;

    public ChatRedisUtil() {
        logger = LoggerFactory.getLogger(ChatRedisUtil.class);
    }

    public String store(Object object, String key) {
        Chat chat = (Chat)object;
        Jedis jedis = MyJedisPool.INSTANCE.getJedis();
        jedis.hmset(key, ChatUtil.INSTANCE.toMap(chat));
        jedis.close();
//        logger.debug("store " + key);

        jedis.set("mark" + key, "written");
        return key;
    }

    public Object fetch(String key) {
        Jedis jedis = MyJedisPool.INSTANCE.getJedis();
        Map<String, String> chat = jedis.hgetAll(key);
        jedis.close();
//        logger.debug("fetch " + key);

        return ChatUtil.INSTANCE.parse(chat);
    }
}
