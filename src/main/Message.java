package main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Vector;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import dataBase.DataBase;
import dataBase.model.Friend;
import dataBase.model.User;

public class Message {
	private String messageNumber = ""; // 对应开发文档里的编号
	private String messageField1 = ""; // 字段1
	private String messageField2 = ""; // 字段2
	private String messageField3 = ""; // 字段3

	public Message() {
	}

	public Message(String messageNumber) {
		this.messageNumber = messageNumber;
	}

	public String getMessageNumber() {
		return messageNumber;
	}

	public void setMessageNumber(String messageNumber) {
		this.messageNumber = messageNumber;
	}

	public String getMessageField1() {
		return messageField1;
	}

	public void setMessageField1(String messageField1) {
		this.messageField1 = messageField1;
	}

	public String getMessageField2() {
		return messageField2;
	}

	public void setMessageField2(String messageField2) {
		this.messageField2 = messageField2;
	}

	public String getMessageField3() {
		return messageField3;
	}

	public void setMessageField3(String messageField3) {
		this.messageField3 = messageField3;
	}

	private void sendMessageAsByteArray(DataOutputStream dataOutputStream, Message message) throws IOException {
		byte[] messageByteArray;
		messageByteArray = JSON.toJSONString(message).getBytes("utf-8");
		dataOutputStream.write(messageByteArray);
	}

	public static Message receiveMessage(DataBase dataBase, DataInputStream dataInputStream) throws IOException {
		// 裸奔版
		final int MAX_SIZE = 0xffff;
		byte[] messageByteArray = new byte[MAX_SIZE];
		dataInputStream.read(messageByteArray);
		System.out.println(new String(messageByteArray, "utf-8"));
		return JSON.parseObject(new String(messageByteArray, "utf-8"), Message.class);

		// 首部长度校验版
		// int length = dataInputStream.readInt();
		// byte[] messageByteArray = new byte[length];
		// dataInputStream.read(messageByteArray);
		// return JSON.parseObject(new String(messageByteArray, "utf-8"),
		// Message.class);
	}

	public boolean message1(DataBase dataBase, DataOutputStream dataOutputStream) throws IOException {
		Message message = new Message();
		message.setMessageNumber("1r");
		sendMessageAsByteArray(dataOutputStream, message);
		return true;
	}

	// 登录
	public boolean message2(DataBase dataBase, DataOutputStream dataOutputStream) throws IOException {
		// 先获得用户名和密码
		String username = getMessageField1();
		String password = getMessageField2();

		Message message = new Message("2r");

		// 检验登录
		if (!dataBase.checkLogin(username, password)) {
			message.setMessageField1("0");
			message.setMessageField2("用户名或密码错误");
			sendMessageAsByteArray(dataOutputStream, message);
			return false;
		} else {
			// 检验该用户是否已经登录
			if (dataBase.searchSocketByUsername(username) != null) {
				message.setMessageField1("0");
				message.setMessageField2("该用户已登录");
				sendMessageAsByteArray(dataOutputStream, message);
				return true;
			}
			message.setMessageField1("1");
			message.setMessageField2("OK");
			sendMessageAsByteArray(dataOutputStream, message);
			return true;
		}
	}

	// 注册
	public boolean message3(DataBase dataBase, DataOutputStream dataOutputStream) throws IOException {
		// 获得用户名、密码和昵称
		String username = getMessageField1();
		String password = getMessageField2();
		String nickname = getMessageField3();

		Message message = new Message("3r");

		String pattern1 = "[A-Za-z0-9_]{6,10}";
		String pattern2 = "[A-Za-z0-9_]{9,16}";

		if (!Pattern.matches(pattern1, username)) {
			message.setMessageField1("0");
			message.setMessageField2("用户名格式非法");
			sendMessageAsByteArray(dataOutputStream, message);
			return false;
		} else if (!Pattern.matches(pattern2, password)) {
			message.setMessageField1("0");
			message.setMessageField2("密码格式非法");
			sendMessageAsByteArray(dataOutputStream, message);
			return false;
		} else if (!dataBase.checkUsernameUniqueness(username)) {
			message.setMessageField1("0");
			message.setMessageField2("用户名已被占用");
			sendMessageAsByteArray(dataOutputStream, message);
			return false;
		} else {
			User user = new User(username, password, nickname);
			dataBase.registerUser(user);
			message.setMessageField1("1");
			message.setMessageField2("OK");
			sendMessageAsByteArray(dataOutputStream, message);
			return true;
		}
	}

	// 请求好友列表
	public void message4(DataBase dataBase, String username, DataOutputStream dataOutputStream) throws IOException {
		// 先获得所有的好友对象
		Vector<Friend> friends = dataBase.getFriends(username);

		Vector<Message> messageArray = new Vector<Message>();
		Message message;
		byte[] messageByteArray;

		for (Friend friend : friends) {
			message = new Message("4r");
			message.setMessageField1(friend.getUsername());
			message.setMessageField2(friend.getNickname());
			messageArray.add(message);
		}
		// 数组对象特殊处理
		messageByteArray = JSONArray.toJSONString(messageArray).getBytes("utf-8");
		dataOutputStream.write(messageByteArray);
	}

	// 难点
	public void message5() {

	}

	// 创建会话
	public void message6(DataBase dataBase, String username, DataOutputStream dataOutputStream) throws IOException {
		int sessionId = dataBase.createSession(username);
		Message message = new Message("6r");
		message.setMessageField1(String.valueOf(sessionId));
		sendMessageAsByteArray(dataOutputStream, message);
	}

	// 加入会话
	public void message7(DataBase dataBase, DataOutputStream dataOutputStream) throws IOException {
		// 目标用户
		String username = getMessageField1();
		// 目标会话
		int sessionId = Integer.parseInt(getMessageField2());
		Message message = new Message("7r");

		if (dataBase.joinSession(username, sessionId)) {
			message.setMessageField1("OK");
			sendMessageAsByteArray(dataOutputStream, message);
		} else {
			message.setMessageField1("未知错误");
			sendMessageAsByteArray(dataOutputStream, message);
		}

	}

	public void message9(DataBase dataBase, String senderUsername) throws IOException {
		// 服务器收到这种包后，应该将其转发给会话中除了发送者外所有的用户
		// 内容除了messageNumber外不会变
		setMessageNumber("9r");

		// 获得该会话中的所有用户
		Vector<String> users = dataBase.getUsers(Integer.parseInt(getMessageField1()));

		Socket socket;
		OutputStream outputStream;
		DataOutputStream dataOutputStream;

		// 转发给会话中除了发送者外所有的用户
		for (String username : users) {
			if (!username.contentEquals(senderUsername)) {
				socket = dataBase.searchSocketByUsername(username);
				outputStream = socket.getOutputStream();
				dataOutputStream = new DataOutputStream(outputStream);
				sendMessageAsByteArray(dataOutputStream, this);
			}
		}
	}
}
