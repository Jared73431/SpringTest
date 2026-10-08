package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.example.demo.dto.CarRequest;
import com.example.demo.dto.CarResponse;
import com.example.demo.dto.CarSearchCriteria;
import com.example.demo.exception.CarNotFoundException;
import com.example.demo.service.CarService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Controller 的單元測試：只啟動 WebFlux 層，CarService 用 mock 取代。驗證路由、參數綁定、驗證與錯誤回應。
 *
 * <p>
 * 修正前用 @TestConfiguration 自己註冊 mock(CarService.class)；現在用 @MockitoBean（Spring Framework 6.2 起取代 @MockBean）。
 */
@WebFluxTest(CarController.class)
class CarControllerTest {

	private static final CarRequest VALID_REQUEST = new CarRequest("Toyota", "Camry", 2022, "White",
			new BigDecimal("25000.00"));

	private static final CarResponse CAMRY = new CarResponse(1L, "Toyota", "Camry", 2022, "White",
			new BigDecimal("25000.00"), LocalDateTime.of(2026, 1, 1, 9, 0), LocalDateTime.of(2026, 1, 1, 9, 0));

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private CarService carService;

	@Test
	void getAllCars_shouldReturnCars() {
		when(carService.getAllCars()).thenReturn(Flux.just(CAMRY));

		webTestClient.get().uri("/api/cars").exchange()
				.expectStatus().isOk()
				.expectBodyList(CarResponse.class).isEqualTo(List.of(CAMRY));
	}

	@Test
	void getCarById_shouldReturnCar_whenExists() {
		when(carService.getCarById(1L)).thenReturn(Mono.just(CAMRY));

		webTestClient.get().uri("/api/cars/1").exchange()
				.expectStatus().isOk()
				.expectBody(CarResponse.class).isEqualTo(CAMRY);
	}

	@Test
	void getCarById_shouldReturn404ProblemDetail_whenMissing() {
		when(carService.getCarById(1L)).thenReturn(Mono.error(new CarNotFoundException(1L)));

		webTestClient.get().uri("/api/cars/1").exchange()
				.expectStatus().isNotFound()
				.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody().jsonPath("$.detail").isEqualTo("找不到汽車：1");
	}

	@Test
	void createCar_shouldReturn201WithLocation_whenValid() {
		when(carService.createCar(VALID_REQUEST)).thenReturn(Mono.just(CAMRY));

		webTestClient.post().uri("/api/cars").bodyValue(VALID_REQUEST).exchange()
				.expectStatus().isCreated()
				.expectHeader().location("/api/cars/1")
				.expectBody(CarResponse.class).isEqualTo(CAMRY);
	}

	@Test
	void createCar_shouldReturn400WithFieldErrors_whenInvalid() {
		var invalid = new CarRequest("", "Camry", 1800, "White", new BigDecimal("-1000"));

		webTestClient.post().uri("/api/cars").bodyValue(invalid).exchange()
				.expectStatus().isBadRequest()
				.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.errors.make").isEqualTo("Make is required")
				.jsonPath("$.errors.year").isEqualTo("Year must be greater than 1900")
				.jsonPath("$.errors.price").isEqualTo("Price must be greater than 0");
	}

	@Test
	void createCar_shouldReturn400_whenMakeIsMissing() {
		var invalid = new CarRequest(null, "Camry", 2022, "White", new BigDecimal("25000"));

		webTestClient.post().uri("/api/cars").bodyValue(invalid).exchange()
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors.make").isEqualTo("Make is required");
	}

	@Test
	void createCar_shouldReturn400_whenPriceIsZero() {
		var invalid = new CarRequest("Toyota", "Camry", 2022, "White", BigDecimal.ZERO);

		webTestClient.post().uri("/api/cars").bodyValue(invalid).exchange()
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors.price").isEqualTo("Price must be greater than 0");
	}

	// 修正前：catch-all 回傳 500，並把 ex.getMessage()（含資料庫的 constraint 名稱）回傳給客戶端
	@Test
	void createCar_shouldReturn409WithoutDatabaseMessage_whenDatabaseRejectsIt() {
		when(carService.createCar(any(CarRequest.class)))
				.thenReturn(Mono.error(new DataIntegrityViolationException("violates check constraint \"car_year_check\"")));

		webTestClient.post().uri("/api/cars").bodyValue(VALID_REQUEST).exchange()
				.expectStatus().isEqualTo(409)
				.expectBody()
				.jsonPath("$.detail").isEqualTo("資料違反資料庫的限制條件")
				.consumeWith(result -> assertThat(new String(result.getResponseBodyContent()))
						.doesNotContain("car_year_check"));
	}

	@Test
	void updateCar_shouldReturnUpdatedCar_whenExists() {
		when(carService.updateCar(eq(1L), any(CarRequest.class))).thenReturn(Mono.just(CAMRY));

		webTestClient.put().uri("/api/cars/1").bodyValue(VALID_REQUEST).exchange()
				.expectStatus().isOk()
				.expectBody(CarResponse.class).isEqualTo(CAMRY);
	}

	@Test
	void updateCar_shouldReturn404_whenMissing() {
		when(carService.updateCar(eq(999L), any(CarRequest.class)))
				.thenReturn(Mono.error(new CarNotFoundException(999L)));

		webTestClient.put().uri("/api/cars/999").bodyValue(VALID_REQUEST).exchange()
				.expectStatus().isNotFound();
	}

	@Test
	void deleteCar_shouldReturn204_whenExists() {
		when(carService.deleteCar(1L)).thenReturn(Mono.empty());

		webTestClient.delete().uri("/api/cars/1").exchange().expectStatus().isNoContent();
	}

	@Test
	void deleteCar_shouldReturn404_whenMissing() {
		when(carService.deleteCar(1L)).thenReturn(Mono.error(new CarNotFoundException(1L)));

		webTestClient.delete().uri("/api/cars/1").exchange().expectStatus().isNotFound();
	}

	// 搜尋的 SQL 由 CarApiTest 用真的資料庫驗證；這裡只驗證查詢參數有沒有正確綁定到 CarSearchCriteria
	@Test
	void searchCars_shouldBindAllQueryParametersToCriteria() {
		when(carService.search(any(CarSearchCriteria.class))).thenReturn(Flux.just(CAMRY));

		webTestClient.get()
				.uri("/api/cars/search?make=Toyota&model=Camry&year=2022&minPrice=1000&maxPrice=30000&yearFrom=2020&yearTo=2023")
				.exchange()
				.expectStatus().isOk()
				.expectBodyList(CarResponse.class).hasSize(1);

		verify(carService).search(new CarSearchCriteria("Toyota", "Camry", 2022, new BigDecimal("1000"),
				new BigDecimal("30000"), 2020, 2023));
	}

	@Test
	void searchCars_shouldPassEmptyCriteria_whenNoParameterIsGiven() {
		when(carService.search(any(CarSearchCriteria.class))).thenReturn(Flux.empty());

		webTestClient.get().uri("/api/cars/search").exchange().expectStatus().isOk();

		verify(carService).search(new CarSearchCriteria(null, null, null, null, null, null, null));
	}

	@Test
	void countCarsByMake_shouldReturnCount() {
		when(carService.countCarsByMake("Toyota")).thenReturn(Mono.just(2L));

		webTestClient.get().uri("/api/cars/count?make=Toyota").exchange()
				.expectStatus().isOk()
				.expectBody(Long.class).isEqualTo(2L);
	}

	@Test
	void countCarsByMake_shouldReturn400_whenMakeIsMissing() {
		webTestClient.get().uri("/api/cars/count").exchange().expectStatus().isBadRequest();
	}
}
