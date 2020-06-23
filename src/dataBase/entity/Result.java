package dataBase.entity;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Result {
	private String receiverUsername;
	private String avatarAddress;
	private String result;
	private String time;

	public Result() {

	}

	public Result(String receiverUsername, String avatarAddress, String result, Date date) {
		this.receiverUsername = receiverUsername;
		this.avatarAddress = avatarAddress;
		this.result = result;
		final SimpleDateFormat dateForm = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		this.time = dateForm.format(date);
	}

	public String getReceiverUsername() {
		return receiverUsername;
	}

	public void setReceiverUsername(String username) {
		this.receiverUsername = username;
	}

	public String getAvatarAddress() {
		return avatarAddress;
	}

	public void setAvatarAddress(String avatarAddress) {
		this.avatarAddress = avatarAddress;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}
}
