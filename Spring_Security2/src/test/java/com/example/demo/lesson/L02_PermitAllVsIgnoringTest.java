package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import java.security.Principal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.PostgresContainerTestBase;

/**
 * 第 2 課：permitAll() 和 web.ignoring() 都能讓路徑「不需要登入」，差在哪裡？
 *
 * <pre>
 *                  Security 的 filter   知道登入者是誰   安全性標頭（X-Content-Type-Options…）
 * permitAll()      仍然執行             知道             有
 * web.ignoring()   整個跳過             不知道           沒有
 * </pre>
 *
 * 修正前的 Spring_Security2 想用 web.ignoring() 開放 /ignore1（但設定類別沒有被載入，所以沒有作用）。
 * Spring Security 建議改用 permitAll()；web.ignoring() 只適合完全不需要保護的靜態檔案。
 *
 * <p>
 * 主程式只用 permitAll()；這裡另外加一個 ignoring 的路徑來比較。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(L02_PermitAllVsIgnoringTest.IgnoredPathConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class L02_PermitAllVsIgnoringTest extends PostgresContainerTestBase {

	@TestConfiguration
	static class IgnoredPathConfig {

		@Bean
		WebSecurityCustomizer ignoreOnePath() {
			return web -> web.ignoring().requestMatchers("/test/ignored");
		}

		@Bean
		IgnoredController ignoredController() {
			return new IgnoredController();
		}
	}

	// 和 /api/public/hello 一模一樣，只是路徑被 ignoring
	@RestController
	static class IgnoredController {

		@GetMapping("/test/ignored")
		String hello(Principal principal) {
			return "Hello, " + (principal == null ? "guest" : principal.getName()) + "!";
		}
	}

	@Autowired
	private MockMvc mvc;

	@Test
	void permitAll_shouldStillKnowWhoIsLoggedIn_andAddSecurityHeaders() throws Exception {
		mvc.perform(get("/api/public/hello").with(httpBasic("alice", "password")))
				.andExpect(content().string("Hello, alice!"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"));
	}

	@Test
	void ignoring_shouldSkipSecurityEntirely() throws Exception {
		mvc.perform(get("/test/ignored").with(httpBasic("alice", "password")))
				.andExpect(content().string("Hello, guest!")) // 帶了帳密也不會被處理
				.andExpect(header().doesNotExist("X-Content-Type-Options"));
	}

	@Test
	void ignoring_shouldBeWarnedAgainstAtStartup(CapturedOutput output) {
		assertThat(output.getOut()).contains("You are asking Spring Security to ignore");
	}
}
