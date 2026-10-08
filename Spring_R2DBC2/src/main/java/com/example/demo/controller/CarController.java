package com.example.demo.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import com.example.demo.dto.CarRequest;
import com.example.demo.dto.CarResponse;
import com.example.demo.dto.CarSearchCriteria;
import com.example.demo.service.CarService;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/cars")
public class CarController {

	private final CarService carService;

	public CarController(CarService carService) {
		this.carService = carService;
	}

	@GetMapping
	public Flux<CarResponse> getAllCars() {
		return carService.getAllCars();
	}

	@GetMapping("/{id}")
	public Mono<CarResponse> getCarById(@PathVariable Long id) {
		return carService.getCarById(id);
	}

	@PostMapping
	public Mono<ResponseEntity<CarResponse>> createCar(@Valid @RequestBody CarRequest request) {
		return carService.createCar(request)
				.map(car -> ResponseEntity.created(URI.create("/api/cars/" + car.id())).body(car));
	}

	@PutMapping("/{id}")
	public Mono<CarResponse> updateCar(@PathVariable Long id, @Valid @RequestBody CarRequest request) {
		return carService.updateCar(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public Mono<Void> deleteCar(@PathVariable Long id) {
		return carService.deleteCar(id);
	}

	// 查詢參數綁定到 record（沒有 @RequestParam 的物件參數會當成 model attribute）；沒有給任何條件時回傳全部
	@GetMapping("/search")
	public Flux<CarResponse> searchCars(CarSearchCriteria criteria) {
		return carService.search(criteria);
	}

	// @RequestParam 預設就是必填，缺少時回傳 400（修正前另外加了 @Validated + @NotNull，作用相同）
	@GetMapping("/count")
	public Mono<Long> countCarsByMake(@RequestParam String make) {
		return carService.countCarsByMake(make);
	}
}
