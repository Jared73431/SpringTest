package com.example.demo.lesson;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.PostgresContainerTestBase;

/**
 * 第 1 課：路徑規則 —— 誰可以進哪裡？
 *
 * <pre>
 * /api/public/**   permitAll()          不需要登入
 * /api/admin/**    hasRole("ADMIN")     需要 ADMIN 角色
 * 其他             authenticated()      需要登入
 * </pre>
 *
 * 401：不知道你是誰（沒有登入）。403：知道你是誰，但你沒有權限。
 */
@SpringBootTest
@AutoConfigureMockMvc
class L01_AuthorizationRulesTest extends PostgresContainerTestBase {

	@Autowired
	private MockMvc mvc;

	@Test
	void publicPath_shouldBeOpenToEveryone() throws Exception {
		mvc.perform(get("/api/public/hello"))
				.andExpect(status().isOk())
				.andExpect(content().string("Hello, guest!"));
	}

	@Test
	void authenticatedPath_shouldReturn401_whenNotLoggedIn() throws Exception {
		mvc.perform(get("/api/me").accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
	}

	// 沒有列出的路徑落到 anyRequest().authenticated()：預設就需要登入，忘了設定也不會意外開放
	@Test
	void unlistedPath_shouldAlsoRequireLogin() throws Exception {
		mvc.perform(get("/api/anything").accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
	}

	@Test
	void adminPath_shouldReturn403_forAUserWithoutTheAdminRole() throws Exception {
		mvc.perform(get("/api/admin/stats").with(httpBasic("alice", "password"))).andExpect(status().isForbidden());
	}

	@Test
	void adminPath_shouldBeOpen_forAdmin() throws Exception {
		mvc.perform(get("/api/admin/stats").with(httpBasic("admin", "password")))
				.andExpect(status().isOk())
				.andExpect(content().string("Users: 3"));
	}
}
