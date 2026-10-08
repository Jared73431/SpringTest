package com.example.demo.lesson;

import java.math.BigDecimal;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.example.demo.entity.Product;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 第 1 課用的 Repository。放在測試程式碼中：它在 com.example.demo 底下，執行測試時 Spring Data 會掃描到它，
 * 正式程式碼不必為了示範而多出一堆沒有用到的方法。
 */
public interface ProductQueryRepository extends ReactiveCrudRepository<Product, Integer> {

	// ---- 依方法名稱產生查詢（derived query） ----

	Flux<Product> findByDescriptionContainingIgnoreCase(String keyword);

	Flux<Product> findByPriceBetweenOrderByPriceAsc(BigDecimal min, BigDecimal max);

	// 分頁：R2DBC 回傳 Flux，沒有 Page 物件；總筆數要另外 count
	Flux<Product> findAllBy(Pageable pageable);

	// ---- 自己寫 SQL ----

	@Query("SELECT * FROM product WHERE price < :max ORDER BY price DESC")
	Flux<Product> findCheaperThan(BigDecimal max);

	// 修改資料的 SQL 要加 @Modifying，回傳值是受影響的筆數（也可以是 Mono<Boolean> 或 Mono<Void>）
	@Modifying
	@Query("UPDATE product SET price = price * :rate WHERE price >= :min")
	Mono<Integer> raisePrice(BigDecimal rate, BigDecimal min);
}
