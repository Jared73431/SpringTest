package com.example.demo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 所有整合測試共用一個 Kafka 與一個 PostgreSQL container（只啟動一次），不會連到本機的 Kafka 與資料庫。
 *
 * <p>
 * Shipping 服務不需要啟動：測試直接用 Kafka 的 client 扮演它，送出 / 讀取「JSON 字串」，
 * 也就是兩個服務之間真正的約定。
 */
public abstract class ContainersTestBase {

	@ServiceConnection
	protected static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka-native:4.2.1");

	@ServiceConnection
	protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine");

	static {
		KAFKA.start();
		POSTGRES.start();
	}

	/** 扮演 Shipping 服務：把 JSON 字串送到 topic */
	protected static void sendJson(String topic, String key, String json) {
		try (var producer = new KafkaProducer<String, String>(
				Map.<String, Object>of("bootstrap.servers", KAFKA.getBootstrapServers()), new StringSerializer(),
				new StringSerializer())) {
			producer.send(new ProducerRecord<>(topic, key, json));
		}
	}

	/** 從頭讀取 topic，回傳 key 相同的訊息（最多等 10 秒，至少要有一則） */
	protected static List<ConsumerRecord<String, String>> readRecords(String topic, String key) {
		try (var consumer = new KafkaConsumer<String, String>(Map.of(
				ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
				ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
				ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"), new StringDeserializer(), new StringDeserializer())) {
			consumer.subscribe(List.of(topic));
			List<ConsumerRecord<String, String>> found = new ArrayList<>();
			long deadline = System.currentTimeMillis() + 10_000;
			while (found.isEmpty() && System.currentTimeMillis() < deadline) {
				for (var record : consumer.poll(Duration.ofMillis(500))) {
					if (key.equals(record.key())) {
						found.add(record);
					}
				}
			}
			return found;
		}
	}
}
