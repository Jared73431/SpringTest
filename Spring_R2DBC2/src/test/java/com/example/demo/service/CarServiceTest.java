package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;

import com.example.demo.dto.CarRequest;
import com.example.demo.entity.Car;
import com.example.demo.exception.CarNotFoundException;
import com.example.demo.repository.CarRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Service 的單元測試：Repository 用 mock 取代，驗證商業邏輯（查不到時的錯誤、請求如何套用到 Entity）。
 *
 * <p>
 * 搜尋的 SQL 與 Auditing 的時間戳記需要真的資料庫，由 CarApiTest 驗證。修正前的 createCar_ShouldSetTimestamps
 * 讓 mock 回傳已經有時間的物件，再斷言時間不是 null —— 測到的是 mock，不是 Service。
 */
@ExtendWith(MockitoExtension.class)
class CarServiceTest {

	private static final CarRequest REQUEST = new CarRequest("Honda", "Accord", 2023, "Blue",
			new BigDecimal("28000.00"));

	@Mock
	private CarRepository carRepository;

	@Mock
	private R2dbcEntityTemplate template;

	@InjectMocks
	private CarService carService;

	@Captor
	private ArgumentCaptor<Car> savedCar;

	private Car camry;

	@BeforeEach
	void setUp() {
		camry = new Car();
		camry.setId(1L);
		camry.setMake("Toyota");
		camry.setModel("Camry");
		camry.setYear(2022);
		camry.setColor("White");
		camry.setPrice(new BigDecimal("25000.00"));
	}

	@Test
	void getAllCars_shouldMapEntitiesToResponses() {
		when(carRepository.findAll()).thenReturn(Flux.just(camry));

		StepVerifier.create(carService.getAllCars())
				.assertNext(car -> assertThat(car.make()).isEqualTo("Toyota"))
				.verifyComplete();
	}

	@Test
	void getCarById_shouldReturnCar_whenExists() {
		when(carRepository.findById(1L)).thenReturn(Mono.just(camry));

		StepVerifier.create(carService.getCarById(1L))
				.assertNext(car -> assertThat(car.id()).isEqualTo(1L))
				.verifyComplete();
	}

	@Test
	void getCarById_shouldFailWithNotFound_whenMissing() {
		when(carRepository.findById(1L)).thenReturn(Mono.empty());

		StepVerifier.create(carService.getCarById(1L)).expectError(CarNotFoundException.class).verify();
	}

	@Test
	void createCar_shouldSaveEntityBuiltFromRequest() {
		when(carRepository.save(any(Car.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

		StepVerifier.create(carService.createCar(REQUEST)).expectNextCount(1).verifyComplete();

		verify(carRepository).save(savedCar.capture());
		assertThat(savedCar.getValue().getId()).isNull(); // id 是 null → INSERT（由資料庫產生）
		assertThat(savedCar.getValue().getMake()).isEqualTo("Honda");
		assertThat(savedCar.getValue().getPrice()).isEqualByComparingTo("28000.00");
	}

	@Test
	void updateCar_shouldApplyRequestToLoadedEntityAndSave_whenExists() {
		when(carRepository.findById(1L)).thenReturn(Mono.just(camry));
		when(carRepository.save(any(Car.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

		StepVerifier.create(carService.updateCar(1L, REQUEST))
				.assertNext(car -> assertThat(car.model()).isEqualTo("Accord"))
				.verifyComplete();

		verify(carRepository).save(savedCar.capture());
		assertThat(savedCar.getValue()).isSameAs(camry); // 修改查出來的 Entity（保留 id），再 save → UPDATE
		assertThat(camry.getMake()).isEqualTo("Honda");
	}

	@Test
	void updateCar_shouldFailWithNotFound_whenMissing() {
		when(carRepository.findById(1L)).thenReturn(Mono.empty());

		StepVerifier.create(carService.updateCar(1L, REQUEST)).expectError(CarNotFoundException.class).verify();

		verify(carRepository, never()).save(any(Car.class));
	}

	@Test
	void deleteCar_shouldDeleteLoadedEntity_whenExists() {
		when(carRepository.findById(1L)).thenReturn(Mono.just(camry));
		when(carRepository.delete(camry)).thenReturn(Mono.empty());

		StepVerifier.create(carService.deleteCar(1L)).verifyComplete();

		verify(carRepository).delete(camry);
	}

	@Test
	void deleteCar_shouldFailWithNotFound_whenMissing() {
		when(carRepository.findById(1L)).thenReturn(Mono.empty());

		StepVerifier.create(carService.deleteCar(1L)).expectError(CarNotFoundException.class).verify();

		verify(carRepository, never()).delete(any(Car.class));
	}
}
