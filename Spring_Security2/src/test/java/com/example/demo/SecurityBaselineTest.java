package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Baseline：鎖定重構前的行為。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class SecurityBaselineTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ApplicationContext context;

	@Test
	void root_shouldRequireLogin() throws Exception {
		mvc.perform(get("/").accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
		mvc.perform(get("/").with(user("someone")))
				.andExpect(status().isOk())
				.andExpect(content().string("Spring Boot + Spring Security Configuration Demo"));
	}

	// DemoWebSecurityConfig 只有 @EnableWebSecurity：Spring Security 6 起 @EnableWebSecurity 不再包含 @Configuration，
	// component scan 不會載入這個類別，所以 webSecurityCustomizer 不存在，/ignore1 仍然需要登入
	@Test
	void webSecurityCustomizer_shouldNotBeLoaded() {
		assertThat(context.getBeansOfType(WebSecurityCustomizer.class)).isEmpty();
	}

	@Test
	void ignore1_shouldStillRequireLogin() throws Exception {
		mvc.perform(get("/ignore1").accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
	}

	// 沒有設定帳密：Spring Boot 產生一個隨機密碼（帳號 user），印在 log
	@Test
	void startup_shouldLogAGeneratedPassword(CapturedOutput output) {
		assertThat(output.getOut()).contains("Using generated security password");
	}
}
