package dataBase;

import java.net.Socket;

import java.util.Hashtable;
import java.util.Iterator;
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
import dataBase.model.Friend;
import dataBase.model.Session;
import dataBase.model.User;

public class DataBase {
	private static final String MONGODB_SERVER_HOST = "mongodb://Andersen:213533@127.0.0.1:27017/?authSource=admin&readPreference=primary&appname=MongoDB%20Compass&ssl=false";
	@SuppressWarnings("unused")
	private static final String MONGODB_SERVER_LOCALHOST = "mongodb://127.0.0.1:27017";
	// mongodb对象的引用
	private final static MongoClient mongoClient = new MongoClient(new MongoClientURI(MONGODB_SERVER_HOST));
	// FaceLook数据库的引用
	private final static MongoDatabase faceLook = MongoDBAPI.getOrCreateDatabase(mongoClient, "FaceLook");
	// 以下是各个表的引用
	// 用户表
	@SuppressWarnings("unused")
	private final static MongoCollection<Document> userCollection = MongoDBAPI.getOrCreateCollection(faceLook,
			"userCollection");
	// 对话表
	@SuppressWarnings("unused")
	private final static MongoCollection<Document> sessionCollection = MongoDBAPI.getOrCreateCollection(faceLook,
			"sessionCollection");
	// ......

	private static final String USER_COLLECTION_NAME = "userCollection";
	private static final String SESSION_COLLECTION_NAME = "sessionCollection";
	// ......

	// socket关联表，Hashtable线程安全
	private Hashtable<String, Socket> socketTable = new Hashtable<String, Socket>();
	// 单例对象的引用
	private final static DataBase dataBaseInstance = new DataBase();

	private DataBase() {
		// 获取当前最大的sessionId

	}

	// 获得单例对象的引用
	public static DataBase getDataBaseInstance() {
		return dataBaseInstance;
	}

	/* 数据库相关 */
	// 检验用户名唯一性
	public boolean checkUsernameUniqueness(String username) {
		Document document = new Document("username", username);
		Document result = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION_NAME, document);
		return !isValidDocument(result);
	}

	// 用户注册
	public boolean registerUser(User user) {
		Document document = Document.parse(JSON.toJSONString(user));
		MongoDBAPI.insertOneDocument(faceLook, USER_COLLECTION_NAME, document);
		return true;
	}

	// 检验登录
	public boolean checkLogin(String username, String password) {
		Document document = new Document("username", username).append("password", password);
		Document result = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION_NAME, document);
		return isValidDocument(result);
	}

	// 获得好友列表
	public Vector<Friend> getFriends(String username) {
		Document document = new Document("username", username);
		Document result = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION_NAME, document);
		User user = JSON.parseObject(result.toJson(), User.class);
		Vector<String> friendsUsername = user.getFriends();
		Vector<Friend> friends = new Vector<Friend>(8);
		Friend friend;
		for (String string : friendsUsername) {
			document = new Document("username", string);
			result = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION_NAME, document);
			user = JSON.parseObject(result.toJson(), User.class);
			friend = new Friend();
			friend.setUsername(string);
			friend.setNickname(user.getNickname());
			friends.add(friend);
		}
		return friends;
	}

	// 创建对话
	public int createSession(String username) {
		// 获取用户
		Document oldDocument = new Document("username", username);
		Document result = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION_NAME, oldDocument);
		User user = JSON.parseObject(result.toJson(), User.class);

		// 新建session
		Session.sessionNum++;
		Session session = new Session();
		int sessionId = session.getSessionId();

		// session中添加用户
		session.addSessionMember(user.getUsername());

		// 用户中添加session
		user.addSession(sessionId);

		// 记录入库
		Document document = Document.parse(JSON.toJSONString(session));
		MongoDBAPI.insertOneDocument(faceLook, SESSION_COLLECTION_NAME, document);
		Document newDocument = Document.parse(JSON.toJSONString(user));
		MongoDBAPI.updateOneDocument(faceLook, USER_COLLECTION_NAME, oldDocument, newDocument);
		return sessionId;
	}

	// 加入对话
	public boolean joinSession(String username, int sessionId) {
		Document sessionDocument = new Document("sessionId", sessionId);
		Document sessionResult = MongoDBAPI.findOneDocument(faceLook, SESSION_COLLECTION_NAME, sessionDocument);

		Document userDocument = new Document("username", username);
		Document userResult = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION_NAME, userDocument);

		if (isValidDocument(sessionResult) && isValidDocument(userResult)) {
			// 更新会话集合中用户信息
			Session session = JSON.parseObject(sessionResult.toJson(), Session.class);
			session.addSessionMember(username);
			Document newSessionDocument = Document.parse(JSON.toJSONString(session));
			MongoDBAPI.updateOneDocument(faceLook, SESSION_COLLECTION_NAME, sessionDocument, newSessionDocument);

			// 更新用户下面的会话列表
			User user = JSON.parseObject(userResult.toJson(), User.class);
			user.addSession(sessionId);
			Document newUserDocument = Document.parse(JSON.toJSONString(user));
			MongoDBAPI.updateOneDocument(faceLook, USER_COLLECTION_NAME, userDocument, newUserDocument);
			return true;
		}
		return false;
	}

	// 退出对话
	public boolean quitSession(String username, int sessionId) {
		Document sessionDocument = new Document("sessionId", sessionId);
		Document sessionResult = MongoDBAPI.findOneDocument(faceLook, SESSION_COLLECTION_NAME, sessionDocument);

		Document userDocument = new Document("username", username);
		Document userResult = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION_NAME, userDocument);

		if (isValidDocument(sessionResult) && isValidDocument(userResult)) {
			// 更新会话集合中用户信息
			Session session = JSON.parseObject(sessionResult.toJson(), Session.class);
			// String类重写了equals方法，可以直接remove
			session.getSessionMembers().remove(username);
			Document newSessionDocument = Document.parse(JSON.toJSONString(session));
			MongoDBAPI.updateOneDocument(faceLook, SESSION_COLLECTION_NAME, sessionDocument, newSessionDocument);

			// 更新用户下面的会话列表
			User user = JSON.parseObject(userResult.toJson(), User.class);
			// 这里remove时要注意，Integer没有重写Object的equals方法
			Vector<Integer> sessionIds = user.getSessions();
			Iterator<Integer> iter = sessionIds.iterator();
			while (iter.hasNext()) {
				Integer id = iter.next();
				if (id.intValue() == sessionId) {
					sessionIds.remove(id);
					break;
				}
			}
			Document newUserDocument = Document.parse(JSON.toJSONString(user));
			MongoDBAPI.updateOneDocument(faceLook, USER_COLLECTION_NAME, userDocument, newUserDocument);
			return true;
		}
		return false;
	}

	// 获得用户的对话列表
	public Vector<Integer> getSessions(String username) {
		Document document = new Document("username", username);
		Document result = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION_NAME, document);
		if (isValidDocument(result)) {
			User user = JSON.parseObject(result.toJson(), User.class);
			return user.getSessions();
		}
		return null;
	}

	// 获得对话的用户列表
	public Vector<String> getUsers(int sessionId) {
		Document document = new Document("sessionId", sessionId);
		Document result = MongoDBAPI.findOneDocument(faceLook, SESSION_COLLECTION_NAME, document);
		if (isValidDocument(result)) {
			Session session = JSON.parseObject(result.toJson(), Session.class);
			return session.getSessionMembers();
		}
		return null;
	}

	// 添加好友
	public boolean addFriend(String username1, String username2) {
		Document user1Document = new Document("username", username1);
		Document user1Result = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION_NAME, user1Document);
		Document user2Document = new Document("username", username2);
		Document user2Result = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION_NAME, user2Document);
		User user1 = JSON.parseObject(user1Result.toJson(), User.class);
		User user2 = JSON.parseObject(user2Result.toJson(), User.class);
		if (user1 == null || user2 == null) {
			return false;
		}
		user1.getFriends().add(username2);
		user2.getFriends().add(username1);
		Document newUser1Document = Document.parse(JSON.toJSONString(user1));
		MongoDBAPI.updateOneDocument(faceLook, USER_COLLECTION_NAME, user1Document, newUser1Document);
		Document newUser2Document = Document.parse(JSON.toJSONString(user2));
		MongoDBAPI.updateOneDocument(faceLook, USER_COLLECTION_NAME, user2Document, newUser2Document);
		return true;
	}

	// 模糊查询，根据nickname查询
	public Vector<User> fuzzySearchByNickname(String nickname) {
		Pattern pattern = Pattern.compile(nickname);
		Document result = new Document("nickname", pattern);
		FindIterable<Document> documents = MongoDBAPI.findDocument(faceLook, USER_COLLECTION_NAME, result);
		Vector<User> users = new Vector<User>();
		documents.forEach((Consumer<Document>) document -> users.add(JSON.parseObject(document.toJson(), User.class)));
		return users;
	}

	// 准确查询，根据username查询
	public User searchByUsername(String username) {
		Document result = new Document("username", username);
		Document document = MongoDBAPI.findOneDocument(faceLook, USER_COLLECTION_NAME, result);
		if (document != null) {
			return JSON.parseObject(document.toJson(), User.class);
		}
		return null;
	}

	// 检验合法文档（检索结果的合法性检查）
	private boolean isValidDocument(Document document) {
		return (document != null) && (!document.isEmpty());
	}

	/* socket表相关 */
	// 添加socket关联
	public void addSocket(String username, Socket socket) {
		socketTable.put(username, socket);
	}

	// 根据用户名查找socket
	public Socket searchSocketByUsername(String username) {
		return socketTable.get(username);
	}

	// 删除socket关联
	public void delSocket(String username) {
		socketTable.remove(username);
	}
}
