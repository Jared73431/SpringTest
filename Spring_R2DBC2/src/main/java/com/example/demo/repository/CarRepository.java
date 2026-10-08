package com.example.demo.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.example.demo.entity.Car;

/**
 * 基本的 CRUD 用 Repository；搜尋的條件會依參數變化，改用 R2dbcEntityTemplate + Criteria（見 CarService）。
 * 修正前為每一種搜尋各寫一個方法（findByMake、findByYearRange…，其中 3 個沒有被使用），也只能一次用一個條件。
 */
public interface CarRepository extends ReactiveCrudRepository<Car, Long> {
}
