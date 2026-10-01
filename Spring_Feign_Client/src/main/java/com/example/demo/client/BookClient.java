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
 * 呼叫 Spring_Feign_Server 書籍 API 的 Feign Client。
 * 只需要宣告介面，Spring Cloud OpenFeign 會在啟動時產生實作：呼叫方法 → 組出 HTTP 請求 → 把回應 JSON 轉成回傳型別。
 * 註解沿用 Spring MVC 的 @GetMapping / @PathVariable / @RequestBody，寫法和 Server 端的 Controller 對稱。
 *
 * <ul>
 * <li>name：這個 client 的名稱，對應設定 spring.cloud.openfeign.client.config.book-server.*；
 * 搭配 Eureka 等服務註冊中心時，也是用來查詢服務位址的服務名稱</li>
 * <li>url：Server 位址，從設定檔讀取（本機為 localhost:8082，Docker Compose 中為服務名稱 book-server）</li>
 * <li>configuration：只套用在這個 client 的設定（自訂 ErrorDecoder），見 {@link BookClientConfig}</li>
 * </ul>
 */
@FeignClient(name = "book-server", url = "${book-server.url}", configuration = BookClientConfig.class)
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
