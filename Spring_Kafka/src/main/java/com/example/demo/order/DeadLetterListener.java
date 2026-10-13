package com.example.demo.order;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import com.example.demo.config.KafkaConfig;

/**
 * 監看 Dead Letter Topic：記錄送進來的訊息與失敗原因（第 5 課）。實務上會發出警示，由人處理後重新送出。
 *
 * <p>
 * DLT 裡可能有格式錯誤的訊息，所以 value 直接當成字串讀，不轉成 OrderEvent。
 * 失敗原因在 DeadLetterPublishingRecoverer 加上的標頭 kafka_dlt-exception-message。
 */
@Component
public class DeadLetterListener {

	private static final Logger log = LoggerFactory.getLogger(DeadLetterListener.class);

	public record DeadLetter(String key, String value, String reason) {
	}

	private final List<DeadLetter> received = new CopyOnWriteArrayList<>();

	@KafkaListener(topics = KafkaConfig.DEAD_LETTER_TOPIC, groupId = "dead-letter-monitor",
			properties = "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer")
	public void onDeadLetter(ConsumerRecord<String, String> record) {
		var reasonHeader = record.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_MESSAGE);
		String reason = reasonHeader == null ? null : new String(reasonHeader.value(), StandardCharsets.UTF_8);
		received.add(new DeadLetter(record.key(), record.value(), reason));
		log.warn("Dead letter: key={} reason={}", record.key(), reason);
	}

	public List<DeadLetter> received() {
		return List.copyOf(received);
	}
}
