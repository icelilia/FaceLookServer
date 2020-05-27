package main;

import java.net.ServerSocket;

import dataBase.DataBase;

import java.io.IOException;

public class Main {

	public static void main(String[] args) {
		try {
			// 最高并发设为10
			ServerSocket server = new ServerSocket(21915, 10);
			LinkThread.server = server;
			DataBase dataBase = DataBase.getDataBaseInstance();
			LinkThread.dataBase = dataBase;
			// 线程数要和最高并发数相同
			LinkThread linkThread0 = new LinkThread();
			LinkThread linkThread1 = new LinkThread();
			LinkThread linkThread2 = new LinkThread();
			LinkThread linkThread3 = new LinkThread();
			LinkThread linkThread4 = new LinkThread();
			LinkThread linkThread5 = new LinkThread();
			LinkThread linkThread6 = new LinkThread();
			LinkThread linkThread7 = new LinkThread();
			LinkThread linkThread8 = new LinkThread();
			LinkThread linkThread9 = new LinkThread();
			linkThread0.start();
			linkThread1.start();
			linkThread2.start();
			linkThread3.start();
			linkThread4.start();
			linkThread5.start();
			linkThread6.start();
			linkThread7.start();
			linkThread8.start();
			linkThread9.start();
			// 回头可以加点服务器端的命令，像"stop"之类的
		} catch (IOException e) {
			System.err.println("出现异常：" + e);
			System.exit(-1);
		}
	}
}
