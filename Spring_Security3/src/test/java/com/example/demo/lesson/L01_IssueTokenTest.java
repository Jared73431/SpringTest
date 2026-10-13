package com.example.demo.lesson;

import static com.example.demo.lesson.TokenTestSupport.decodePart;
import static com.example.demo.lesson.TokenTestSupport.tokenFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/**
 * 第 1 課：用帳密換一個 JWT。
 *
 * <pre>
 * JWT = header.payload.signature（三段，用 . 分隔）
 * header     {"alg":"RS256"}                     用什麼演算法簽章
 * payload    {"sub":"alice","scope":"USER",...}  Token 的內容（claims）
 * signature  用私鑰對前兩段簽章的結果
 * </pre>
 *
 * 前兩段只是 Base64URL 編碼，<b>任何人都能解開</b>：JWT 防的是「被竄改」，不是「被看到」。
 */
@SpringBootTest
@AutoConfigureMockMvc
class L01_IssueTokenTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void token_shouldBeIssued_forCorrectCredentials() throws Exception {
		mvc.perform(post("/api/auth/token").with(httpBasic("alice", "password")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(900));
	}

	@Test
	void token_shouldNotBeIssued_forAWrongPassword() throws Exception {
		mvc.perform(post("/api/auth/token").with(httpBasic("alice", "wrong"))).andExpect(status().isUnauthorized());
	}

	@Test
	void token_shouldHaveThreePartsSeparatedByDots() throws Exception {
		assertThat(tokenFor(mvc, "alice").split("\\.")).hasSize(3);
	}

	// 不需要任何金鑰就能讀出內容：不要在 Token 中放密碼或敏感資料
	@Test
	void tokenContent_shouldBeReadableByAnyone() throws Exception {
		String token = tokenFor(mvc, "alice");

		String header = decodePart(token, 0);
		String payload = decodePart(token, 1);

		assertThat((String) JsonPath.read(header, "$.alg")).isEqualTo("RS256"); // RSA 簽章
		assertThat((String) JsonPath.read(payload, "$.iss")).isEqualTo("spring-security3");
		assertThat((String) JsonPath.read(payload, "$.sub")).isEqualTo("alice");
		assertThat((String) JsonPath.read(payload, "$.scope")).isEqualTo("USER");
		long issuedAt = ((Number) JsonPath.read(payload, "$.iat")).longValue();
		long expiresAt = ((Number) JsonPath.read(payload, "$.exp")).longValue();
		assertThat(expiresAt - issuedAt).isEqualTo(15 * 60); // 15 分鐘後到期
	}

	@Test
	void adminToken_shouldContainBothScopes() throws Exception {
		assertThat((String) JsonPath.read(decodePart(tokenFor(mvc, "admin"), 1), "$.scope")).isEqualTo("ADMIN USER");
	}
}
