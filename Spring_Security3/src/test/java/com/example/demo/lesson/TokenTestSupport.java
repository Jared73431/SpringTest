package com.example.demo.lesson;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/** 課程共用：用帳密換 Token、解開 Token 的內容 */
final class TokenTestSupport {

	private TokenTestSupport() {
	}

	static String tokenFor(MockMvc mvc, String username) throws Exception {
		String json = mvc.perform(post("/api/auth/token").with(httpBasic(username, "password")))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(json, "$.accessToken");
	}

	static String bearer(String token) {
		return "Bearer " + token;
	}

	/** JWT = header.payload.signature，前兩段是 Base64URL 編碼的 JSON：不需要任何金鑰就能解開 */
	static String decodePart(String token, int index) {
		return new String(Base64.getUrlDecoder().decode(token.split("\\.")[index]), StandardCharsets.UTF_8);
	}
}
