package test;

import redis.Redis;

public class Test {
	public static void main(String[] args) {

		System.out.println(Redis.receive(1));
	}
}
