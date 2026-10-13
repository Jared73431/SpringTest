package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.ContainersTestBase;
import com.example.demo.config.KafkaConfig;
import com.example.demo.order.Order;
import com.example.demo.order.OrderRepository;
import com.example.demo.order.OrderService;

/**
 * 第 3 課：資料庫與 Kafka 是兩個系統 —— 怎麼不重複、怎麼不遺失。
 *
 * <ul>
 * <li><b>收</b>：Kafka 是「至少送達一次」，同一個回覆可能送達兩次。
 * 「記住處理過的 eventId」和「更新訂單」放在同一個資料庫交易：第二次直接略過</li>
 * <li><b>送</b>：「寫入訂單」和「送出事件」沒辦法放在同一個交易。
 * 先寫入、再送出、送出失敗就 rollback：大部分情況兩邊一致；
 * 剩下「送出成功、commit 失敗」的情況要靠 Outbox pattern（見 README）</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
class L03_ConsistencyTest extends ContainersTestBase {

	@MockitoSpyBean
	private OrderService orderService;

	@MockitoSpyBean
	private OrderRepository orderRepository;

	@MockitoSpyBean
	private KafkaTemplate<?, ?> kafkaTemplate;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcClient jdbcClient;

	@Test
	void duplicateShipmentEvent_shouldBeAppliedOnlyOnce() {
		Order order = orderService.place("dave", "lamp", 1);
		String event = """
				{"eventId":"%s","orderId":"%s","status":"SHIPPED","reason":null,"occurredAt":"2026-10-13T08:00:00Z"}
				""".formatted(UUID.randomUUID(), order.id());

		sendJson(KafkaConfig.SHIPMENT_EVENTS, order.id().toString(), event);
		sendJson(KafkaConfig.SHIPMENT_EVENTS, order.id().toString(), event); // 模擬重複送達

		// 兩則都收到了，但只有第一則更新訂單
		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> verify(orderService, times(2))
				.applyShipmentResult(argThat(e -> e != null && order.id().equals(e.orderId()))));
		verify(orderRepository, times(1)).updateStatus(eq(order.id()), eq(Order.Status.SHIPPED), any());
	}

	@SuppressWarnings("unchecked")
	@Test
	void placeOrder_shouldNotSaveTheOrder_whenTheEventCannotBeSent() throws Exception {
		String customer = "erin-" + UUID.randomUUID();
		doThrow(new KafkaException("broker unavailable")).when((KafkaTemplate<String, Object>) kafkaTemplate)
				.send(eq(KafkaConfig.ORDER_EVENTS), anyString(), any());

		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"customer":"%s","item":"desk","quantity":1}
						""".formatted(customer)))
				.andExpect(status().isServiceUnavailable());

		// 寫入訂單的交易 rollback 了：不會留下一筆「永遠等不到回覆」的 PENDING 訂單
		Long count = jdbcClient.sql("SELECT count(*) FROM orders WHERE customer = :customer")
				.param("customer", customer).query(Long.class).single();
		assertThat(count).isZero();
	}
}
