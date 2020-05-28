package redis.activeMq;

import javax.jms.MessageConsumer;
import java.util.Hashtable;

public class Consumer {
    public static Consumer INSTANCE = new Consumer();
    public Hashtable<String, MessageConsumer> consumerHashMap;

    public Consumer() {
        this.consumerHashMap = new Hashtable<String, MessageConsumer>();
    }

    public MessageConsumer getConsumer(String key){
        return consumerHashMap.get(key);
    }

    public void injectConsumer(String key, MessageConsumer consumer){
        consumerHashMap.put(key, consumer);
    }
}
