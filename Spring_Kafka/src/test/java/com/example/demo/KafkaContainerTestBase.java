package com.example.demo;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.kafka.KafkaContainer;

/**
 * 所有整合測試共用一個 Kafka container（只啟動一次）。
 * apache/kafka-native：用 GraalVM 編譯的 Kafka，啟動只要一兩秒，適合測試；正式環境用 apache/kafka。
 */
public abstract class KafkaContainerTestBase {

	@ServiceConnection
	protected static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka-native:4.2.1");

	static {
		KAFKA.start();
	}
}
