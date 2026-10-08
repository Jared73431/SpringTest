package com.example.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CarDto;
import com.example.demo.dto.CarSearchCriteria;
import com.example.demo.entity.Car;
import com.example.demo.service.CarService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CarController {

    private final CarService carService;

    @GetMapping
    public Flux<Car> getAllCars() {
        return carService.getAllCars();
    }

    @GetMapping("/{id}")
    public Mono<Car> getCarById(@PathVariable Long id) {
        return carService.getCarById(id);
    }

    @PostMapping
    public Mono<ResponseEntity<Car>> createCar(@Valid @RequestBody CarDto carDto) {
        return carService.createCar(carDto)
                .map(car -> ResponseEntity.status(HttpStatus.CREATED).body(car));
    }

    @PutMapping("/{id}")
    public Mono<Car> updateCar(@PathVariable Long id, @Valid @RequestBody CarDto carDto) {
        return carService.updateCar(id, carDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteCar(@PathVariable Long id) {
        return carService.deleteCar(id);
    }

    // 查詢參數綁定到 record（沒有 @RequestParam 的物件參數會當成 model attribute）；沒有給任何條件時回傳全部
    @GetMapping("/search")
    public Flux<Car> searchCars(CarSearchCriteria criteria) {
        return carService.search(criteria);
    }

    @GetMapping("/count")
    public Mono<Long> countCarsByMake(@RequestParam @NotNull String make) {
        return carService.countCarsByMake(make);
    }
}
