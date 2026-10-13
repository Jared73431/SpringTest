package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.ContainersTestBase;
import com.example.demo.config.KafkaConfig;
import com.jayway.jsonpath.JsonPath;

/**
 * 第 1 課：建立訂單、送出事件；兩個服務之間的約定是 JSON。
 *
 * <pre>
 * POST /api/orders → 寫入 orders（PENDING）→ 送出 OrderPlacedEvent 到 order-events → 202
 * </pre>
 *
 * 事件的 JSON 不帶 Java 類別名稱（標頭 __TypeId__）：Shipping 服務有自己的類別，不需要知道 Order 服務的 package。
 */
@SpringBootTest
@AutoConfigureMockMvc
class L01_PublishEventTest extends ContainersTestBase {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void placeOrder_shouldSaveAPendingOrderAndPublishAnEvent() throws Exception {
		String body = mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"customer":"alice","item":"book","quantity":2}
						"""))
				.andExpect(status().isAccepted())
				.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/orders/")))
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andReturn().getResponse().getContentAsString();
		String orderId = JsonPath.read(body, "$.id");

		// 查詢：還在等 Shipping 服務回覆
		mockMvc.perform(get("/api/orders/{id}", orderId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDING"));

		// 事件：key 是訂單編號，value 是 JSON，沒有 __TypeId__ 標頭
		var records = readRecords(KafkaConfig.ORDER_EVENTS, orderId);
		assertThat(records).hasSize(1);
		var record = records.getFirst();
		assertThat(record.headers().lastHeader("__TypeId__")).isNull();
		assertThat((String) JsonPath.read(record.value(), "$.orderId")).isEqualTo(orderId);
		assertThat((String) JsonPath.read(record.value(), "$.customer")).isEqualTo("alice");
		assertThat((Integer) JsonPath.read(record.value(), "$.quantity")).isEqualTo(2);
		assertThat((String) JsonPath.read(record.value(), "$.eventId")).isNotBlank();
	}

	@Test
	void placeOrder_shouldReturn400_whenQuantityIsNotPositive() throws Exception {
		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"customer":"alice","item":"book","quantity":0}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void findOrder_shouldReturn404_whenOrderDoesNotExist() throws Exception {
		mockMvc.perform(get("/api/orders/{id}", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").exists());
	}
}
