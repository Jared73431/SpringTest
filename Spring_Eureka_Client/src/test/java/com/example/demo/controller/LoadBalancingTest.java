package com.example.demo.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

/**
 * 同一個服務名稱有兩個實例時，LoadBalancer（預設 Round Robin）會把請求輪流送到兩台。
 * 兩個 WireMock 代表兩台 Provider，各自回傳不同的實例 id。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class LoadBalancingTest {

	@RegisterExtension
	static WireMockExtension providerA = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort())
			.build();

	@RegisterExtension
	static WireMockExtension providerB = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort())
			.build();

	@DynamicPropertySource
	static void discovery(DynamicPropertyRegistry registry) {
		registry.add("eureka.client.enabled", () -> "false");
		registry.add("spring.cloud.discovery.client.simple.instances.service-provider[0].uri", providerA::baseUrl);
		registry.add("spring.cloud.discovery.client.simple.instances.service-provider[1].uri", providerB::baseUrl);
	}

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void hello_shouldAlternateBetweenInstances_whenTwoProvidersRegistered() {
		providerA.stubFor(get("/api/hello").willReturn(aResponse().withBody("Hello from A")));
		providerB.stubFor(get("/api/hello").willReturn(aResponse().withBody("Hello from B")));

		List<String> responses = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			responses.add(restTemplate.getForObject("/api/hello", String.class));
		}

		// Round Robin 的起點是隨機的，所以只檢查「相鄰兩次一定不同台」與兩台各處理一半
		assertThat(responses.get(0)).isNotEqualTo(responses.get(1));
		assertThat(responses).containsOnly("Hello from A", "Hello from B");
		assertThat(responses).filteredOn("Hello from A"::equals).hasSize(2);
	}
}
