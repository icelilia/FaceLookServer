package test;

import dataBase.*;
import dataBase.entity.*;

import java.util.Vector;

class UserDelegateImplTest {

	public static void main(String[] args) {
		DataBase dataBase = DataBase.getDataBaseInstance();

		/*
		 * 注册，验证用户名是否已注册过，未注册则入库
		 * 注册阶段，用户对象应该是不存在的，参数应该只有一个username
		 */
		// 用户输入的理想用户名为"Andersen"
		boolean validUser1 = dataBase.checkUsernameUniqueness("huangchangzhou");
		// 为假表示不存在
		if (validUser1) {
			// 新建基础的用户对象
			User user1 = new User("huangchangzhou", "123456", "hcz");
			// 记录
			dataBase.registerUser(user1);
			System.out.println("用户\"huangchangzhou\"注册成功");
		} else {
			// 重名处理
		}
		boolean validUser2 = dataBase.checkUsernameUniqueness("denshiman");
		if (validUser2) {
			User user2 = new User("denshiman", "123456", "dsm");
			dataBase.registerUser(user2);
			System.out.println("用户\"denshiman\"注册成功");
		} else {
			// 重名处理
		}

		// 整合UserAccount和UserInfo后，不需要添加UserInfo

		/*
		 * 登录，验证账号密码是否正确
		 * 同理，登录阶段，参数也应该只有username和password
		 */
		System.out.println("用户\"huangchangzhou\"的登录结果：" + dataBase.checkLogin("huangchangzhou", "123456"));

		/* 
		 * 获取当前用户的好友列表 
		 * 在LinkThread类中，一个线程对应的是一个用户的连接，LinkThread只保存用户的username
		 * 而User类不应该长期存于内存中
		 * 所以参数尽量设为username
		 */
		Vector<Friend> friends = dataBase.getFriends("huangchangzhou");
		System.out.print("huangchangzhou开始的好友：");
		for (Friend friend: friends) {
			System.out.print(friend.getNickname() + "| ");
		}
		System.out.println();
		
		// 添加好友并重新拉取好友列表
        dataBase.addFriend("huangchangzhou", "denshiman");
        
        friends = dataBase.getFriends("huangchangzhou");
        System.out.print("huangchangzhou现在的好友：");
        for (Friend friend: friends) {
			System.out.print(friend.getNickname() + "| ");
		}
        System.out.println();
        
        friends = dataBase.getFriends("denshiman");
        System.out.print("denshiman现在的好友：");
        for (Friend friend: friends) {
			System.out.print(friend.getNickname() + "| ");
		}
        System.out.println();
        
		/* 创建会话 */
		int sessionId = dataBase.createSession("huangchangzhou");
		System.out.println("新建的sessionId为：" + sessionId);
		
		/* 加入会话 */
		dataBase.joinSession("denshiman", sessionId);
		
		/* 获取当前用户的会话列表 */
		Vector<Integer> sessions = dataBase.getSessions("huangchangzhou");
		System.out.print("huangchangzhou现在的会话列表：");
        for (Integer id: sessions) {
			System.out.print(id + "| ");
		}
        System.out.println();
        
        sessions = dataBase.getSessions("denshiman");
        System.out.print("denshiman现在的会话列表：");
        for (Integer id: sessions) {
			System.out.print(id + "| ");
		}
        System.out.println();
        
		/* 获取当前会话所有用户 */
		Vector<String> members = dataBase.getMembers(sessionId);
		System.out.print("刚刚新建的会话列表中的用户：");
        for (String str: members) {
			System.out.print(str + "| ");
		}
        System.out.println();
        
		/* 退出会话 */
		if (dataBase.quitSession("denshiman", sessionId)) {
			members = dataBase.getMembers(sessionId);
			System.out.print("现在会话列表中的用户：");
	        for (String str: members) {
				System.out.print(str + "| ");
			}
	        System.out.println();
	        
	        sessions = dataBase.getSessions("denshiman");
	        System.out.print("denshiman现在的会话列表：");
	        for (Integer id: sessions) {
				System.out.print(id + "| ");
			}
	        System.out.println();
		}
	}
}