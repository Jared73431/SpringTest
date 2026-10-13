package com.example.demo.client;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import com.example.demo.dto.BookRequest;
import com.example.demo.dto.BookResponse;

/**
 * 呼叫 Spring_Feign_Server 書籍 API 的 HTTP Service Client。
 *
 * <p>
 * 和 Spring_Feign_Client 的 BookClient 一樣只宣告介面，Spring 在啟動時產生實作：呼叫方法 → 組出 HTTP 請求 →
 * 把回應 JSON 轉成回傳型別。差別是：
 * <ul>
 * <li>註解用 @GetExchange / @PostExchange…（不是 Spring MVC 的 @GetMapping）</li>
 * <li>Spring Framework 內建，底層是 RestClient，不需要 Spring Cloud</li>
 * <li>位址、逾時等設定在 spring.http.serviceclient.book-server.*，註冊在 HttpServicesConfig</li>
 * </ul>
 */
@HttpExchange("/api")
public interface BookClient {

	@GetExchange("/hello")
	String hello();

	@GetExchange("/books")
	List<BookResponse> findAll();

	@GetExchange("/books/{id}")
	BookResponse findById(@PathVariable Integer id);

	@PostExchange("/books")
	BookResponse create(@RequestBody BookRequest request);

	@PutExchange("/books/{id}")
	BookResponse update(@PathVariable Integer id, @RequestBody BookRequest request);

	@DeleteExchange("/books/{id}")
	void delete(@PathVariable Integer id);
}
