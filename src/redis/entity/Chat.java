package redis.entity;

import java.io.Serializable;

public class Chat implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 8459789330635891615L;
	String from;
    String to;
    String time;
    String content;

    public Chat(String from, String to, String time, String content) {
        this.from = from;
        this.to = to;
        this.time = time;
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "Chat{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", time='" + time + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
