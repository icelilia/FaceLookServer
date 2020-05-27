package dataBase.model;

import java.util.Vector;

public class Session {
	public static int sessionNum = 0;

	private int sessionId = sessionNum;

	// 非群聊对话应该占大多数，默认初始成员数为2，节省空间
	private Vector<String> sessionMembers = new Vector<String>(2);

	public int getSessionId() {
		return sessionId;
	}

	public void setSessionId(int sessionId) {
		this.sessionId = sessionId;
	}

	// 获得对话中的用户列表
	synchronized public Vector<String> getSessionMembers() {
		return sessionMembers;
	}

	// 这个应该只用于多个用户共同新建一个Session时（多人建群）
	synchronized public void setSessionMembers(Vector<String> sessionMembers) {
		this.sessionMembers = sessionMembers;
	}

	// 向对话中添加新的用户
	synchronized public void addSessionMember(String sessionMemberUsername) {
		this.sessionMembers.add(sessionMemberUsername);
	}
}
