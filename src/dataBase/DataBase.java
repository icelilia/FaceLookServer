package dataBase;

import java.net.Socket;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.bson.Document;
import com.alibaba.fastjson.JSON;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.FindIterable;

import dataBase.MongoDBAPI;
import dataBase.entity.*;

public class DataBase {
	/**
	 * mongoDB地址
	 */
	private static final String MONGODB_SERVER_HOST = "mongodb://Andersen:213533@175.24.10.214:27017/?authSource=admin&readPreference=primary&appname=MongoDB%20Compass&ssl=false";

	/**
	 * mongodb对象的引用
	 */
	private final static MongoClient mongoClient = new MongoClient(new MongoClientURI(MONGODB_SERVER_HOST));

	/**
	 * FaceLook数据库的引用
	 */
	private final static MongoDatabase faceLook = MongoDBAPI.getOrCreateDatabase(mongoClient, "FaceLook");

	private static final String USER_COLLECTION = "userCollection";
	/**
	 * Usercollection的引用
	 */
	@SuppressWarnings("unused")
	private final static MongoCollection<Document> userCollection = MongoDBAPI.getOrCreateCollection(faceLook,
			USER_COLLECTION);

	private static final String SESSION_COLLECTION = "sessionCollection";
	/**
	 * Sessioncollection的引用
	 */
	@SuppressWarnings("unused")
	private final static MongoCollection<Document> sessionCollection = MongoDBAPI.getOrCreateCollection(faceLook,
			SESSION_COLLECTION);

	/**
	 * <username, socket>关联
	 */
	private Hashtable<String, Socket> socketTable = new Hashtable<String, Socket>();

	/**
	 * 构造函数，同时包含了一些初始化工作
	 */
	private DataBase() {
		// 数据库初始化工作
		// 获取当前最大的sessionId，以防重启服务端后生成的sessionId和以前的重复
		Document document = MongoDBAPI.getMax(faceLook, SESSION_COLLECTION, "sessionId");
		try {
			int maxSessionId = JSON.parseObject(document.toJson(), Session.class).getSessionId();
			Session.sessionNum = maxSessionId;
		} catch (Exception e) {
			Session.sessionNum = 0;
		}

	}

	/**
	 * 单例模式
	 */
	private final static DataBase dataBaseInstance = new DataBase();

	/**
	 * 获得数据库单例对象的引用
	 * 
	 * @return 数据库单例对象的引用
	 */
	public static DataBase getDataBaseInstance() {
		return dataBaseInstance;
	}

	/**
	 * 检验Document的合法性。
	 * 
	 * @param document 待检验Document的引用
	 * @return true：合法；false：不合法
	 */
	private boolean isValidDocument(Document document) {
		return (document != null) && (!document.isEmpty());
	}

	/* 基础的检索 */

	/**
	 * 根据username获取User对象。
	 * 
	 * @param username
	 * @return User
	 */
	synchronized public User getUserByUsername(final String username) {
		Document searchUserDocument = new Document("username", username);
		Document userDocument = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION, searchUserDocument);
		return JSON.parseObject(userDocument.toJson(), User.class);
	}

	/**
	 * 根据sessionId获取Session对象。
	 * 
	 * @param sessionId
	 * @return Session
	 */
	synchronized public Session getSessionBySessionId(final int sessionId) {
		Document searchSessionDocument = new Document("sessionId", sessionId);
		Document sessionDocument = MongoDBAPI.findOneDocument(faceLook, SESSION_COLLECTION, searchSessionDocument);
		return JSON.parseObject(sessionDocument.toJson(), Session.class);
	}

	/* User相关 */

	/**
	 * 检验登录。登录时检验username和密码是否匹配。
	 * 
	 * @param username 登录的username
	 * @param password 登录的密码
	 * @return true：匹配，登录成功；false：不匹配，登录失败
	 */
	synchronized public boolean checkLogin(final String username, final String password) {
		Document searchUserDocument = new Document("username", username).append("password", password);
		Document userDocument = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION, searchUserDocument);
		return isValidDocument(userDocument);
	}

	/**
	 * 检验username唯一性。
	 * 
	 * @param username 待检验的username
	 * @return true：该username唯一；false：该username已存在
	 */
	synchronized public boolean checkUsernameUniqueness(final String username) {
		Document searchUsernameDocument = new Document("username", username);
		Document usernameDocument = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION, searchUsernameDocument);
		return !isValidDocument(usernameDocument);
	}

	/**
	 * 注册User，将User注册至数据库中。注意，该方法并不对待注册的User对象做任何格式检查。
	 * 
	 * @param user 待注册的User对象
	 */
	synchronized public void registerUser(final User user) {
		Document userDocument = Document.parse(JSON.toJSONString(user));
		MongoDBAPI.insertOneDocument(faceLook, USER_COLLECTION, userDocument);
	}

	synchronized public User updateUserInfo(User tempUser, final String username) {
		Document searchUserDocument = new Document("username", username);
		Document userDocument = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION, searchUserDocument);
		User user = JSON.parseObject(userDocument.toJson(), User.class);

		user.setNickname(tempUser.getNickname());
		user.setAvatarAddress(tempUser.getAvatarAddress());
		user.setInvitee(tempUser.getInvitee());
		user.setEmail(tempUser.getEmail());
		user.setPhoneNumber(tempUser.getPhoneNumber());
		user.setOccupation(tempUser.getOccupation());
		user.setLocation(tempUser.getLocation());

		userDocument = Document.parse(JSON.toJSONString(user));
		MongoDBAPI.updateOneDocument(faceLook, USER_COLLECTION, searchUserDocument, userDocument);

		user.justInformation();
		return user;
	}

	synchronized public void delFriend(String activeUsername, String passiveUsername) {
		// 获取两个User对象
		Document searchActiveUserDocument = new Document("username", activeUsername);
		Document activeUserDocument = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION, searchActiveUserDocument);
		User activeUser = JSON.parseObject(activeUserDocument.toJson(), User.class);

		Document searchPassiveUserDocument = new Document("username", passiveUsername);
		Document passiveUserDocument = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION, searchPassiveUserDocument);

		User passiveUser = JSON.parseObject(passiveUserDocument.toJson(), User.class);

		// 删除好友关系后写回
		activeUser.getFriendUsernames().remove(passiveUsername);
		Document newActiveUserDocument = Document.parse(JSON.toJSONString(activeUser));
		MongoDBAPI.updateOneDocument(faceLook, USER_COLLECTION, activeUserDocument, newActiveUserDocument);

		passiveUser.getFriendUsernames().remove(activeUsername);
		Document newPassiveUserDocument = Document.parse(JSON.toJSONString(passiveUser));
		MongoDBAPI.updateOneDocument(faceLook, USER_COLLECTION, passiveUserDocument, newPassiveUserDocument);
	}

	/**
	 * 获得指定User的好友列表。
	 * 
	 * @param username 指定User的username
	 * @return 指定User的好友列表
	 */
	synchronized public Vector<String> getFriendUsernames(final String username) {
		Document searchUserDocument = new Document("username", username);
		Document userDocument = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION, searchUserDocument);
		User user = JSON.parseObject(userDocument.toJson(), User.class);
		return user.getFriendUsernames();
	}

	/**
	 * 获得指定User的Session列表。
	 * 
	 * @param username 指定User的username
	 * @return 指定User的Session列表
	 */
	synchronized public Vector<Integer> getSessionIds(final String username) {
		Document searchUserDocument = new Document("username", username);
		Document userDocument = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION, searchUserDocument);
		User user = JSON.parseObject(userDocument.toJson(), User.class);
		return user.getSessionIds();

	}

	/**
	 * 获得指定User的申请列表。
	 * 
	 * @param username 指定User的username
	 * @return 指定User的申请列表，一个元素类型为Request的Vector
	 */
	synchronized public Vector<Request> getRequests(String username) {
		Document searchUserDocument = new Document("username", username);
		Document userDocument = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION, searchUserDocument);
		User user = JSON.parseObject(userDocument.toJson(), User.class);
		return user.getRequests();
	}

	/**
	 * 发送方发出好友或群聊申请。无论接收方是否在线，该申请均会被添加至接收方的申请列表里。
	 * 
	 * @param requestorUsername 发送方的username
	 * @param receiverUsername  好友请求：接收方的username；群聊请求：null
	 * @param checkMessage      验证信息
	 * @param sessionId         好友请求：“0”；群聊请求：sessionId
	 * @return 好友请求：接收方的username；群聊请求：群主username
	 */
	synchronized public Request addRequest(String requestorUsername, String receiverUsername, String checkMessage,
			Date date) {
		// 获取接收方User对象
		Document searchReceiverDocument = new Document("username", receiverUsername);
		Document receiverDocument = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION, searchReceiverDocument);
		User receiver = JSON.parseObject(receiverDocument.toJson(), User.class);

		// 获取申请方User对象
		Document searchRequestorDocument = new Document("username", requestorUsername);
		Document requestorDocument = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION, searchRequestorDocument);
		User requestor = JSON.parseObject(requestorDocument.toJson(), User.class);

		Request request = new Request(requestorUsername, requestor.getAvatarAddress(), checkMessage, date);

		Vector<Request> requests = receiver.getRequests();
		Iterator<Request> iterator = requests.iterator();
		while (iterator.hasNext()) {
			Request tempRequest = iterator.next();
			if (tempRequest.getRequestorUsername().contentEquals(requestorUsername)) {
				return null;
			}
		}

		// 添加请求
		receiver.addRequest(request);

		// 写回
		receiverDocument = Document.parse(JSON.toJSONString(receiver));
		MongoDBAPI.updateOneDocument(faceLook, USER_COLLECTION, searchReceiverDocument, receiverDocument);

		return request;
	}

	/**
	 * 获得指定User的结果列表
	 * 
	 * @param username 指定User的username
	 * @return 指定User的结果列表，一个元素类型为Result的Vector
	 */
	synchronized public Vector<Result> getResults(String username) {
		Document searchUserDocument = new Document("username", username);
		Document userDocument = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION, searchUserDocument);
		User user = JSON.parseObject(userDocument.toJson(), User.class);
		return user.getResults();
	}

	/**
	 * 接收方处理好友申请。无论发送方是否在线，该申请均会被从接收方的申请列表里清除。 并且会将结果添加至发送方的结果列表里。
	 * 
	 * @param requestorUsername 发送方的username
	 * @param receiverUsername  接收方的username
	 * @param resultString      构造的结果对象
	 */
	synchronized public Result checkRequest(String requestorUsername, String receiverUsername, String resultString,
			Date date) {

		// 获取接收方User对象
		Document searchReceiverDocument = new Document("username", receiverUsername);
		Document receiverDocument = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION, searchReceiverDocument);
		User receiver = JSON.parseObject(receiverDocument.toJson(), User.class);

		// 获取申请方User对象
		Document searchRequestorDocument = new Document("username", requestorUsername);
		Document requestorDocument = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION, searchRequestorDocument);
		User requestor = JSON.parseObject(requestorDocument.toJson(), User.class);

		// 将申请方从接收方的请求列表里删除
		boolean hadRequest = false;
		Vector<Request> requests = receiver.getRequests();
		for (Request request : requests) {
			if (request.getRequestorUsername().contentEquals(requestorUsername)) {
				requests.remove(request);
				hadRequest = true;
				break;
			}
		}
		// 并未在申请列表里则直接结束，什么也不做
		if (!hadRequest) {
			return null;
		}

		// 若同意，则双方互相成为好友
		if (resultString.contentEquals("1")) {
			receiver.getFriendUsernames().add(requestorUsername);
			requestor.getFriendUsernames().add(receiverUsername);
		}

		// 写回结果
		receiverDocument = Document.parse(JSON.toJSONString(receiver));
		MongoDBAPI.updateOneDocument(faceLook, USER_COLLECTION, searchReceiverDocument, receiverDocument);

		Result result = new Result(receiverUsername, receiver.getAvatarAddress(), resultString, date);

		requestor.getResults().add(result);
		requestorDocument = Document.parse(JSON.toJSONString(requestor));
		MongoDBAPI.updateOneDocument(faceLook, USER_COLLECTION, searchRequestorDocument, requestorDocument);

		return result;
	}

	/**
	 * 创建Session。创建者将自动加入新Session。
	 * 
	 * @param username 创建者的username
	 * @return 新创建的Session的sessionId
	 */
	synchronized public int createSession(final String username) {
		// 获取User
		Document searchUserDocument = new Document("username", username);
		Document userDocument = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION, searchUserDocument);
		User user = JSON.parseObject(userDocument.toJson(), User.class);

		// 新建session
		Session.sessionNum++;
		Session session = new Session();
		int sessionId = session.getSessionId();

		// session中添加User
		session.addSessionMember(user.getUsername());

		// User中添加session
		user.addSession(sessionId);

		// 写回
		Document sessionDocument = Document.parse(JSON.toJSONString(session));
		MongoDBAPI.insertOneDocument(faceLook, SESSION_COLLECTION, sessionDocument);
		userDocument = Document.parse(JSON.toJSONString(user));
		MongoDBAPI.updateOneDocument(faceLook, USER_COLLECTION, searchUserDocument, userDocument);

		return sessionId;
	}

	/**
	 * 创建群聊。创建者将自动加入新Session，同时默认创建者为新Session的manager。
	 * 
	 * @param username    创建者的username
	 * @param sessionName 创建的群聊的名称
	 * @return 新创建的群聊的sessionId
	 */
	synchronized public int createSession(final String username, final String sessionName) {
		// 获取User
		Document searchUserDocument = new Document("username", username);
		Document userDocument = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION, searchUserDocument);
		User user = JSON.parseObject(userDocument.toJson(), User.class);

		// 新建session
		Session.sessionNum++;
		Session session = new Session();
		int sessionId = session.getSessionId();

		// 设置群聊名称
		session.setSessionName(sessionName);

		// 默认创建者为群主
		session.setManagerUsername(username);

		// session中添加User
		session.addSessionMember(user.getUsername());

		// User中添加session
		user.addSession(sessionId);

		// 写回
		Document sessionDocument = Document.parse(JSON.toJSONString(session));
		MongoDBAPI.insertOneDocument(faceLook, SESSION_COLLECTION, sessionDocument);
		userDocument = Document.parse(JSON.toJSONString(user));
		MongoDBAPI.updateOneDocument(faceLook, USER_COLLECTION, searchUserDocument, userDocument);

		return sessionId;
	}

	synchronized public String getSessionName(int sessionId) {
		Document searchSessionDocument = new Document("sessionId", sessionId);
		Document sessionDocument = MongoDBAPI.findOneDocument(faceLook, SESSION_COLLECTION, searchSessionDocument);
		Session session = JSON.parseObject(sessionDocument.toJson(), Session.class);
		return session.getSessionName();
	}

	synchronized public String getSessionManager(int sessionId) {
		Document searchSessionDocument = new Document("sessionId", sessionId);
		Document sessionDocument = MongoDBAPI.findOneDocument(faceLook, SESSION_COLLECTION, searchSessionDocument);
		Session session = JSON.parseObject(sessionDocument.toJson(), Session.class);
		return session.getManagerUsername();
	}

	/**
	 * 获得指定Session的成员列表。
	 * 
	 * @param sessionId 指定Session的sessinId
	 * @return 一个包含该Session中所有成员的username的Vector
	 */
	synchronized public Vector<String> getMembers(int sessionId) {
		Document searchSessionDocument = new Document("sessionId", sessionId);
		Document sessionDocument = MongoDBAPI.findOneDocument(faceLook, SESSION_COLLECTION, searchSessionDocument);
		Session session = JSON.parseObject(sessionDocument.toJson(), Session.class);
		return session.getSessionMembers();
	}

	synchronized public Session updateSessionInfo(final int sessionId, final String sessionName) {
		Document searchSessionDocument = new Document("sessionId", sessionId);
		Document sessionDocument = MongoDBAPI.findOneDocument(faceLook, SESSION_COLLECTION, searchSessionDocument);
		Session session = JSON.parseObject(sessionDocument.toJson(), Session.class);

		session.setSessionName(sessionName);

		// 写回
		sessionDocument = Document.parse(JSON.toJSONString(session));
		MongoDBAPI.updateOneDocument(faceLook, SESSION_COLLECTION, searchSessionDocument, sessionDocument);

		return session;
	}

	/**
	 * 建立指定User和指定Session的双向关系。
	 * 
	 * @param username  指定User的username
	 * @param sessionId 指定Session的sessionId
	 * @return true：该User之前未在该Session中；false：该User之前已在该Session中
	 */
	synchronized public boolean joinSession(final String username, final int sessionId) {
		Document searchSessionDocument = new Document("sessionId", sessionId);
		Document sessionDocument = MongoDBAPI.findOneDocument(faceLook, SESSION_COLLECTION, searchSessionDocument);
		Session session = JSON.parseObject(sessionDocument.toJson(), Session.class);

		// 先判断User是否已经在Session中
		Vector<String> sessionMembers = session.getSessionMembers();
		for (String memberUsername : sessionMembers) {
			// User已经在Session中
			if (memberUsername.contentEquals(username)) {
				return false;
			}
		}

		// User不在Session中
		Document searchUserDocument = new Document("username", username);
		Document userDocument = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION, searchUserDocument);
		User user = JSON.parseObject(userDocument.toJson(), User.class);

		// 更新Session中的成员列表
		session.addSessionMember(username);
		// 更新User的Session列表
		user.addSession(sessionId);

		// 写回
		sessionDocument = Document.parse(JSON.toJSONString(session));
		MongoDBAPI.updateOneDocument(faceLook, SESSION_COLLECTION, searchSessionDocument, sessionDocument);
		userDocument = Document.parse(JSON.toJSONString(user));
		MongoDBAPI.updateOneDocument(faceLook, USER_COLLECTION, searchUserDocument, userDocument);

		return true;
	}

	/**
	 * 将指定User从指定Session中去除。
	 * 
	 * @param username  指定User的username
	 * @param sessionId 指定Session的sessionId
	 * @return 更新后的Session对象
	 */
	synchronized public Session quitSession(final String username, final int sessionId) {
		Document searchSessionDocument = new Document("sessionId", sessionId);
		Document sessionDocument = MongoDBAPI.findOneDocument(faceLook, SESSION_COLLECTION, searchSessionDocument);
		Session session = JSON.parseObject(sessionDocument.toJson(), Session.class);

		Document searchUserDocument = new Document("username", username);
		Document userDocument = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION, searchUserDocument);
		User user = JSON.parseObject(userDocument.toJson(), User.class);

		// Session集合中的User
		Vector<String> sessionMembers = session.getSessionMembers();

		// 是manager的话则直接解散群聊
		if (user.getUsername().contentEquals(session.getManagerUsername())) {
			for (String sessionMemberUsername : sessionMembers) {
				// 获取sessionMember的User对象
				Document searchSessionMemberDocument = new Document("username", sessionMemberUsername);
				Document sessionMemberDocument = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION,
						searchSessionMemberDocument);
				User sessionMember = JSON.parseObject(sessionMemberDocument.toJson(), User.class);

				Vector<Integer> sessionIds = sessionMember.getSessionIds();

				Iterator<Integer> iterator = sessionIds.iterator();

				while (iterator.hasNext()) {
					Integer tempSessionId = iterator.next();
					if (tempSessionId == sessionId) {
						iterator.remove();
						break;
					}
				}

				// 写回User
				sessionMemberDocument = Document.parse(JSON.toJSONString(sessionMember));
				MongoDBAPI.updateOneDocument(faceLook, USER_COLLECTION, searchSessionMemberDocument,
						sessionMemberDocument);
			}

			// 写回Session
			MongoDBAPI.deleteOneDocument(faceLook, SESSION_COLLECTION, sessionDocument);

			session.setManagerUsername(null);
			return session;
		}

		// 不是manager则只用删除掉指定User
		Iterator<String> iterator1 = sessionMembers.iterator();
		while (iterator1.hasNext()) {
			String sessionMember = iterator1.next();
			if (sessionMember.contentEquals(username)) {
				iterator1.remove();
				break;
			}
		}

		// 更新User下面的Session列表
		Vector<Integer> sessionIds = user.getSessionIds();
		Iterator<Integer> iterator2 = sessionIds.iterator();
		while (iterator2.hasNext()) {
			Integer tempSessionId = iterator2.next();
			if (tempSessionId == sessionId) {
				iterator2.remove();
				break;
			}
		}

		// 写回
		sessionDocument = Document.parse(JSON.toJSONString(session));
		MongoDBAPI.updateOneDocument(faceLook, SESSION_COLLECTION, searchSessionDocument, sessionDocument);

		userDocument = Document.parse(JSON.toJSONString(user));
		MongoDBAPI.updateOneDocument(faceLook, USER_COLLECTION, searchUserDocument, userDocument);

		return session;
	}

	// 模糊查询，根据nickname查询
	synchronized public Vector<User> fuzzySearchByNickname(String nickname) {
		Pattern pattern = Pattern.compile(nickname);
		Document result = new Document("nickname", pattern);
		FindIterable<Document> documents = MongoDBAPI.findDocument(faceLook, USER_COLLECTION, result);
		Vector<User> users = new Vector<User>();
		documents.forEach((Consumer<Document>) document -> users.add(JSON.parseObject(document.toJson(), User.class)));
		return users;
	}

	// 准确查询，根据username查询
	synchronized public User searchByUsername(String username) {
		Document searchUserDocument = new Document("username", username);
		Document userDocument = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION, searchUserDocument);
		if (isValidDocument(userDocument)) {
			return JSON.parseObject(userDocument.toJson(), User.class);
		}
		return null;
	}

	/* socket表相关 */

	/**
	 * 在socketTable中添加<username, socket>关联。
	 * 
	 * @param username 指定User的username
	 * @param socket   指定连接的socket引用
	 */
	synchronized public void addSocket(String username, Socket socket) {
		if (username != null) {
			socketTable.put(username, socket);
		}
	}

	/**
	 * 根据username查找socket，可用来检验User是否在线。
	 * 
	 * @param username 待查找User的username
	 * @return 存在以username为key的键值对则返回对应的socket，不存在则返回null
	 */
	synchronized public Socket searchSocketByUsername(final String username) {
		if (username != null) {
			return socketTable.get(username);
		}
		return null;
	}

	/**
	 * 在socketTable中删除username,
	 * socket关联。可处理username为null以及socketTable中不存在以username为key的情况
	 * 
	 * @param username 待删除User的username
	 */
	synchronized public void delSocket(String username) {
		if (username != null && searchSocketByUsername(username) != null) {
			socketTable.remove(username);
		}
	}

	public Set<String> showSocket() {
		return socketTable.keySet();
	}
}
