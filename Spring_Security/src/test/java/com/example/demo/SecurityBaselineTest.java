package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Baseline：鎖定重構前的行為（Spring Boot 的 Security 自動設定，帳密來自設定檔）。
 *
 * <p>
 * MockMvc 不經過 context path（/demo），所以路徑直接從 / 開始。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityBaselineTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private Environment environment;

	@Test
	void settings_shouldUseContextPathAndPort8080() {
		assertThat(environment.getProperty("server.servlet.context-path")).isEqualTo("/demo");
		assertThat(environment.getProperty("server.port")).isEqualTo("8080");
	}

	// 瀏覽器（Accept: text/html）沒有登入：導向自動產生的登入頁。
	// Spring Security 7（Boot 4）起 Location 是相對路徑 /login（Boot 3 是 http://localhost/login）
	@Test
	void browser_shouldBeRedirectedToLoginPage_whenNotLoggedIn() throws Exception {
		mvc.perform(get("/").accept(MediaType.TEXT_HTML))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	void loginPage_shouldBeGeneratedBySpringSecurity() throws Exception {
		mvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Please sign in")));
	}

	@Test
	void formLogin_shouldSucceed_withTheConfiguredUser() throws Exception {
		mvc.perform(formLogin().user("Jared").password("password"))
				.andExpect(authenticated().withUsername("Jared"))
				.andExpect(redirectedUrl("/"));
	}

	@Test
	void formLogin_shouldFail_withAWrongPassword() throws Exception {
		mvc.perform(formLogin().user("Jared").password("wrong"))
				.andExpect(unauthenticated())
				.andExpect(redirectedUrl("/login?error"));
	}

	// HTTP Basic 也預設啟用
	@Test
	void httpBasic_shouldReturnTheDemoText() throws Exception {
		mvc.perform(get("/").with(httpBasic("Jared", "password")))
				.andExpect(status().isOk())
				.andExpect(content().string("Spring Boot + Spring Security Configuration Demo"));
	}

	// 非瀏覽器的請求（沒有 Accept: text/html）沒有登入：401
	@Test
	void apiClient_shouldGet401_whenNotLoggedIn() throws Exception {
		mvc.perform(get("/").accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
	}
}
