package com.example.demo.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.demo.dto.BookRequest;
import com.example.demo.dto.BookResponse;

/**
 * 呼叫 Spring_Eureka_Provider 書籍 API 的 Feign Client。
 *
 * 與 Spring_Feign_Client 最大的差別：<b>只寫服務名稱，不寫 url</b>。
 * <pre>
 * bookClient.findAll()
 *   → 服務名稱 service-provider
 *   → DiscoveryClient 向 Eureka 查詢這個名稱目前有哪些實例（例如 172.18.0.5:8084、172.18.0.6:8084）
 *   → LoadBalancer 挑一台（預設 Round Robin，輪流）
 *   → GET http://172.18.0.5:8084/api/books
 * </pre>
 * name 也對應設定 spring.cloud.openfeign.client.config.service-provider.*。
 * configuration：只套用在這個 client 的設定（自訂 ErrorDecoder），見 {@link BookClientConfig}。
 */
@FeignClient(name = "service-provider", configuration = BookClientConfig.class)
public interface BookClient {

	@GetMapping("/api/hello")
	String hello();

	@GetMapping("/api/books")
	List<BookResponse> findAll();

	// @PathVariable 明確寫出名稱 "id"：Feign 依名稱把參數填進 URL 範本
	@GetMapping("/api/books/{id}")
	BookResponse findById(@PathVariable("id") Integer id);

	@PostMapping("/api/books")
	BookResponse create(@RequestBody BookRequest request);

	@PutMapping("/api/books/{id}")
	BookResponse update(@PathVariable("id") Integer id, @RequestBody BookRequest request);

	@DeleteMapping("/api/books/{id}")
	void delete(@PathVariable("id") Integer id);
}
