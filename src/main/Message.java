package main;

import java.io.IOException;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import java.net.Socket;

import java.util.Vector;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import dataBase.DataBase;
import dataBase.entity.*;

import redis.Redis;
import redis.entity.Content;

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

	/**
	 * 初始化，1号消息的处理方法。
	 * 
	 * @param dataBase         数据库对象引用
	 * @param dataOutputStream 输出流对象引用
	 * @throws IOException 流IO错误
	 */
	public void message1(DataBase dataBase, DataOutputStream dataOutputStream) throws IOException {
		Message message = new Message();
		message.setMessageNumber("1r");
		sendMessageAsByteArray(dataOutputStream, message);
	}

	/**
	 * 登录，2号消息的处理方法。
	 * 
	 * @param dataBase         数据库对象引用
	 * @param dataOutputStream 输出流对象引用
	 * @return true：登录成功；false：登录失败
	 * @throws IOException 流IO错误
	 */
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

	/**
	 * 注册，3号消息的处理方法。
	 * 
	 * @param dataBase         数据库对象引用
	 * @param dataOutputStream 输出流对象引用
	 * @return true：注册成功；false注册失败
	 * @throws IOException 流IO错误
	 */
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

	/**
	 * 请求好友列表，4号消息的处理方法。
	 * 
	 * @param dataBase         数据库对象引用
	 * @param username         被请求的用户名
	 * @param dataOutputStream 输出流对象引用
	 * @throws IOException 流IO错误
	 */
	public void message4(DataBase dataBase, String username, DataOutputStream dataOutputStream) throws IOException {
		// 先获得所有的好友对象
		Vector<Friend> friends = dataBase.getFriends(username);

		Vector<Message> messages = new Vector<Message>();
		Message message;

		for (Friend friend : friends) {
			message = new Message("4r");
			message.setMessageField1(friend.getUsername());
			message.setMessageField2(friend.getNickname());
			messages.add(message);
		}
		// 数组对象特殊处理
		byte[] messageByteArray = JSONArray.toJSONString(messages).getBytes("utf-8");
		dataOutputStream.write(messageByteArray);
	}

	/**
	 * 获取历史聊天记录，5号消息的处理方法。
	 * 
	 * @param dataBase         数据库对象引用
	 * @param dataOutputStream 输出流对象引用
	 * @param username         被请求的用户名
	 * @throws IOException 流IO错误
	 */
	public void message5(DataBase dataBase, DataOutputStream dataOutputStream, String username) throws IOException {
		// 获取用户所有的session
		Vector<Integer> sessions = dataBase.getSessions(username);

		Vector<Message> messages = new Vector<Message>();
		Message message;

		// 遍历sessions，从redis中搜索聊天记录
		for (Integer sessionId : sessions) {
			message = new Message("5r");
			message.setMessageField1(String.valueOf(sessionId));
			message.setMessageField2(Redis.receive(sessionId));
			messages.add(message);
		}
		byte[] messageByteArray = JSONArray.toJSONString(messages).getBytes("utf-8");
		dataOutputStream.write(messageByteArray);
	}

	/**
	 * 创建会话，6号消息的处理方法。
	 * 
	 * @param dataBase         数据库对象引用
	 * @param username         创建者的用户名
	 * @param dataOutputStream 输出流对象引用
	 * @throws IOException 流IO错误
	 */
	public void message6(DataBase dataBase, String username, DataOutputStream dataOutputStream) throws IOException {
		int sessionId = dataBase.createSession(username);
		Message message = new Message("6r");
		message.setMessageField1(String.valueOf(sessionId));
		sendMessageAsByteArray(dataOutputStream, message);
	}

	/**
	 * 加入会话，7号消息的处理方法。
	 * 
	 * @param dataBase         数据库对象引用
	 * @param dataOutputStream 输出流对象引用
	 * @throws IOException 流IO错误
	 */
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

	/**
	 * 服务端处理9号消息。服务端接收到这种消息后，将其转发给会话中除了发送者外所有的用户。
	 * 
	 * @param dataBase       数据库引用
	 * @param senderUsername 发送者用户名，即应LinkThread中储存的username
	 * @throws IOException 向所有接收方的InputStream中写入数据时可能发生IOException
	 */
	public void message9(DataBase dataBase, String senderUsername) throws IOException {
		// 内容除了messageNumber外不会变
		setMessageNumber("9r");
		// 会话编号
		int sessionId = Integer.parseInt(getMessageField1());
		//
		String contentString = getMessageField2();
		Content content = JSON.parseObject(contentString, Content.class);

		// 储存在redis中
		Redis.send(sessionId, content);

		// 获得该会话中的所有用户
		Vector<String> users = dataBase.getMembers(sessionId);

		Socket socket;
		OutputStream outputStream;
		DataOutputStream dataOutputStream;

		// 转发给会话中除了发送者外所有的用户
		for (String username : users) {
			if (!username.contentEquals(senderUsername)) {
				socket = dataBase.searchSocketByUsername(username);
				// 为空表示未上线，直接跳过
				if (socket == null) {
					continue;
				}
				outputStream = socket.getOutputStream();
				dataOutputStream = new DataOutputStream(outputStream);
				sendMessageAsByteArray(dataOutputStream, this);
			}
		}
	}

	public void message10(DataBase dataBase, DataOutputStream dataOutputStream, String requestorUsername)
			throws IOException {
		String receiverUsername = getMessageField1();
		String checkMessage = getMessageField2();
		// 构建好友请求对象
		FriendRequest request = new FriendRequest(requestorUsername, checkMessage);
		// 如果接收方在线，则直接发送11号消息
		if (dataBase.addRequest(receiverUsername, request)) {

		}

	}

	public void message11(DataBase dataBase, DataInputStream dataInputStream, DataOutputStream dataOutputStream) {

	}
}
