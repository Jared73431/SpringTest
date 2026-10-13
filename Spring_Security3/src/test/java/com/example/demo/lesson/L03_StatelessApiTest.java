package com.example.demo.lesson;

import static com.example.demo.lesson.TokenTestSupport.bearer;
import static com.example.demo.lesson.TokenTestSupport.tokenFor;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 第 3 課：無狀態的 API 與權限。
 *
 * <ul>
 * <li>不建立 Session：每個請求都自己帶 Token，伺服器不記得任何人 → 可以任意增加主機，不必共用 Session</li>
 * <li>不需要 CSRF token：CSRF 利用的是瀏覽器自動帶 cookie，Bearer Token 要由程式明確放進標頭</li>
 * <li>只接受 Token：其他路徑不接受 HTTP Basic（帳密只用來換 Token）</li>
 * <li>權限來自 Token 的 scope：scope 有 ADMIN → 權限 SCOPE_ADMIN</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
class L03_StatelessApiTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void api_shouldNotCreateASession() throws Exception {
		mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(mvc, "alice"))))
				.andExpect(status().isOk())
				.andExpect(request().sessionAttributeDoesNotExist("SPRING_SECURITY_CONTEXT"))
				.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
	}

	// 和 Spring_Security 第 2 課不同：沒有 CSRF token 的 POST 也能成功
	@Test
	void post_shouldNotNeedACsrfToken() throws Exception {
		mvc.perform(post("/api/messages").header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(mvc, "alice")))
				.content("hi"))
				.andExpect(status().isOk())
				.andExpect(content().string("alice says: hi"));
	}

	// API 只接受 Bearer Token：帳密只能拿去換 Token
	@Test
	void api_shouldNotAcceptHttpBasic() throws Exception {
		mvc.perform(get("/api/me").with(httpBasic("alice", "password"))).andExpect(status().isUnauthorized());
	}

	@Test
	void adminPath_shouldReturn403_forATokenWithoutTheAdminScope() throws Exception {
		mvc.perform(get("/api/admin/stats").header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(mvc, "alice"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminPath_shouldBeOpen_forATokenWithTheAdminScope() throws Exception {
		mvc.perform(get("/api/admin/stats").header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(mvc, "admin"))))
				.andExpect(status().isOk())
				.andExpect(content().string("Users: 2"));
	}
}
