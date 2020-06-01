package dataBase.entity;

public class FriendRequest {
	private String username;
	private String checkMessage;

	public FriendRequest() {

	}

	public FriendRequest(String username, String checkMessage) {
		this.username = username;
		this.checkMessage = checkMessage;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getCheckMessage() {
		return checkMessage;
	}

	public void setCheckMessage(String checkMessage) {
		this.checkMessage = checkMessage;
	}
}
