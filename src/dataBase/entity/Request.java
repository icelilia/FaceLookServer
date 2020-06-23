package dataBase.entity;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Request {
	private String requestorUsername;
	private String avatarAddress;
	private String checkMessage;
	private String time;

	public Request() {

	}

	public Request(String requestorUsername, String avatarAddress, String checkMessage, Date date) {
		this.requestorUsername = requestorUsername;
		this.avatarAddress = avatarAddress;
		this.checkMessage = checkMessage;
		final SimpleDateFormat dateForm = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		this.setTime(dateForm.format(date));
	}

	public String getRequestorUsername() {
		return requestorUsername;
	}

	public void setRequestorUsername(String username) {
		this.requestorUsername = username;
	}

	public String getAvatarAddress() {
		return avatarAddress;
	}

	public void setAvatarAddress(String avatarAddress) {
		this.avatarAddress = avatarAddress;
	}

	public String getCheckMessage() {
		return checkMessage;
	}

	public void setCheckMessage(String checkMessage) {
		this.checkMessage = checkMessage;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}
}
