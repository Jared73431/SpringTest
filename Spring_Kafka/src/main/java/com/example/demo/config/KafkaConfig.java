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

import com.example.demo.shipping.ShippingRejectedException;

/**
 * Topic 與錯誤處理。
 *
 * <pre>
 * orders       訂單事件，3 個 partition（同時最多 3 個 consumer 分工處理）
 * orders-dlt   Dead Letter Topic：重試後仍然失敗、或格式錯誤的訊息放到這裡，不會卡住後面的訊息（第 5 課）
 * </pre>
 */
@Configuration
public class KafkaConfig {

	public static final String ORDERS_TOPIC = "orders";

	// Spring Kafka 4 的預設名稱是「原本的 topic + -dlt」（舊版是 .DLT）
	public static final String DEAD_LETTER_TOPIC = ORDERS_TOPIC + "-dlt";

	// 應用程式啟動時，topic 不存在就建立（已經存在則不變）
	@Bean
	public NewTopic ordersTopic() {
		return TopicBuilder.name(ORDERS_TOPIC).partitions(3).replicas(1).build();
	}

	// 失敗的訊息送到 DLT「同一個編號」的 partition，所以 partition 數量要一樣
	@Bean
	public NewTopic deadLetterTopic() {
		return TopicBuilder.name(DEAD_LETTER_TOPIC).partitions(3).replicas(1).build();
	}

	/**
	 * @KafkaListener 處理訊息拋出例外時：
	 * <ul>
	 * <li>每 500ms 重試，最多 2 次（暫時性的錯誤，例如資料庫斷線，再試可能就好）</li>
	 * <li>還是失敗，送到 orders-dlt，繼續處理下一則訊息</li>
	 * <li>ShippingRejectedException（業務規則不允許）重試也沒用：不重試，直接送到 DLT</li>
	 * </ul>
	 * Spring Boot 會把這個 Bean 自動套用到所有的 @KafkaListener。
	 */
	@Bean
	public DefaultErrorHandler errorHandler(DeadLetterPublishingRecoverer deadLetterRecoverer) {
		var errorHandler = new DefaultErrorHandler(deadLetterRecoverer, new FixedBackOff(500, 2));
		errorHandler.addNotRetryableExceptions(ShippingRejectedException.class);
		return errorHandler;
	}

	/**
	 * 把失敗的訊息原樣送到 DLT。
	 *
	 * <p>
	 * 格式錯誤（無法轉成物件）的訊息只有原始的 byte[]，要用 ByteArraySerializer 送出；
	 * 其他訊息用一般的 KafkaTemplate（JSON）。依 value 的型別選擇 template，byte[] 要放在前面。
	 */
	@Bean
	public DeadLetterPublishingRecoverer deadLetterRecoverer(KafkaTemplate<Object, Object> kafkaTemplate,
			ProducerFactory<Object, Object> producerFactory) {
		// 複製 Spring Boot 建好的 ProducerFactory，只換掉 value 的 serializer：連線位址等設定都沿用
		// （自己用 KafkaProperties 組設定的話，測試中 @ServiceConnection 提供的位址不在裡面，會連到 localhost:9092）
		var bytesTemplate = new KafkaTemplate<>(producerFactory.copyWithConfigurationOverride(
				Map.of(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class)));

		Map<Class<?>, KafkaOperations<?, ?>> templates = new LinkedHashMap<>();
		templates.put(byte[].class, bytesTemplate);
		templates.put(Object.class, kafkaTemplate);
		return new DeadLetterPublishingRecoverer(templates);
	}
}
