package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 書店的三個批次，透過 REST API 啟動（/api/jobs）：
 *
 * <ul>
 * <li>bookReportJob：匯出書籍報表（CSV）</li>
 * <li>priceUpdateJob：套用價格更新檔，再匯出新的報表</li>
 * <li>orderShippingJob：把待出貨的訂單改成已出貨</li>
 * </ul>
 */
@SpringBootApplication
public class SpringBatch4Application {

	public static void main(String[] args) {
		SpringApplication.run(SpringBatch4Application.class, args);
	}
}
