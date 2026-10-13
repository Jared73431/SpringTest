package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.ContainersTestBase;
import com.example.demo.config.KafkaConfig;
import com.jayway.jsonpath.JsonPath;

/**
 * 第 1 課：收到訂單事件 → 出貨 → 回覆事件。
 *
 * <pre>
 * order-events（JSON）→ OrderPlacedListener → 寫入 shipment → shipment-events（JSON）
 * </pre>
 *
 * Order 服務送來的 JSON 沒有 Java 類別名稱；Shipping 服務用設定 spring.json.value.default.type
 * 轉成自己的 OrderPlacedEvent。兩邊只要 JSON 欄位對得上，各自的類別怎麼命名、放在哪個 package 都無所謂。
 */
@SpringBootTest
@AutoConfigureMockMvc
class L01_ShipAndReplyTest extends ContainersTestBase {

	@Autowired
	private MockMvc mockMvc;

	static String orderPlaced(UUID eventId, UUID orderId, int quantity) {
		return """
				{"eventId":"%s","orderId":"%s","customer":"alice","item":"book","quantity":%d,"occurredAt":"2026-10-13T08:00:00Z"}
				""".formatted(eventId, orderId, quantity);
	}

	@Test
	void orderPlaced_shouldBeShippedAndReplied() throws Exception {
		UUID orderId = UUID.randomUUID();

		sendJson(KafkaConfig.ORDER_EVENTS, orderId.toString(), orderPlaced(UUID.randomUUID(), orderId, 2));

		// 回覆：key 是訂單編號，value 是 JSON，沒有 __TypeId__ 標頭
		var replies = readRecords(KafkaConfig.SHIPMENT_EVENTS, orderId.toString());
		assertThat(replies).hasSize(1);
		var reply = replies.getFirst();
		assertThat(reply.headers().lastHeader("__TypeId__")).isNull();
		assertThat((String) JsonPath.read(reply.value(), "$.orderId")).isEqualTo(orderId.toString());
		assertThat((String) JsonPath.read(reply.value(), "$.status")).isEqualTo("SHIPPED");

		// 自己的資料庫也有出貨紀錄
		mockMvc.perform(get("/api/shipments/{orderId}", orderId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SHIPPED"));
	}

	@Test
	void findShipment_shouldReturn404_whenTheOrderHasNotBeenHandled() throws Exception {
		mockMvc.perform(get("/api/shipments/{orderId}", UUID.randomUUID()))
				.andExpect(status().isNotFound());
	}
}
