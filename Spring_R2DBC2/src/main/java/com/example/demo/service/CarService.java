package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.demo.dto.CarDto;
import com.example.demo.entity.Car;
import com.example.demo.repository.CarRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarService {

    private final CarRepository carRepository;

    public Flux<Car> getAllCars() {
        log.info("Fetching all cars");
        return carRepository.findAll();
    }

    public Mono<Car> getCarById(Long id) {
        log.info("Fetching car with id: {}", id);
        return carRepository.findById(id);
    }

    public Mono<Car> createCar(CarDto carDto) {
        log.info("Creating new car: {}", carDto);
        Car car = mapToEntity(carDto);
        car.setCreatedAt(LocalDateTime.now());
        car.setUpdatedAt(LocalDateTime.now());
        return carRepository.save(car);
    }

    public Mono<Car> updateCar(Long id, CarDto carDto) {
        log.info("Updating car with id: {}", id);
        return carRepository.findById(id)
                .flatMap(existingCar -> {
                    existingCar.setMake(carDto.getMake());
                    existingCar.setModel(carDto.getModel());
                    existingCar.setYear(carDto.getYear());
                    existingCar.setColor(carDto.getColor());
                    existingCar.setPrice(carDto.getPrice());
                    existingCar.setUpdatedAt(LocalDateTime.now());
                    return carRepository.save(existingCar);
                });
    }

    public Mono<Void> deleteCar(Long id) {
        log.info("Deleting car with id: {}", id);
        return carRepository.deleteById(id);
    }

    public Flux<Car> getCarsByMake(String make) {
        log.info("Fetching cars by make: {}", make);
        return carRepository.findByMakeIgnoreCase(make);
    }

    public Flux<Car> getCarsByModel(String model) {
        log.info("Fetching cars by model: {}", model);
        return carRepository.findByModel(model);
    }

    public Flux<Car> getCarsByYear(Integer year) {
        log.info("Fetching cars by year: {}", year);
        return carRepository.findByYear(year);
    }

    public Flux<Car> getCarsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.info("Fetching cars by price range: {} - {}", minPrice, maxPrice);
        return carRepository.findByPriceBetween(minPrice, maxPrice);
    }

    public Flux<Car> getCarsByYearRange(Integer yearFrom, Integer yearTo) {
        log.info("Fetching cars by year range: {} - {}", yearFrom, yearTo);
        return carRepository.findByYearRange(yearFrom, yearTo);
    }

    public Mono<Long> countCarsByMake(String make) {
        log.info("Counting cars by make: {}", make);
        return carRepository.countByMake(make);
    }

    private Car mapToEntity(CarDto carDto) {
        Car car = new Car();
        car.setMake(carDto.getMake());
        car.setModel(carDto.getModel());
        car.setYear(carDto.getYear());
        car.setColor(carDto.getColor());
        car.setPrice(carDto.getPrice());
        return car;
    }
}
