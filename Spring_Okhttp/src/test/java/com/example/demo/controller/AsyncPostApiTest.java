package com.example.demo.controller;

/**
 * /api/async/posts（OkHttp enqueue + CompletableFuture）的測試，案例定義在 {@link PostApiContractTest}。
 */
class AsyncPostApiTest extends PostApiContractTest {

	@Override
	String basePath() {
		return "/api/async/posts";
	}
}
