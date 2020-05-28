package redis.entity;

import java.util.HashMap;
import java.util.Map;

public class ChatUtil {
    public static ChatUtil INSTANCE = new ChatUtil();

    public Chat parse(Map<String, String> map){
        return new Chat(map.get("from"), map.get("to"), map.get("time"),
                map.get("content"));
    }

    public Map<String, String> toMap(Chat chat){
        Map<String, String> map = new HashMap<String, String>();
        map.put("from", chat.getFrom());
        map.put("to", chat.getTo());
        map.put("time", chat.getTime());
        map.put("content", chat.getContent());
        return map;
    }
}
