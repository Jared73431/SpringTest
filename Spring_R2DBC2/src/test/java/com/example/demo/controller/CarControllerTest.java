package com.example.demo.controller;

import com.example.demo.dto.CarDto;
import com.example.demo.dto.CarSearchCriteria;
import com.example.demo.entity.Car;
import com.example.demo.exception.CarNotFoundException;
import com.example.demo.service.CarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(CarController.class)
public class CarControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public CarService carService() {
            return mock(CarService.class);
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CarService carService;

    private Car testCar;
    private CarDto testCarDto;

    @BeforeEach
    void setUp() {
        testCar = new Car();
        testCar.setId(1L);
        testCar.setMake("Toyota");
        testCar.setModel("Camry");
        testCar.setYear(2022);
        testCar.setColor("White");
        testCar.setPrice(new BigDecimal("25000.00"));
        testCar.setCreatedAt(LocalDateTime.now());
        testCar.setUpdatedAt(LocalDateTime.now());

        testCarDto = new CarDto();
        testCarDto.setMake("Toyota");
        testCarDto.setModel("Camry");
        testCarDto.setYear(2022);
        testCarDto.setColor("White");
        testCarDto.setPrice(new BigDecimal("25000.00"));
    }

    @Test
    void getAllCars_ShouldReturnAllCars() {
        // Given
        Car car1 = new Car();
        car1.setId(1L);
        car1.setMake("Toyota");
        car1.setModel("Camry");

        Car car2 = new Car();
        car2.setId(2L);
        car2.setMake("Honda");
        car2.setModel("Civic");

        when(carService.getAllCars()).thenReturn(Flux.just(car1, car2));

        // When & Then
        webTestClient.get()
                .uri("/api/cars")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(Car.class)
                .hasSize(2)
                .contains(car1, car2);
    }

    @Test
    void getCarById_WhenCarExists_ShouldReturnCar() {
        // Given
        when(carService.getCarById(1L)).thenReturn(Mono.just(testCar));

        // When & Then
        webTestClient.get()
                .uri("/api/cars/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(Car.class)
                .isEqualTo(testCar);
    }

    @Test
    void getCarById_WhenCarNotExists_ShouldReturnNotFound() {
        // Given
        when(carService.getCarById(1L)).thenReturn(Mono.error(new CarNotFoundException(1L)));

        // When & Then
        webTestClient.get()
                .uri("/api/cars/1")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void createCar_WithValidData_ShouldCreateCar() {
        // Given
        when(carService.createCar(any(CarDto.class))).thenReturn(Mono.just(testCar));

        // When & Then
        webTestClient.post()
                .uri("/api/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testCarDto)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(Car.class)
                .isEqualTo(testCar);
    }

    @Test
    void createCar_WithInvalidData_ShouldReturnBadRequest() {
        // Given
        CarDto invalidDto = new CarDto();
        invalidDto.setMake(""); // Invalid - empty make
        invalidDto.setModel("Camry");
        invalidDto.setYear(1800); // Invalid - year too old
        invalidDto.setPrice(new BigDecimal("-1000")); // Invalid - negative price

        // When & Then
        webTestClient.post()
                .uri("/api/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void updateCar_WhenCarExists_ShouldUpdateCar() {
        // Given
        Car updatedCar = new Car();
        updatedCar.setId(1L);
        updatedCar.setMake("Honda");
        updatedCar.setModel("Accord");
        updatedCar.setYear(2023);
        updatedCar.setColor("Blue");
        updatedCar.setPrice(new BigDecimal("28000.00"));

        when(carService.updateCar(anyLong(), any(CarDto.class))).thenReturn(Mono.just(updatedCar));

        // When & Then
        webTestClient.put()
                .uri("/api/cars/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testCarDto)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(Car.class)
                .isEqualTo(updatedCar);
    }

    @Test
    void updateCar_WhenCarNotExists_ShouldReturnNotFound() {
        // Given
        when(carService.updateCar(anyLong(), any(CarDto.class))).thenReturn(Mono.error(new CarNotFoundException(999L)));

        // When & Then
        webTestClient.put()
                .uri("/api/cars/999")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testCarDto)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteCar_WhenCarExists_ShouldReturnNoContent() {
        // Given
        when(carService.deleteCar(1L)).thenReturn(Mono.empty());

        // When & Then
        webTestClient.delete()
                .uri("/api/cars/1")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void deleteCar_WhenCarNotExists_ShouldReturnNotFound() {
        // Given
        when(carService.deleteCar(1L)).thenReturn(Mono.error(new CarNotFoundException(1L)));

        // When & Then
        webTestClient.delete()
                .uri("/api/cars/1")
                .exchange()
                .expectStatus().isNotFound();
    }

    // 違反資料庫限制（CHECK、UNIQUE…）：409，回應不帶資料庫的錯誤訊息（修正前是 500，並回傳 ex.getMessage()）
    @Test
    void createCar_WhenDatabaseRejectsIt_ShouldReturnConflictWithoutDatabaseMessage() {
        when(carService.createCar(any(CarDto.class)))
                .thenReturn(Mono.error(new DataIntegrityViolationException("violates check constraint \"car_year_check\"")));

        webTestClient.post()
                .uri("/api/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testCarDto)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.detail").isEqualTo("資料違反資料庫的限制條件")
                .consumeWith(result -> assertThat(new String(result.getResponseBodyContent()))
                        .doesNotContain("car_year_check"));
    }

    // 搜尋的 SQL 由 CarApiTest 用真的資料庫驗證；這裡只驗證查詢參數有沒有正確綁定到 CarSearchCriteria
    @Test
    void searchCars_ShouldBindAllQueryParametersToCriteria() {
        when(carService.search(any(CarSearchCriteria.class))).thenReturn(Flux.just(testCar));

        webTestClient.get()
                .uri("/api/cars/search?make=Toyota&model=Camry&year=2022&minPrice=1000&maxPrice=30000&yearFrom=2020&yearTo=2023")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Car.class).hasSize(1);

        verify(carService).search(new CarSearchCriteria("Toyota", "Camry", 2022, new BigDecimal("1000"),
                new BigDecimal("30000"), 2020, 2023));
    }

    @Test
    void searchCars_WithoutParams_ShouldPassEmptyCriteria() {
        when(carService.search(any(CarSearchCriteria.class))).thenReturn(Flux.empty());

        webTestClient.get().uri("/api/cars/search").exchange().expectStatus().isOk();

        verify(carService).search(new CarSearchCriteria(null, null, null, null, null, null, null));
    }

    @Test
    void countCarsByMake_ShouldReturnCount() {
        when(carService.countCarsByMake("Toyota")).thenReturn(Mono.just(2L));

        webTestClient.get().uri("/api/cars/count?make=Toyota").exchange()
                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(2L);
    }

    @Test
    void createCar_WithNullMake_ShouldReturnBadRequest() {
        // Given
        CarDto invalidDto = new CarDto();
        invalidDto.setMake(null); // Invalid - null make
        invalidDto.setModel("Camry");
        invalidDto.setYear(2022);
        invalidDto.setPrice(new BigDecimal("25000"));

        // When & Then
        webTestClient.post()
                .uri("/api/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createCar_WithInvalidYear_ShouldReturnBadRequest() {
        // Given
        CarDto invalidDto = new CarDto();
        invalidDto.setMake("Toyota");
        invalidDto.setModel("Camry");
        invalidDto.setYear(2150); // Invalid - year too far in future
        invalidDto.setPrice(new BigDecimal("25000"));

        // When & Then
        webTestClient.post()
                .uri("/api/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createCar_WithZeroPrice_ShouldReturnBadRequest() {
        // Given
        CarDto invalidDto = new CarDto();
        invalidDto.setMake("Toyota");
        invalidDto.setModel("Camry");
        invalidDto.setYear(2022);
        invalidDto.setPrice(BigDecimal.ZERO); // Invalid - zero price

        // When & Then
        webTestClient.post()
                .uri("/api/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidDto)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
