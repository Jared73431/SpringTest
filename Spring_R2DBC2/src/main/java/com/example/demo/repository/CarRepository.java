package com.example.demo.repository;

import java.math.BigDecimal;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Car;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CarRepository extends ReactiveCrudRepository<Car, Long> {

    Flux<Car> findByMake(String make);

    Flux<Car> findByModel(String model);

    Flux<Car> findByYear(Integer year);

    Flux<Car> findByMakeAndModel(String make, String model);

    Flux<Car> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    @Query("SELECT * FROM car WHERE make ILIKE :make")
    Flux<Car> findByMakeIgnoreCase(String make);

    @Query("SELECT * FROM car WHERE year >= :yearFrom AND year <= :yearTo")
    Flux<Car> findByYearRange(Integer yearFrom, Integer yearTo);

    @Query("SELECT * FROM car WHERE price <= :maxPrice ORDER BY price DESC")
    Flux<Car> findByPriceLessThanEqualOrderByPriceDesc(BigDecimal maxPrice);

    @Query("SELECT COUNT(*) FROM car WHERE make = :make")
    Mono<Long> countByMake(String make);
}
