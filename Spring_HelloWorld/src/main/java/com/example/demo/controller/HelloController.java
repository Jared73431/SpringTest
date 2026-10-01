package com.example.demo.controller;

import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 最小的 REST API 範例。
 * {@code @RestController} = {@code @Controller} + {@code @ResponseBody}：方法的回傳值直接寫入 HTTP Response Body，
 * 而不是當成頁面（View）名稱。
 *
 * @author Jared
 */
@RestController
@RequestMapping("/api") // Class 層級的共同路徑前綴，所有方法的路徑都會接在 /api 之後
public class HelloController {

	// 使用 SLF4J Logger 而不是 System.out：可控制 log 層級，並自動附上時間、Thread、Class
	private static final Logger log = LoggerFactory.getLogger(HelloController.class);

	/**
	 * GET /api/hello
	 *
	 * @return 純文字 "Hello World"（Content-Type: text/plain）
	 */
	@GetMapping("/hello")
	public String hello() {
		log.info("hello API 呼叫成功");
		return "Hello World";
	}

	/**
	 * 早期練習 Java 語法（ArrayList、Lambda、forEach）留下的草稿，與 API 無關，
	 * Spring 啟動時不會執行；刻意保留作為學習紀錄。
	 */
	public static void main(String[] args) {
		System.out.println("好");

		ArrayList<String> list = new ArrayList<>(Arrays.asList("I", "love", "you", "too"));
		list.forEach(str -> {
			if (str.length() == 3)
				System.out.println(str);
		});
	}
}
