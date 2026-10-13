package com.example.demo.lesson;

import static com.example.demo.lesson.TokenTestSupport.bearer;
import static com.example.demo.lesson.TokenTestSupport.decodePart;
import static com.example.demo.lesson.TokenTestSupport.tokenFor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 第 2 課：伺服器怎麼確認 Token 是真的？
 *
 * <p>
 * 每個請求都用<b>公鑰</b>檢查簽章與到期時間，不需要查資料庫、也不需要 Session：
 * <ul>
 * <li>內容被改過 → 簽章對不上 → 401</li>
 * <li>超過 exp → 401</li>
 * <li>用別的私鑰簽的（例如攻擊者自己簽一個 ADMIN 的 Token）→ 公鑰驗不過 → 401</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
class L02_VerifyTokenTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Test
	void validToken_shouldBeAccepted() throws Exception {
		mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(mvc, "alice"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("alice"))
				.andExpect(jsonPath("$.scope").value("USER"));
	}

	// WWW-Authenticate 告訴呼叫端「要用 Bearer Token」。Spring Security 7 還會附上 resource_metadata：
	// 一個說明「這個 API 接受哪裡簽發的 Token」的網址（OAuth2 的標準 RFC 9728，本專案不使用）
	@Test
	void missingToken_shouldBeRejectedWith401() throws Exception {
		mvc.perform(get("/api/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.startsWith("Bearer")));
	}

	// 把 payload 的 scope 從 USER 改成 ADMIN USER，簽章不變
	@Test
	void tamperedToken_shouldBeRejected() throws Exception {
		String[] parts = tokenFor(mvc, "alice").split("\\.");
		String forgedPayload = decodePart(String.join(".", parts), 1).replace("\"USER\"", "\"ADMIN USER\"");
		parts[1] = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(forgedPayload.getBytes(StandardCharsets.UTF_8));

		mvc.perform(get("/api/admin/stats").header(HttpHeaders.AUTHORIZATION, bearer(String.join(".", parts))))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE,
						org.hamcrest.Matchers.containsString("invalid_token")));
	}

	@Test
	void expiredToken_shouldBeRejected() throws Exception {
		Instant twoHoursAgo = Instant.now().minusSeconds(7200);
		var claims = JwtClaimsSet.builder()
				.subject("alice")
				.issuedAt(twoHoursAgo)
				.expiresAt(twoHoursAgo.plusSeconds(900))
				.claim("scope", "USER")
				.build();
		String expired = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

		mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(expired)))
				.andExpect(status().isUnauthorized());
	}

	// 攻擊者用自己的私鑰簽一個 ADMIN 的 Token：伺服器的公鑰驗不過
	@Test
	void tokenSignedWithAnotherKey_shouldBeRejected() throws Exception {
		var generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		var attackerKeys = generator.generateKeyPair();
		var attackerEncoder = NimbusJwtEncoder.withKeyPair((RSAPublicKey) attackerKeys.getPublic(),
				(RSAPrivateKey) attackerKeys.getPrivate()).build();
		var claims = JwtClaimsSet.builder()
				.subject("attacker")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(900))
				.claim("scope", "ADMIN USER")
				.build();
		String forged = attackerEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

		mvc.perform(get("/api/admin/stats").header(HttpHeaders.AUTHORIZATION, bearer(forged)))
				.andExpect(status().isUnauthorized());
	}
}
