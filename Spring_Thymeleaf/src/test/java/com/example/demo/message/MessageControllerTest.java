package com.example.demo.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 伺服器端渲染的測試：不需要開瀏覽器，用 MockMvc 驗證 view 名稱、Model、重新導向、flash 訊息，以及產生的 HTML。
 * \@WebMvcTest 只啟動 Web 層；MessageService 是記憶體的實作，直接 @Import 進來。
 */
@WebMvcTest(MessageController.class)
@Import(MessageService.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // 每個測試重新開始，留言不互相影響
class MessageControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MessageService messageService;

	@Test
	void home_shouldRedirectToMessages() throws Exception {
		mockMvc.perform(get("/")).andExpect(redirectedUrl("/messages"));
	}

	@Test
	void list_shouldRenderMessagesView_withEmptyFormAndMessages() throws Exception {
		messageService.add("Amy", "第一則留言");

		mockMvc.perform(get("/messages"))
				.andExpect(status().isOk())
				.andExpect(view().name("messages"))
				.andExpect(model().attribute("messages", hasSize(1)))
				.andExpect(model().attributeExists("form"))
				.andExpect(content().string(containsString("第一則留言")))
				.andExpect(content().string(containsString("留言（<span>1</span>）")));
	}

	@Test
	void list_shouldShowEmptyMessage_whenNoMessages() throws Exception {
		mockMvc.perform(get("/messages")).andExpect(content().string(containsString("目前沒有留言")));
	}

	// PRG：成功後重新導向，flash 訊息只在下一個請求出現
	@Test
	void add_shouldRedirectWithFlashNotice_whenValid() throws Exception {
		mockMvc.perform(post("/messages").param("author", "Amy").param("content", "哈囉"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/messages"))
				.andExpect(flash().attribute("notice", "已送出留言"));

		assertThat(messageService.findAll()).singleElement()
				.satisfies(message -> assertThat(message.content()).isEqualTo("哈囉"));
	}

	// 驗證失敗：不是 400，而是回到同一頁（200），顯示錯誤並保留使用者輸入的內容
	@Test
	void add_shouldRerenderFormWithErrors_whenInvalid() throws Exception {
		mockMvc.perform(post("/messages").param("author", "Amy").param("content", " "))
				.andExpect(status().isOk())
				.andExpect(view().name("messages"))
				.andExpect(model().attributeHasFieldErrors("form", "content"))
				.andExpect(content().string(containsString("請輸入留言")))
				.andExpect(content().string(containsString("value=\"Amy\""))); // 填回剛輸入的名稱

		assertThat(messageService.findAll()).isEmpty();
	}

	// th:text 會跳脫 HTML：使用者輸入的 <script> 只會顯示成文字（防止 XSS）
	@Test
	void list_shouldEscapeHtmlInUserInput() throws Exception {
		messageService.add("駭客", "<script>alert('xss')</script>");

		mockMvc.perform(get("/messages"))
				.andExpect(content().string(containsString("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;")))
				.andExpect(content().string(not(containsString("<script>alert"))));
	}
}
