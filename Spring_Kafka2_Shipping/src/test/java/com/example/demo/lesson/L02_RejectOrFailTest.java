package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.ContainersTestBase;
import com.example.demo.config.KafkaConfig;
import com.example.demo.event.ShipmentEvent.Status;
import com.example.demo.shipment.ShippingService;
import com.jayway.jsonpath.JsonPath;

/**
 * 第 2 課：業務上的「拒絕」vs 技術上的「失敗」。
 *
 * <table>
 * <tr><th>情況</th><th>處理</th><th>Order 服務看到</th></tr>
 * <tr><td>一次超過 10 件</td><td>正常處理，回覆 REJECTED 與原因</td><td>訂單變成 REJECTED</td></tr>
 * <tr><td>JSON 壞掉、重試後仍然失敗</td><td>送到 order-events-dlt，由人處理</td><td>訂單一直是 PENDING</td></tr>
 * </table>
 *
 * 拒絕要「回覆」：如果用拋出例外表示拒絕，訊息會進 DLT，Order 服務永遠不知道結果。
 */
@SpringBootTest
class L02_RejectOrFailTest extends ContainersTestBase {

	@Autowired
	private ShippingService shippingService;

	@Test
	void tooManyItems_shouldBeRejectedAndReplied() {
		UUID orderId = UUID.randomUUID();

		sendJson(KafkaConfig.ORDER_EVENTS, orderId.toString(), L01_ShipAndReplyTest.orderPlaced(UUID.randomUUID(), orderId, 11));

		var replies = readRecords(KafkaConfig.SHIPMENT_EVENTS, orderId.toString());
		assertThat((String) JsonPath.read(replies.getFirst().value(), "$.status")).isEqualTo("REJECTED");
		assertThat((String) JsonPath.read(replies.getFirst().value(), "$.reason")).isEqualTo("quantity 11 exceeds 10");
		assertThat(shippingService.find(orderId)).hasValueSatisfying(s -> assertThat(s.status()).isEqualTo(Status.REJECTED));
	}

	@Test
	void malformedEvent_shouldGoToTheDeadLetterTopicWithoutAReply() {
		String key = UUID.randomUUID().toString();

		sendJson(KafkaConfig.ORDER_EVENTS, key, "this is not json");

		var deadLetters = readRecords(KafkaConfig.ORDER_EVENTS_DLT, key);
		assertThat(deadLetters).hasSize(1);
		assertThat(deadLetters.getFirst().value()).isEqualTo("this is not json");
	}
}
