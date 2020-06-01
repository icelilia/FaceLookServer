package test;

import com.alibaba.fastjson.JSON;

import redis.entity.Content;

public class Test {
	public static void main(String[] args) {

		Content content = new Content("a", "b", "2020-05-31", "wdnmd");
		System.out.println(JSON.toJSONString(content));
	}
}
