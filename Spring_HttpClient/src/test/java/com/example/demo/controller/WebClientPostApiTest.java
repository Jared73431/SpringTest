package com.example.demo.controller;

/**
 * /api/reactive/posts（WebClient，Reactive）的測試，案例定義在 {@link PostApiContractTest}。
 */
class WebClientPostApiTest extends PostApiContractTest {

	@Override
	String basePath() {
		return "/api/reactive/posts";
	}
}
