package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.PostgresContainerTestBase;
import com.example.demo.report.ReportService;

/**
 * 第 3 課：方法層級的權限 —— @PreAuthorize。
 *
 * <p>
 * 路徑規則保護的是「網址」；@PreAuthorize 保護的是「方法」。/api/reports/sales 只要求登入，
 * 但它呼叫的 ReportService.salesReport() 要求 ADMIN。之後就算有別的 Controller 或排程呼叫這個方法，也一樣會檢查。
 */
@SpringBootTest
@AutoConfigureMockMvc
class L03_MethodSecurityTest extends PostgresContainerTestBase {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ReportService reportService;

	@Test
	void report_shouldReturn403_whenTheUserIsNotAdmin() throws Exception {
		mvc.perform(get("/api/reports/sales").with(httpBasic("alice", "password"))).andExpect(status().isForbidden());
	}

	@Test
	void report_shouldBeReturned_forAdmin() throws Exception {
		mvc.perform(get("/api/reports/sales").with(httpBasic("admin", "password")))
				.andExpect(status().isOk())
				.andExpect(content().string("Sales report: 42 books sold"));
	}

	// 不經過 HTTP 直接呼叫也會檢查：沒有登入者時直接拒絕
	@Test
	void calledDirectly_shouldBeRejected_withoutALoggedInUser() {
		assertThatThrownBy(reportService::salesReport).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
	}

	// @WithMockUser：測試中直接指定「目前是誰、有什麼角色」，不必真的登入
	@Test
	@WithMockUser(roles = "ADMIN")
	void calledDirectly_shouldWork_forAdmin() {
		assertThat(reportService.salesReport()).isEqualTo("Sales report: 42 books sold");
	}
}
