package com.example.demo.lesson;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 第 1 課：只加上 spring-boot-starter-security，什麼都不設定，會發生什麼事？
 *
 * <ul>
 * <li>所有路徑都需要登入</li>
 * <li>沒有登入時：瀏覽器導向登入頁；程式（API client）收到 401</li>
 * <li>兩種登入方式：表單登入（登入頁）、HTTP Basic（每個請求都帶帳密）</li>
 * <li>表單登入成功後，用 Session 記住「已經登入」</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
class L01_DefaultSecurityTest {

	@Autowired
	private MockMvc mvc;

	// 怎麼判斷是不是瀏覽器：看 Accept 標頭有沒有 text/html
	@Test
	void browser_shouldBeRedirectedToTheLoginPage() throws Exception {
		mvc.perform(get("/api/hello").accept(MediaType.TEXT_HTML))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/login"));
	}

	// 程式呼叫：401，並用 WWW-Authenticate 告訴對方可以用 HTTP Basic 登入
	@Test
	void apiClient_shouldGet401() throws Exception {
		mvc.perform(get("/api/hello").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Basic")));
	}

	@Test
	void loginPage_shouldBeGeneratedBySpringSecurity() throws Exception {
		mvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Please sign in")));
	}

	// HTTP Basic：每個請求都在 Authorization 標頭帶帳密（Base64 編碼，不是加密，所以一定要搭配 HTTPS）
	@Test
	void httpBasic_shouldLogInOnEveryRequest() throws Exception {
		mvc.perform(get("/api/hello").with(httpBasic("user", "password")))
				.andExpect(status().isOk())
				.andExpect(content().string("Hello, user!"));
	}

	@Test
	void formLogin_shouldRejectAWrongPassword() throws Exception {
		mvc.perform(formLogin().user("user").password("wrong"))
				.andExpect(unauthenticated())
				.andExpect(redirectedUrl("/login?error"));
	}

	// 表單登入成功後，伺服器把「已經登入」存在 Session；之後的請求帶著同一個 Session（瀏覽器的 JSESSIONID cookie）就不必再登入
	@Test
	void formLogin_shouldRememberTheUserInTheSession() throws Exception {
		var session = (MockHttpSession) mvc.perform(formLogin().user("user").password("password"))
				.andExpect(authenticated().withUsername("user"))
				.andReturn().getRequest().getSession();

		mvc.perform(get("/api/hello").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string("Hello, user!"));
	}
}
