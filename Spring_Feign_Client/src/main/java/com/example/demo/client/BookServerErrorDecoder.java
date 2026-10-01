package com.example.demo.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import feign.Response;
import feign.codec.ErrorDecoder;

/**
 * Server 回傳非 2xx 時，Feign 會呼叫 ErrorDecoder 決定要拋出什麼例外。
 * 預設的 ErrorDecoder 拋出 FeignException，Client 沒有處理就一律變成 500，Server 回傳的狀態碼與錯誤原因都會消失。
 * 這裡把狀態碼、Content-Type 與回應內容原封不動包進 {@link BookServerException}，
 * 交給 GlobalExceptionHandler 決定如何回應呼叫端。
 */
public class BookServerErrorDecoder implements ErrorDecoder {

	@Override
	public Exception decode(String methodKey, Response response) {
		return new BookServerException(response.status(), contentType(response), readBody(response));
	}

	private static String contentType(Response response) {
		Collection<String> values = response.headers().get("Content-Type");
		return values == null || values.isEmpty() ? null : values.iterator().next();
	}

	private static byte[] readBody(Response response) {
		if (response.body() == null) {
			return new byte[0];
		}
		try (InputStream body = response.body().asInputStream()) {
			return body.readAllBytes();
		} catch (IOException e) {
			// 讀不到內容時仍保留狀態碼，由 GlobalExceptionHandler 補上標準的 ProblemDetail
			return new byte[0];
		}
	}
}
