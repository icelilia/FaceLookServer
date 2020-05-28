package redis.entity;

//sessionId
//发送放
//接收方
//时间 yyyy-mm-dd-hh-mm-ss
//内容

import java.io.Serializable;

public class Content implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = -6351399744375196027L;
	String sessionId;
    String from;
    String to;
    String time;
    String content;

    public Content(String sessionId, String from, String to, String time, String content) {
        this.sessionId = sessionId;
        this.from = from;
        this.to = to;
        this.time = time;
        this.content = content;
    }

    @Override
    public String toString() {
        return "Content{" +
                "sessionId='" + sessionId + '\'' +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", time='" + time + '\'' +
                ", content='" + content + '\'' +
                '}';
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
