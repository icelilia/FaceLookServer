package dataBase.entity;

import java.util.Vector;

public class Session {
	public static int sessionNum = 0;

	private int sessionId = sessionNum;

	private String sessionName;

	private String managerUsername;

	private Vector<String> sessionMembers = new Vector<String>();

	private String contents;

	public int getSessionId() {
		return sessionId;
	}

	public void setSessionId(int sessionId) {
		this.sessionId = sessionId;
	}

	public String getSessionName() {
		return sessionName;
	}

	public void setSessionName(String sessionName) {
		this.sessionName = sessionName;
	}

	public String getManagerUsername() {
		return managerUsername;
	}

	public void setManagerUsername(String managerUsername) {
		this.managerUsername = managerUsername;
	}

	synchronized public Vector<String> getSessionMembers() {
		return sessionMembers;
	}

	synchronized public void setSessionMembers(Vector<String> sessionMembers) {
		this.sessionMembers = sessionMembers;
	}

	// 向对话中添加新的用户
	synchronized public void addSessionMember(String sessionMemberUsername) {
		this.sessionMembers.add(sessionMemberUsername);
	}

	public String getContents() {
		return contents;
	}

	public void setContents(String contents) {
		this.contents = contents;
	}
}
