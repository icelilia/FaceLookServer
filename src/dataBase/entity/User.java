package dataBase.entity;

import java.util.Vector;

public class User {
	private String username;
	private String password;
	private String nickname;

	// 好友列表
	private Vector<String> friends = new Vector<String>();

	// 会话列表
	private Vector<Integer> sessions = new Vector<Integer>();

	// 好友请求列表
	private Vector<FriendRequest> requests = new Vector<FriendRequest>();

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

	synchronized public String getNickname() {
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

	public Vector<Integer> getSessions() {
		return sessions;
	}

	public void setSessions(Vector<Integer> sessions) {
		this.sessions = sessions;
	}

	public void addSession(int sessionId) {
		this.sessions.add(sessionId);
	}

	public Vector<FriendRequest> getRequests() {
		return requests;
	}

	public void setRequests(Vector<FriendRequest> requests) {
		this.requests = requests;
	}

	public void addRequest(FriendRequest request) {
		this.requests.add(request);
	}
}
