package com.example.demo.controller;

/**
 * /api/posts（RestClient，同步）的測試，案例定義在 {@link PostApiContractTest}。
 */
class RestClientPostApiTest extends PostApiContractTest {

	@Override
	String basePath() {
		return "/api/posts";
	}
}
