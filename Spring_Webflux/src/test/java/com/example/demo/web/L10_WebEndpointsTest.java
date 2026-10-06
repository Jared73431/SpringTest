package com.example.demo.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.example.demo.web.BlockingController.BlockingResult;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * 第 10 課：WebFlux 端點。
 *
 * <p>
 * 使用 RANDOM_PORT 啟動真正的 Netty，才能觀察 event loop 執行緒（MOCK 環境不會經過 Netty）。
 * 串流相關的延遲縮短，讓測試快速完成。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"demo.numbers.delay=10ms", "demo.ticks.interval=50ms", "demo.blocking.delay=10ms" })
@AutoConfigureWebTestClient
class L10_WebEndpointsTest {

	@Autowired
	private WebTestClient webTestClient;

	// ---- Annotated Controller vs Functional Endpoint：結果完全相同 ----

	@Test
	void greet_shouldReturnGreeting_whenUsingAnnotatedController() {
		webTestClient.get().uri("/api/greetings/{name}", "Amy").exchange()
				.expectStatus().isOk()
				.expectBody(Greeting.class).isEqualTo(new Greeting("Hello, Amy!"));
	}

	@Test
	void greet_shouldReturnGreeting_whenUsingFunctionalEndpoint() {
		webTestClient.get().uri("/api/fn/greetings/{name}", "Amy").exchange()
				.expectStatus().isOk()
				.expectBody(Greeting.class).isEqualTo(new Greeting("Hello, Amy!"));
	}

	// ---- 同一個 Flux，不同的 Content-Type ----

	@Test
	void numbers_shouldReturnJsonArray_whenAcceptIsJson() {
		webTestClient.get().uri("/api/numbers?count=3").accept(MediaType.APPLICATION_JSON).exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBodyList(Square.class).isEqualTo(List.of(new Square(1, 1), new Square(2, 4), new Square(3, 9)));
	}

	@Test
	void numbers_shouldStreamOneJsonPerLine_whenAcceptIsNdjson() {
		Flux<Square> body = webTestClient.get().uri("/api/numbers?count=3").accept(MediaType.APPLICATION_NDJSON)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_NDJSON)
				.returnResult(Square.class).getResponseBody();

		StepVerifier.create(body).expectNext(new Square(1, 1), new Square(2, 4), new Square(3, 9)).verifyComplete();
	}

	@Test
	void numbers_shouldReturn400ProblemDetail_whenCountIsOutOfRange() {
		webTestClient.get().uri("/api/numbers?count=0").accept(MediaType.APPLICATION_JSON).exchange()
				.expectStatus().isBadRequest()
				.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody().jsonPath("$.status").isEqualTo(400);
	}

	// ---- SSE：無限串流，客戶端取得需要的數量後取消 ----

	@Test
	void ticks_shouldStreamServerSentEvents() {
		Flux<ServerSentEvent<Tick>> body = webTestClient.get().uri("/api/ticks")
				.accept(MediaType.TEXT_EVENT_STREAM).exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
				.returnResult(new ParameterizedTypeReference<ServerSentEvent<Tick>>() {
				}).getResponseBody();

		StepVerifier.create(body.take(3))
				.assertNext(event -> assertTick(event, 0))
				.assertNext(event -> assertTick(event, 1))
				.assertNext(event -> assertTick(event, 2))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	private static void assertTick(ServerSentEvent<Tick> event, long sequence) {
		assertThat(event.event()).isEqualTo("tick");
		assertThat(event.id()).isEqualTo(String.valueOf(sequence));
		assertThat(event.data()).isNotNull();
		assertThat(event.data().sequence()).isEqualTo(sequence);
	}

	// ---- 阻塞呼叫在哪個執行緒上執行 ----

	// Netty event loop 的執行緒名稱：正式執行時使用 Reactor Netty 的全域資源（reactor-http-nio-* / reactor-http-epoll-*）；
	// 測試中由 Spring 的 ReactorResourceFactory 建立專用資源（webflux-http-*）。兩者都是 event loop
	@Test
	void onEventLoop_shouldBlockNettyEventLoopThread() {
		webTestClient.get().uri("/api/blocking/on-event-loop").exchange()
				.expectStatus().isOk()
				.expectBody(BlockingResult.class)
				.value(result -> assertThat(result.thread()).matches("(reactor|webflux)-http-.+"));
	}

	@Test
	void onBoundedElastic_shouldRunBlockingCallOffTheEventLoop() {
		webTestClient.get().uri("/api/blocking/on-bounded-elastic").exchange()
				.expectStatus().isOk()
				.expectBody(BlockingResult.class)
				.value(result -> assertThat(result.thread()).startsWith("boundedElastic-"));
	}

	// ---- 訊息：Sinks 廣播（第 2 課的 Hot 在 HTTP 上的樣子） ----

	private Message send(String text) {
		return webTestClient.post().uri("/api/messages").bodyValue(new MessageRequest(text)).exchange()
				.expectStatus().isCreated()
				.expectBody(Message.class).returnResult().getResponseBody();
	}

	/** 開啟 SSE 連線，只保留訊息的文字（略過連線時的註解事件） */
	private Flux<String> openStream() {
		return webTestClient.get().uri("/api/messages").accept(MediaType.TEXT_EVENT_STREAM).exchange()
				.expectStatus().isOk()
				.returnResult(new ParameterizedTypeReference<ServerSentEvent<Message>>() {
				}).getResponseBody()
				.filter(event -> event.data() != null)
				.map(event -> event.data().text());
	}

	@Test
	void send_shouldReturn201WithLocation() {
		webTestClient.post().uri("/api/messages").bodyValue(new MessageRequest("哈囉")).exchange()
				.expectStatus().isCreated()
				.expectHeader().value("Location", location -> assertThat(location).matches("/api/messages/\\d+"))
				.expectBody().jsonPath("$.text").isEqualTo("哈囉");
	}

	@Test
	void send_shouldReturn400ProblemDetail_whenTextIsBlank() {
		webTestClient.post().uri("/api/messages").bodyValue(new MessageRequest(" ")).exchange()
				.expectStatus().isBadRequest()
				.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void stream_shouldBroadcastToAllConnectedClients_butNotToLateOnes() {
		Flux<String> alice = openStream();
		Flux<String> bob = openStream();
		send("第一則");

		Flux<String> late = openStream(); // 第一則送出之後才連線
		send("第二則");

		StepVerifier.create(alice.take(2)).expectNext("第一則", "第二則").expectComplete().verify(Duration.ofSeconds(5));
		StepVerifier.create(bob.take(2)).expectNext("第一則", "第二則").expectComplete().verify(Duration.ofSeconds(5));
		StepVerifier.create(late.take(1)).expectNext("第二則").expectComplete().verify(Duration.ofSeconds(5));
	}

	// 歷史訊息用 JSON 查詢（SSE 只負責即時推播）
	@Test
	void findAll_shouldReturnHistory_whenAcceptIsJson() {
		Message sent = send("留下紀錄");

		webTestClient.get().uri("/api/messages").accept(MediaType.APPLICATION_JSON).exchange()
				.expectStatus().isOk()
				.expectBodyList(Message.class).value(list -> assertThat(list).contains(sent));
	}

	// ---- 查不到資料：空的 Mono 是 200，加上 switchIfEmpty 才是 404（第 5 課） ----

	@Test
	void findById_shouldReturnMessage_whenExists() {
		Message sent = send("找得到");

		webTestClient.get().uri("/api/messages/{id}", sent.id()).exchange()
				.expectStatus().isOk()
				.expectBody(Message.class).isEqualTo(sent);
	}

	@Test
	void findById_shouldReturn404ProblemDetail_whenMissing() {
		webTestClient.get().uri("/api/messages/{id}", 999_999).exchange()
				.expectStatus().isNotFound()
				.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody().jsonPath("$.detail").isEqualTo("找不到訊息：999999");
	}

	@Test
	void findByIdUnchecked_shouldReturn200WithEmptyBody_whenMissing() {
		webTestClient.get().uri("/api/messages/{id}/unchecked", 999_999).exchange()
				.expectStatus().isOk() // 查不到，卻是 200
				.expectBody().isEmpty();
	}
}
