package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.demo.entity.Car;
import com.example.demo.dto.CarDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.repository.CarRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @InjectMocks
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

        when(carRepository.findAll()).thenReturn(Flux.just(car1, car2));

        // When & Then
        StepVerifier.create(carService.getAllCars())
                .expectNext(car1)
                .expectNext(car2)
                .verifyComplete();

        verify(carRepository, times(1)).findAll();
    }

    @Test
    void getCarById_WhenCarExists_ShouldReturnCar() {
        // Given
        when(carRepository.findById(1L)).thenReturn(Mono.just(testCar));

        // When & Then
        StepVerifier.create(carService.getCarById(1L))
                .expectNext(testCar)
                .verifyComplete();

        verify(carRepository, times(1)).findById(1L);
    }

    @Test
    void getCarById_WhenCarNotExists_ShouldReturnEmpty() {
        // Given
        when(carRepository.findById(1L)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(carService.getCarById(1L))
                .verifyComplete();

        verify(carRepository, times(1)).findById(1L);
    }

    @Test
    void createCar_ShouldCreateAndReturnCar() {
        // Given
        Car savedCar = new Car();
        savedCar.setId(1L);
        savedCar.setMake(testCarDto.getMake());
        savedCar.setModel(testCarDto.getModel());
        savedCar.setYear(testCarDto.getYear());
        savedCar.setColor(testCarDto.getColor());
        savedCar.setPrice(testCarDto.getPrice());
        savedCar.setCreatedAt(LocalDateTime.now());
        savedCar.setUpdatedAt(LocalDateTime.now());

        when(carRepository.save(any(Car.class))).thenReturn(Mono.just(savedCar));

        // When & Then
        StepVerifier.create(carService.createCar(testCarDto))
                .expectNextMatches(car ->
                        car.getMake().equals("Toyota") &&
                                car.getModel().equals("Camry") &&
                                car.getYear().equals(2022) &&
                                car.getColor().equals("White") &&
                                car.getPrice().compareTo(new BigDecimal("25000.00")) == 0
                )
                .verifyComplete();

        verify(carRepository, times(1)).save(any(Car.class));
    }

    @Test
    void updateCar_WhenCarExists_ShouldUpdateAndReturnCar() {
        // Given
        CarDto updateDto = new CarDto();
        updateDto.setMake("Honda");
        updateDto.setModel("Accord");
        updateDto.setYear(2023);
        updateDto.setColor("Blue");
        updateDto.setPrice(new BigDecimal("28000.00"));

        Car updatedCar = new Car();
        updatedCar.setId(1L);
        updatedCar.setMake("Honda");
        updatedCar.setModel("Accord");
        updatedCar.setYear(2023);
        updatedCar.setColor("Blue");
        updatedCar.setPrice(new BigDecimal("28000.00"));

        when(carRepository.findById(1L)).thenReturn(Mono.just(testCar));
        when(carRepository.save(any(Car.class))).thenReturn(Mono.just(updatedCar));

        // When & Then
        StepVerifier.create(carService.updateCar(1L, updateDto))
                .expectNextMatches(car ->
                        car.getMake().equals("Honda") &&
                                car.getModel().equals("Accord") &&
                                car.getYear().equals(2023) &&
                                car.getColor().equals("Blue") &&
                                car.getPrice().compareTo(new BigDecimal("28000.00")) == 0
                )
                .verifyComplete();

        verify(carRepository, times(1)).findById(1L);
        verify(carRepository, times(1)).save(any(Car.class));
    }

    @Test
    void updateCar_WhenCarNotExists_ShouldReturnEmpty() {
        // Given
        when(carRepository.findById(1L)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(carService.updateCar(1L, testCarDto))
                .verifyComplete();

        verify(carRepository, times(1)).findById(1L);
        verify(carRepository, never()).save(any(Car.class));
    }

    @Test
    void deleteCar_ShouldDeleteCar() {
        // Given
        when(carRepository.deleteById(1L)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(carService.deleteCar(1L))
                .verifyComplete();

        verify(carRepository, times(1)).deleteById(1L);
    }

    @Test
    void getCarsByMake_ShouldReturnCarsOfSpecificMake() {
        // Given
        Car car1 = new Car();
        car1.setMake("Toyota");
        car1.setModel("Camry");

        Car car2 = new Car();
        car2.setMake("Toyota");
        car2.setModel("Corolla");

        when(carRepository.findByMakeIgnoreCase("Toyota")).thenReturn(Flux.just(car1, car2));

        // When & Then
        StepVerifier.create(carService.getCarsByMake("Toyota"))
                .expectNext(car1)
                .expectNext(car2)
                .verifyComplete();

        verify(carRepository, times(1)).findByMakeIgnoreCase("Toyota");
    }

    @Test
    void getCarsByModel_ShouldReturnCarsOfSpecificModel() {
        // Given
        when(carRepository.findByModel("Camry")).thenReturn(Flux.just(testCar));

        // When & Then
        StepVerifier.create(carService.getCarsByModel("Camry"))
                .expectNext(testCar)
                .verifyComplete();

        verify(carRepository, times(1)).findByModel("Camry");
    }

    @Test
    void getCarsByYear_ShouldReturnCarsOfSpecificYear() {
        // Given
        when(carRepository.findByYear(2022)).thenReturn(Flux.just(testCar));

        // When & Then
        StepVerifier.create(carService.getCarsByYear(2022))
                .expectNext(testCar)
                .verifyComplete();

        verify(carRepository, times(1)).findByYear(2022);
    }

    @Test
    void getCarsByPriceRange_ShouldReturnCarsInPriceRange() {
        // Given
        BigDecimal minPrice = new BigDecimal("20000.00");
        BigDecimal maxPrice = new BigDecimal("30000.00");

        when(carRepository.findByPriceBetween(minPrice, maxPrice)).thenReturn(Flux.just(testCar));

        // When & Then
        StepVerifier.create(carService.getCarsByPriceRange(minPrice, maxPrice))
                .expectNext(testCar)
                .verifyComplete();

        verify(carRepository, times(1)).findByPriceBetween(minPrice, maxPrice);
    }

    @Test
    void getCarsByYearRange_ShouldReturnCarsInYearRange() {
        // Given
        when(carRepository.findByYearRange(2020, 2023)).thenReturn(Flux.just(testCar));

        // When & Then
        StepVerifier.create(carService.getCarsByYearRange(2020, 2023))
                .expectNext(testCar)
                .verifyComplete();

        verify(carRepository, times(1)).findByYearRange(2020, 2023);
    }

    @Test
    void countCarsByMake_ShouldReturnCount() {
        // Given
        when(carRepository.countByMake("Toyota")).thenReturn(Mono.just(5L));

        // When & Then
        StepVerifier.create(carService.countCarsByMake("Toyota"))
                .expectNext(5L)
                .verifyComplete();

        verify(carRepository, times(1)).countByMake("Toyota");
    }

    @Test
    void getCarsByMake_WhenNoResults_ShouldReturnEmpty() {
        // Given
        when(carRepository.findByMakeIgnoreCase("NonExistentMake")).thenReturn(Flux.empty());

        // When & Then
        StepVerifier.create(carService.getCarsByMake("NonExistentMake"))
                .verifyComplete();

        verify(carRepository, times(1)).findByMakeIgnoreCase("NonExistentMake");
    }

    @Test
    void createCar_ShouldSetTimestamps() {
        // Given
        Car carWithTimestamps = new Car();
        carWithTimestamps.setId(1L);
        carWithTimestamps.setMake("Toyota");
        carWithTimestamps.setModel("Camry");
        carWithTimestamps.setYear(2022);
        carWithTimestamps.setColor("White");
        carWithTimestamps.setPrice(new BigDecimal("25000.00"));
        carWithTimestamps.setCreatedAt(LocalDateTime.now());
        carWithTimestamps.setUpdatedAt(LocalDateTime.now());

        when(carRepository.save(any(Car.class))).thenReturn(Mono.just(carWithTimestamps));

        // When & Then
        StepVerifier.create(carService.createCar(testCarDto))
                .expectNextMatches(car ->
                        car.getCreatedAt() != null &&
                                car.getUpdatedAt() != null
                )
                .verifyComplete();
    }

    @Test
    void updateCar_ShouldUpdateTimestamp() {
        // Given
        LocalDateTime originalCreatedAt = LocalDateTime.now().minusDays(1);
        testCar.setCreatedAt(originalCreatedAt);

        Car updatedCar = new Car();
        updatedCar.setId(1L);
        updatedCar.setMake("Honda");
        updatedCar.setModel("Accord");
        updatedCar.setYear(2023);
        updatedCar.setColor("Blue");
        updatedCar.setPrice(new BigDecimal("28000.00"));
        updatedCar.setCreatedAt(originalCreatedAt);
        updatedCar.setUpdatedAt(LocalDateTime.now());

        when(carRepository.findById(1L)).thenReturn(Mono.just(testCar));
        when(carRepository.save(any(Car.class))).thenReturn(Mono.just(updatedCar));

        // When & Then
        StepVerifier.create(carService.updateCar(1L, testCarDto))
                .expectNextMatches(car ->
                        car.getUpdatedAt() != null &&
                                car.getCreatedAt().equals(originalCreatedAt)
                )
                .verifyComplete();
    }
}
