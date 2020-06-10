package main;

import java.io.IOException;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import java.net.Socket;
import java.text.ParseException;
import java.util.Date;
import java.util.Vector;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import dataBase.DataBase;
import dataBase.entity.*;
import redis.Redis;
import redis.entity.Content;

public class Message {
	public static DataBase dataBase;

	private String messageNumber; // 对应开发文档里的编号
	private String messageField1; // 字段1
	private String messageField2; // 字段2
	private String messageField3; // 字段3

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

	public static Message receiveMessage(DataInputStream dataInputStream) throws IOException {
		// 裸奔版
		final int MAX_SIZE = 0xffff;
		byte[] messageByteArray = new byte[MAX_SIZE];
		dataInputStream.read(messageByteArray);
		String messageJSONString = new String(messageByteArray, "utf-8");
		System.err.println("收到包：" + messageJSONString);
		return JSON.parseObject(messageJSONString, Message.class);

		// 首部长度校验版
		// int length = dataInputStream.readInt();
		// byte[] messageByteArray = new byte[length];
		// dataInputStream.read(messageByteArray);
		// return JSON.parseObject(new String(messageByteArray, "utf-8"),
		// Message.class);
	}

	private static void sendMessageAsByteArray(DataOutputStream dataOutputStream, Message message) throws IOException {
		byte[] messageByteArray;
		messageByteArray = JSON.toJSONString(message).getBytes("utf-8");
		dataOutputStream.write(messageByteArray);
		System.out.println("发送包：" + JSON.toJSONString(message));
	}

	/**
	 * 0：登出。将username从socketTable中删除，并输出日志。
	 * 
	 * @param username 待登出User的username
	 */
	public void message0(final String username) {
		// 维护socketTable
		dataBase.delSocket(username);
		// 输出日志
		System.out.println("User" + "[" + username + "]" + "已登出");
	}

	/**
	 * 1：连接初始化。返回发送一个1r消息，表示服务器目前能够以处理这个连接。
	 * 
	 * @param dataOutputStream 输出流对象引用
	 * @throws IOException 输出流IO异常
	 */
	public void message1(final DataOutputStream dataOutputStream) throws IOException {
		Message message = new Message();
		message.setMessageNumber("1r");
		sendMessageAsByteArray(dataOutputStream, message);
	}

	/**
	 * 2：登录。检查username和密码，返回发送一个2r消息，并附带登录结果。若登录成功，维护socketTable并附带User的个人信息。
	 * 
	 * @param dataOutputStream 输出流对象引用
	 * @return true：登录成功；false：登录失败
	 * @throws IOException 输出流IO异常
	 */
	public boolean message2(final DataOutputStream dataOutputStream) throws IOException {
		// 先获得username和密码
		String username = getMessageField1();
		String password = getMessageField2();

		Message message = new Message("2r");

		// 检验登录
		if (!dataBase.checkLogin(username, password)) {
			message.setMessageField1("0");
			message.setMessageField2("username或密码错误");
			sendMessageAsByteArray(dataOutputStream, message);
			return false;
		}

		// 检验该User是否已登录
		if (dataBase.searchSocketByUsername(username) != null) {
			message.setMessageField1("0");
			message.setMessageField2("该User已登录");
			sendMessageAsByteArray(dataOutputStream, message);
			return false;
		}

		// 到这里说明登录成功了
		message.setMessageField1("1");
		message.setMessageField2("OK");

		// 获得User的个人信息
		User user = dataBase.getUserByUsername(username);
		// 处理掉多余部分
		user.justInformation();

		message.setMessageField3(JSON.toJSONString(user));
		sendMessageAsByteArray(dataOutputStream, message);

		// 输出日志
		System.out.println("User" + "[" + username + "]" + "已登录");
		return true;
	}

	/**
	 * 3：注册。检查username和密码是否合法后，返回发送一个3r消息，并附带注册结果。若注册成功，附带新注册User的个人信息
	 * 
	 * @param dataOutputStream 输出流对象引用
	 * @throws IOException 输出流IO异常
	 */
	public void message3(final DataOutputStream dataOutputStream) throws IOException {
		// 获得待注册User的User对象
		User user = JSON.parseObject(getMessageField1(), User.class);

		String username = user.getUsername();
		String password = user.getPassword();

		Message message = new Message("3r");

		// 正则表达式
		final String pattern1 = "[A-Za-z0-9_]{3,16}";
		final String pattern2 = "[A-Za-z0-9_]{3,16}";

		// username合法性检查
		if (!Pattern.matches(pattern1, username)) {
			message.setMessageField1("0");
			message.setMessageField2("username格式非法");
			sendMessageAsByteArray(dataOutputStream, message);
			return;
		}

		// 密码合法性检查
		if (!Pattern.matches(pattern2, password)) {
			message.setMessageField1("0");
			message.setMessageField2("密码格式非法");
			sendMessageAsByteArray(dataOutputStream, message);
			return;
		}

		// username唯一性检查
		if (!dataBase.checkUsernameUniqueness(username)) {
			message.setMessageField1("0");
			message.setMessageField2("username已被占用");
			sendMessageAsByteArray(dataOutputStream, message);
			return;
		}

		// 到这里说明合法性检查通过
		// 经测试得知，即使传输来的序列化数据中不包含4个列表，反序列化后的对象里4个列表也不是null，可以直接注册
		dataBase.registerUser(user);

		message.setMessageField1("1");
		message.setMessageField2("OK");
		// 处理掉多余部分
		user.justInformation();
		message.setMessageField3(JSON.toJSONString(user));
		sendMessageAsByteArray(dataOutputStream, message);

		System.out.println("User" + "[" + username + "]" + "已注册");
	}

	/**
	 * 4：请求好友列表。返回发送一个4r消息，附带一个User[]及其长度信息，User经过justInformation处理。
	 * 
	 * @param dataOutputStream 输出流对象引用
	 * @param username         请求方的username
	 * @throws IOException 输出流IO异常
	 */
	public void message4(final DataOutputStream dataOutputStream, final String username) throws IOException {
		// 先获得所有的好友username
		Vector<String> friendUsernames = dataBase.getFriendUsernames(username);

		int length = friendUsernames.size();

		Vector<User> friends = new Vector<User>(length);
		User friend;

		// 根据好友username，获取好友的User对象，并处理掉多余信息
		for (String friendUsername : friendUsernames) {
			friend = dataBase.getUserByUsername(friendUsername);
			friend.justInformation();
			friends.add(friend);
		}

		Message message;
		message = new Message("4r");
		message.setMessageField1(String.valueOf(length));
		message.setMessageField2(JSONArray.toJSONString(friends));
		sendMessageAsByteArray(dataOutputStream, message);
	}

	/**
	 * 5：请求历史记录。返回发送一个5r消息，附带一个Session[]及其长度信息，Session中的contents为序列化后的消息数组。
	 * 
	 * @param dataOutputStream 输出流对象引用
	 * @param username         请求方的username
	 * @throws IOException 输出流IO异常
	 */
	public void message5(final DataOutputStream dataOutputStream, final String username) throws IOException {
		// 获取User所在的所有会话的sessionId
		Vector<Integer> sessionIds = dataBase.getSessionIds(username);

		int length = sessionIds.size();

		Vector<Session> sessions = new Vector<Session>();
		Session session;

		// 根据sessionId，获取Session对象，并添加上contents信息
		for (Integer sessionId : sessionIds) {
			session = dataBase.getSessionBySessionId(sessionId);
			session.setContents(Redis.receive(sessionId));
			sessions.add(session);
		}

		Message message;
		message = new Message("5r");
		message.setMessageField1(String.valueOf(length));
		message.setMessageField2(JSONArray.toJSONString(sessions));
		sendMessageAsByteArray(dataOutputStream, message);
	}

	/**
	 * 6：创建会话。新建一个会话，并将创建者设置为manager，返回发送一个6r消息，并附带新会话的sessionId。
	 * 
	 * @param dataOutputStream 输出流对象引用
	 * @param creatorUsername  创建者的username
	 * @throws IOException 输出流IO异常
	 */
	public void message6(final DataOutputStream dataOutputStream, final String creatorUsername) throws IOException {
		String sessionName = getMessageField1();
		// 创建群聊
		if (sessionName != null && !sessionName.contentEquals("")) {
			int sessionId = dataBase.createSession(creatorUsername, sessionName);
			Message message = new Message("6r");
			message.setMessageField1(String.valueOf(sessionId));
			sendMessageAsByteArray(dataOutputStream, message);
		}
		// 创建单独会话
		else {
			int sessionId = dataBase.createSession(creatorUsername);
			Message message = new Message("6r");
			message.setMessageField1(String.valueOf(sessionId));
			sendMessageAsByteArray(dataOutputStream, message);
		}
	}

	/**
	 * 7：加入会话。 建立起指定User和指定Session的双向关系。若该User已存在在该Session中，则什么也不做。
	 * 
	 * @param dataOutputStream 输出流对象引用
	 * @throws IOException 输出流IO异常
	 */
	public void message7(final DataOutputStream dataOutputStream) throws IOException {
		// 指定User
		String inviteeUsername = getMessageField1();
		// 指定Session
		int sessionId = Integer.parseInt(getMessageField2());

		Message message = new Message("7r");
		message.setMessageField1("0");
		if (dataBase.joinSession(inviteeUsername, sessionId)) {
			message.setMessageField1("1");
		}
		Message.sendMessageAsByteArray(dataOutputStream, message);
	}

	/**
	 * 8：请求申请列表。
	 * 
	 * @param dataOutputStream 输出流对象引用
	 * @param username         请求方的username
	 * @throws IOException 输出流IO异常
	 */
	public void message8(final DataOutputStream dataOutputStream, final String username) throws IOException {
		Vector<Request> requests = dataBase.getRequests(username);
		Message message;
		message = new Message("8r");
		message.setMessageField1(String.valueOf(requests.size()));
		message.setMessageField2(JSONArray.toJSONString(requests));
		sendMessageAsByteArray(dataOutputStream, message);
	}

	/**
	 * 发送信息，9号消息的处理方法。一成不变地（除了messageNumber）将其转发给对应会话中除了发送者外所有的User。
	 * 
	 * @param dataBase       数据库引用
	 * @param senderUsername 发送者username，即应LinkThread中储存的username
	 * @throws IOException 向所有接收方的InputStream中写入数据时可能发生IOException
	 */
	public void message9(String senderUsername) throws IOException {
		// 内容除了messageNumber外不会变
		setMessageNumber("9r");
		// 会话编号
		int sessionId = Integer.parseInt(getMessageField1());
		//
		String contentString = getMessageField2();
		Content content = JSON.parseObject(contentString, Content.class);

		// 储存在redis中
		Redis.send(sessionId, content);

		// 获得该会话中的所有User
		Vector<String> users = dataBase.getMembers(sessionId);

		Socket socket;
		OutputStream outputStream;
		DataOutputStream dataOutputStream;

		// 转发给会话中所有的User
		for (String username : users) {
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

	/**
	 * 好友申请：申请方==>服务端。 申请方向接收方发出申请，先经过服务器。 无论接收方在线与否，在接收方的请求列表里添加上申请方的请求信息。
	 * 同时，若接收方在线，则调用message11方法立即向接收方发送一个11r消息。
	 * 
	 * @param dataBase          数据库的引用
	 * @param requestorUsername 发送方的username
	 * @throws IOException 流IO异常
	 */
	public void message10(String requestorUsername) throws IOException {
		String receiverUsername = getMessageField1();
		String checkMessage = getMessageField2();

		Date date = new Date();

		// 无论是否在线，先添加至请求列表
		dataBase.addRequest(requestorUsername, receiverUsername, checkMessage, date);

		// 判断接收方是否在线
		Socket receiverSocket = dataBase.searchSocketByUsername(receiverUsername);

		if (receiverSocket != null) {
			// 接收方在线则直接发送11号消息
			message11(receiverSocket, new Request(requestorUsername, checkMessage, date));
		}
	}

	/**
	 * 好友申请：服务端==>接收方。 服务端将申请转发给接收方。 注意，该方法只有在接收方在线时才会调用。
	 * 
	 * @param receiverSocket
	 * @param requestorUsername
	 * @param checkMessage
	 * @throws IOException
	 */
	private void message11(Socket receiverSocket, Request request) throws IOException {
		Message message = new Message("11r");
		message.setMessageField1(JSON.toJSONString(request));
		DataOutputStream dataOutputStream = new DataOutputStream(receiverSocket.getOutputStream());
		sendMessageAsByteArray(dataOutputStream, message);
	}

	/**
	 * 好友申请：接收方==>服务端。 接收方确认后将确认信息发送给服务端。 无论同意还是通过，都将从接收方的请求列表里删除申请方的请求。
	 * 无论申请方在线与否，都将在申请方的结果列表里记录结果。 同时，如果申请方在线，则调用message13方法立即向申请方发送一个13r消息。
	 * 
	 * @param dataBase
	 * @param receiverUsername
	 * @throws IOException
	 * @throws ParseException
	 */
	public void message12(String receiverUsername) throws IOException, ParseException {
		String requestorUsername = getMessageField1();
		String result = getMessageField2();

		Date date = new Date();

		dataBase.checkRequest(requestorUsername, receiverUsername, result, date);

		Socket requestorSocket = dataBase.searchSocketByUsername(requestorUsername);
		if (requestorSocket != null) {
			message13(requestorSocket, new Result(receiverUsername, result, date));
		}
	}

	/**
	 * 好友请求：服务端==>申请方。 服务端将结果发送给申请方。 注意，该方法只有在申请方在线时才会调用。
	 * 
	 * @param requestorSocket
	 * @param receiverUsername
	 * @param result
	 * @throws IOException
	 */
	private void message13(Socket requestorSocket, Result result) throws IOException {
		Message message = new Message("13r");
		message.setMessageField1(JSON.toJSONString(result));
		DataOutputStream dataOutputStream = new DataOutputStream(requestorSocket.getOutputStream());
		sendMessageAsByteArray(dataOutputStream, message);
	}

	public void message14(DataOutputStream dataOutputStream, String username) throws IOException {
		Vector<Result> results = dataBase.getResults(username);
		Message message;
		message = new Message("14r");
		message.setMessageField1(String.valueOf(results.size()));
		message.setMessageField2(JSONArray.toJSONString(results));
		sendMessageAsByteArray(dataOutputStream, message);
	}

	public void message15(DataOutputStream dataOutputStream, String activeUsername) throws IOException {
		String passiveUsername = getMessageField1();
		dataBase.delFriend(activeUsername, passiveUsername);
		Socket passiveUserSocket = dataBase.searchSocketByUsername(passiveUsername);
		if (passiveUserSocket != null) {
			message16(passiveUserSocket, passiveUsername);
		}
	}

	private void message16(Socket passiveUserSocket, String passiveUsername) throws IOException {
		// 先获得所有的好友对象
		Vector<String> friendUsernames = dataBase.getFriendUsernames(passiveUsername);

		int length = friendUsernames.size();

		Vector<User> friends = new Vector<User>(length);
		User friend;

		for (String friendUsername : friendUsernames) {
			friend = dataBase.getUserByUsername(friendUsername);
			friend.justInformation();
			friends.add(friend);
		}

		Message message;
		message = new Message("16r");
		message.setMessageField1(String.valueOf(length));
		message.setMessageField2(JSONArray.toJSONString(friends));
		DataOutputStream dataOutputStream = new DataOutputStream(passiveUserSocket.getOutputStream());
		sendMessageAsByteArray(dataOutputStream, message);
	}

	public void message17(String username) throws IOException {
		int sessionId = Integer.parseInt(getMessageField1());
		Session session = dataBase.quitSession(username, sessionId);

		Vector<String> sessionMembers = session.getSessionMembers();

		Socket socket;
		DataOutputStream dataOutputStream;

		Message message = new Message("17r");
		message.setMessageField1(JSON.toJSONString(session));

		// 向该Session中的所有User返回发送更新后的Session对象
		for (String sessionMember : sessionMembers) {
			socket = dataBase.searchSocketByUsername(sessionMember);
			// 为空表示未上线，直接跳过
			if (socket == null) {
				continue;
			}
			dataOutputStream = new DataOutputStream(socket.getOutputStream());
			sendMessageAsByteArray(dataOutputStream, message);
		}
	}

	/**
	 * 更新User个人信息
	 * 
	 * @param dataBase
	 * @param username
	 */
	public void message18(String username) {
		String userJSONString = getMessageField1();
		User user = JSON.parseObject(userJSONString, User.class);
		dataBase.updateUserInfo(user);
	}

	public void message19() throws IOException {
		int sessionId = Integer.parseInt(getMessageField1());
		String sessionName = getMessageField2();

		Session session = dataBase.updateSessionInfo(sessionId, sessionName);

		Vector<String> sessionMembers = session.getSessionMembers();

		Socket socket;
		DataOutputStream dataOutputStream;

		Message message = new Message("19r");
		message.setMessageField1(JSON.toJSONString(session));

		// 向该Session中的所有User返回发送更新后的Session对象
		for (String sessionMember : sessionMembers) {
			socket = dataBase.searchSocketByUsername(sessionMember);
			// 为空表示未上线，直接跳过
			if (socket == null) {
				continue;
			}
			dataOutputStream = new DataOutputStream(socket.getOutputStream());
			sendMessageAsByteArray(dataOutputStream, message);
		}
	}
}
