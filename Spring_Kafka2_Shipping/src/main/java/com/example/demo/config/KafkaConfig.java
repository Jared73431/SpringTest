package com.example.demo.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * 兩個服務之間的 topic：
 *
 * <pre>
 * order-events          Order 送出 → Shipping 接收
 * shipment-events       Shipping 送出 → Order 接收
 * order-events-dlt      Shipping 處理不了的訂單事件（例如 JSON 壞掉、重試後仍然失敗）
 * </pre>
 *
 * 兩個服務都宣告了這兩個 topic：先啟動哪一個都可以（已經存在則不變）。
 */
@Configuration
public class KafkaConfig {

	public static final String ORDER_EVENTS = "order-events";

	public static final String SHIPMENT_EVENTS = "shipment-events";

	// Spring Kafka 4 的預設名稱是「原本的 topic + -dlt」
	public static final String ORDER_EVENTS_DLT = ORDER_EVENTS + "-dlt";

	@Bean
	public NewTopic orderEventsTopic() {
		return TopicBuilder.name(ORDER_EVENTS).partitions(3).replicas(1).build();
	}

	@Bean
	public NewTopic shipmentEventsTopic() {
		return TopicBuilder.name(SHIPMENT_EVENTS).partitions(3).replicas(1).build();
	}

	@Bean
	public NewTopic orderEventsDeadLetterTopic() {
		return TopicBuilder.name(ORDER_EVENTS_DLT).partitions(3).replicas(1).build();
	}

	/** 處理失敗：每 500ms 重試，最多 2 次，還是失敗就送到 order-events-dlt */
	@Bean
	public DefaultErrorHandler errorHandler(DeadLetterPublishingRecoverer deadLetterRecoverer) {
		return new DefaultErrorHandler(deadLetterRecoverer, new FixedBackOff(500, 2));
	}

	/** 格式錯誤的訊息只有原始的 byte[]，用 ByteArraySerializer 送出；其他訊息用一般的 KafkaTemplate（JSON） */
	@Bean
	public DeadLetterPublishingRecoverer deadLetterRecoverer(KafkaTemplate<Object, Object> kafkaTemplate,
			ProducerFactory<Object, Object> producerFactory) {
		var bytesTemplate = new KafkaTemplate<>(producerFactory.copyWithConfigurationOverride(
				Map.of(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class)));

		Map<Class<?>, KafkaOperations<?, ?>> templates = new LinkedHashMap<>();
		templates.put(byte[].class, bytesTemplate);
		templates.put(Object.class, kafkaTemplate);
		return new DeadLetterPublishingRecoverer(templates);
	}
}
