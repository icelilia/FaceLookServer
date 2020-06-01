package redis.entity;

public class Content {
	String from;
	String to;
	String time;
	String content;

	public Content(String from, String to, String time, String content) {
		this.from = from;
		this.to = to;
		this.time = time;
		this.content = content;
	}

	@Override
	public String toString() {
		return "Content{" + "from='" + from + '\'' + ", to='" + to + '\'' + ", time='" + time + '\'' + ", content='"
				+ content + '\'' + '}';
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
