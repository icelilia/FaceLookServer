package dataBase.model;

import java.util.Vector;

public class User {
	private String username;
	private String password;
	private String nickname;
	private Vector<String> friends = new Vector<String>(4);
	private Vector<Integer> sessionIds = new Vector<Integer>(4);

	public User() {
	}

	public User(String username, String password, String nickname) {
		this.username = username;
		this.password = password;
		this.nickname = nickname;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public Vector<String> getFriends() {
		return friends;
	}

	public void setFriends(Vector<String> friends) {
		this.friends = friends;
	}

	public void addFriend(String friendUsername) {
		this.friends.add(friendUsername);
	}

	public Vector<Integer> getSessionIds() {
		return sessionIds;
	}

	public void setSessionIds(Vector<Integer> sessionIds) {
		this.sessionIds = sessionIds;
	}

	public void addSession(int sessionId) {
		this.sessionIds.add(sessionId);
	}
}
