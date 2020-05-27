package main;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import dataBase.DataBase;
import dataBase.model.Friend;
import dataBase.model.User;

public class Message {
	private String messageNumber = ""; // 对应开发文档里的编号
	private String messageFiled1 = ""; // 字段1
	private String messageFiled2 = ""; // 字段2
	private String messageFiled3 = ""; // 字段3

	public String getMessageNumber() {
		return messageNumber;
	}

	public void setMessageNumber(String messageNumber) {
		this.messageNumber = messageNumber;
	}

	public String getMessageFiled1() {
		return messageFiled1;
	}

	public void setMessageFiled1(String messageFiled1) {
		this.messageFiled1 = messageFiled1;
	}

	public String getMessageFiled2() {
		return messageFiled2;
	}

	public void setMessageFiled2(String messageFiled2) {
		this.messageFiled2 = messageFiled2;
	}

	public String getMessageFiled3() {
		return messageFiled3;
	}

	public void setMessageFiled3(String messageFiled3) {
		this.messageFiled3 = messageFiled3;
	}

	public boolean message2(DataBase dataBase, DataOutputStream dataOutputStream) throws IOException {
		String username = getMessageFiled1();
		String password = getMessageFiled2();
		Message message;
		if (!dataBase.checkLogin(username, password)) {
			message = new Message();
			message.setMessageNumber("2");
			message.setMessageFiled1("0");
			message.setMessageFiled2("用户名或密码错误");
			dataOutputStream.writeUTF(JSON.toJSONString(message));
			return false;
		} else {
			message = new Message();
			message.setMessageNumber("2");
			message.setMessageFiled1("1");
			message.setMessageFiled2("OK");
			dataOutputStream.writeUTF(JSON.toJSONString(message));
			return true;
		}
	}

	private boolean checkForm(String string) {
		String pattern = "[A-Za-z0-9_]{9,16}";
		return Pattern.matches(pattern, string);
	}

	public boolean message3(DataBase dataBase, DataOutputStream dataOutputStream) throws IOException {
		String username = getMessageFiled1();
		String password = getMessageFiled2();
		String nickname = getMessageFiled3();
		Message message = new Message();
		if (!checkForm(username)) {
			message.setMessageNumber("3");
			message.setMessageFiled1("0");
			message.setMessageFiled2("用户名格式非法");
			dataOutputStream.writeUTF(JSON.toJSONString(message));
			return false;
		} else if (!checkForm(password)) {
			message.setMessageNumber("3");
			message.setMessageFiled1("0");
			message.setMessageFiled2("密码格式非法");
			dataOutputStream.writeUTF(JSON.toJSONString(message));
			return false;
		} else if (!dataBase.checkUsernameUniqueness(username)) {
			message.setMessageNumber("3");
			message.setMessageFiled1("0");
			message.setMessageFiled2("用户名已被占用");
			dataOutputStream.writeUTF(JSON.toJSONString(message));
			return false;
		} else {
			User user = new User(username, password, nickname);
			dataBase.registerUser(user);
			message.setMessageNumber("3");
			message.setMessageFiled1("1");
			message.setMessageFiled2("OK");
			dataOutputStream.writeUTF(JSON.toJSONString(message));
			return true;
		}
	}

	public void message4(DataBase dataBase, String username, DataOutputStream dataOutputStream) throws IOException {
		Vector<Friend> friends = dataBase.getFriends(username);
		Vector<Message> messageArray = new Vector<Message>();
		Message message;
		for (Friend friend : friends) {
			message = new Message();
			message.setMessageNumber("4");
			message.setMessageFiled1(friend.getUsername());
			message.setMessageFiled2(friend.getNickname());
			messageArray.add(message);
		}
		dataOutputStream.writeUTF(JSONArray.toJSONString(messageArray));
	}

	// 难点
	public void message5() {

	}

	public void message6(DataBase dataBase, String username, DataOutputStream dataOutputStream) throws IOException {
		int sessionId = dataBase.createSession(username);
		Message message = new Message();
		message.setMessageNumber("6");
		message.setMessageFiled1(String.valueOf(sessionId));
		dataOutputStream.writeUTF(JSON.toJSONString(message));
	}

	public void message7(DataBase dataBase,  DataOutputStream dataOutputStream) throws IOException {
		String username = getMessageFiled1();
		int sessionId = Integer.parseInt(getMessageFiled2());
		Message message = new Message();
		if (dataBase.joinSession(username, sessionId)) {
			message.setMessageNumber("7");
			message.setMessageFiled1("OK");
			dataOutputStream.writeUTF(JSON.toJSONString(message));
		} else {
			message.setMessageNumber("7");
			message.setMessageFiled1("未知错误");
			dataOutputStream.writeUTF(JSON.toJSONString(message));
		}
		
	}
}
