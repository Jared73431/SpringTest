package com.example.demo.client;

import org.springframework.context.annotation.Bean;

import feign.codec.ErrorDecoder;

/**
 * 只給 {@link BookClient} 使用的 Feign 設定，透過 @FeignClient(configuration = ...) 指定。
 * 刻意不加 @Configuration：加了會被 component scan 掃到，變成所有 Feign Client 共用的設定。
 */
public class BookClientConfig {

	@Bean
	ErrorDecoder bookServerErrorDecoder() {
		return new BookServerErrorDecoder();
	}
}
