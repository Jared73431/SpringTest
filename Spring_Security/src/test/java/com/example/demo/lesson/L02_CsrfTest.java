package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 第 2 課：CSRF —— 為什麼已經登入了，POST 還是 403？
 *
 * <p>
 * CSRF（跨站請求偽造）：你登入了銀行網站，又開了一個惡意網站；惡意網站的頁面偷偷送出一個「轉帳」表單到銀行，
 * 瀏覽器會<b>自動帶上</b>銀行的 cookie，銀行以為是你本人送出的。
 *
 * <p>
 * 防禦：會改變資料的請求（POST、PUT、DELETE）必須帶一個 CSRF token。token 只有銀行自己的頁面拿得到，
 * 惡意網站拿不到，所以偽造的請求會被拒絕（403）。GET 不改變資料，不需要 token。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class L02_CsrfTest {

	@Autowired
	private MockMvc mvc;

	// 帳密正確，但沒有 CSRF token：403
	@Test
	void post_shouldBeForbidden_withoutACsrfToken() throws Exception {
		mvc.perform(post("/api/messages").with(httpBasic("user", "password")).content("hi"))
				.andExpect(status().isForbidden());
	}

	// csrf()：測試幫忙放入正確的 token（瀏覽器中，表單會由頁面自動帶上 token）
	@Test
	void post_shouldBeAccepted_withACsrfToken() throws Exception {
		mvc.perform(post("/api/messages").with(httpBasic("user", "password")).with(csrf()).content("hi"))
				.andExpect(status().isOk())
				.andExpect(content().string("user says: hi"));
	}

	@LocalServerPort
	private int port;

	// 同樣的請求送到真正的伺服器，收到的卻是 401，不是 403：
	// 1. CSRF 檢查在 HTTP Basic 登入「之前」，被拒絕時還沒有登入
	// 2. 403 被轉到錯誤頁 /error，/error 也需要登入，而轉過去時不會再做一次 Basic 登入 → 401
	// MockMvc 不會轉到錯誤頁，所以上面的測試看到的是 403。用 curl 測試 POST 收到 401 時，先檢查是不是少了 CSRF token
	@Test
	void post_withoutACsrfToken_shouldLookLike401_onARealServer() throws Exception {
		var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/messages"))
				.header("Authorization",
						"Basic " + Base64.getEncoder().encodeToString("user:password".getBytes()))
				.POST(HttpRequest.BodyPublishers.ofString("hi"))
				.build();

		var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(401);
	}
}
