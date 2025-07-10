package com.example.demo.controller;

import java.math.BigDecimal;

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
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CarDto;
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
    public Mono<ResponseEntity<Car>> getCarById(@PathVariable Long id) {
        return carService.getCarById(id)
                .map(car -> ResponseEntity.ok(car))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<Car>> createCar(@Valid @RequestBody CarDto carDto) {
        return carService.createCar(carDto)
                .map(car -> ResponseEntity.status(HttpStatus.CREATED).body(car));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Car>> updateCar(@PathVariable Long id,
                                               @Valid @RequestBody CarDto carDto) {
        return carService.updateCar(id, carDto)
                .map(car -> ResponseEntity.ok(car))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteCar(@PathVariable Long id) {
        return carService.getCarById(id)
                .flatMap(car -> carService.deleteCar(id)
                        .then(Mono.just(ResponseEntity.ok().<Void>build())))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public Flux<Car> searchCars(@RequestParam(required = false) String make,
                                @RequestParam(required = false) String model,
                                @RequestParam(required = false) Integer year,
                                @RequestParam(required = false) BigDecimal minPrice,
                                @RequestParam(required = false) BigDecimal maxPrice,
                                @RequestParam(required = false) Integer yearFrom,
                                @RequestParam(required = false) Integer yearTo) {

        if (make != null) {
            return carService.getCarsByMake(make);
        }
        if (model != null) {
            return carService.getCarsByModel(model);
        }
        if (year != null) {
            return carService.getCarsByYear(year);
        }
        if (minPrice != null && maxPrice != null) {
            return carService.getCarsByPriceRange(minPrice, maxPrice);
        }
        if (yearFrom != null && yearTo != null) {
            return carService.getCarsByYearRange(yearFrom, yearTo);
        }

        return carService.getAllCars();
    }

    @GetMapping("/count")
    public Mono<Long> countCarsByMake(@RequestParam @NotNull String make) {
        return carService.countCarsByMake(make);
    }
}
