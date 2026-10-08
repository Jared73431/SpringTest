package com.example.demo.service;

import static org.springframework.data.relational.core.query.Criteria.where;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;

import com.example.demo.dto.CarDto;
import com.example.demo.dto.CarSearchCriteria;
import com.example.demo.entity.Car;
import com.example.demo.exception.CarNotFoundException;
import com.example.demo.repository.CarRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CarService {

    private static final Logger log = LoggerFactory.getLogger(CarService.class);

    private final CarRepository carRepository;
    private final R2dbcEntityTemplate template;

    public CarService(CarRepository carRepository, R2dbcEntityTemplate template) {
        this.carRepository = carRepository;
        this.template = template;
    }

    public Flux<Car> getAllCars() {
        return carRepository.findAll();
    }

    public Mono<Car> getCarById(Long id) {
        return findCar(id);
    }

    public Mono<Car> createCar(CarDto carDto) {
        log.info("Creating new car: {} {}", carDto.getMake(), carDto.getModel());
        Car car = mapToEntity(carDto);
        car.setCreatedAt(LocalDateTime.now());
        car.setUpdatedAt(LocalDateTime.now());
        return carRepository.save(car);
    }

    public Mono<Car> updateCar(Long id, CarDto carDto) {
        return findCar(id)
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
        return findCar(id).flatMap(carRepository::delete);
    }

    /**
     * 修正前依序判斷 make → model → year → 價格區間 → 年份區間，只使用第一個有值的條件，其餘被默默忽略
     * （make=Toyota&amp;year=2021 只依 make 搜尋；只給 minPrice 時回傳全部）。
     * 現在用 Criteria 把有給的條件全部用 AND 組合。
     */
    public Flux<Car> search(CarSearchCriteria criteria) {
        return template.select(Car.class)
                .matching(Query.query(toCriteria(criteria)).sort(Sort.by("id")))
                .all();
    }

    // 修正前：搜尋用 ILIKE（不分大小寫），計數用 =（分大小寫），make=toyota 搜得到卻計數為 0。現在共用同一套條件
    public Mono<Long> countCarsByMake(String make) {
        return template.count(Query.query(toCriteria(new CarSearchCriteria(make, null, null, null, null, null, null))),
                Car.class);
    }

    private static Criteria toCriteria(CarSearchCriteria c) {
        Criteria criteria = Criteria.empty();
        if (c.make() != null) {
            criteria = criteria.and(where("make").is(c.make()).ignoreCase(true));
        }
        if (c.model() != null) {
            criteria = criteria.and(where("model").is(c.model()).ignoreCase(true));
        }
        if (c.year() != null) {
            criteria = criteria.and(where("year").is(c.year()));
        }
        if (c.minPrice() != null) {
            criteria = criteria.and(where("price").greaterThanOrEquals(c.minPrice()));
        }
        if (c.maxPrice() != null) {
            criteria = criteria.and(where("price").lessThanOrEquals(c.maxPrice()));
        }
        if (c.yearFrom() != null) {
            criteria = criteria.and(where("year").greaterThanOrEquals(c.yearFrom()));
        }
        if (c.yearTo() != null) {
            criteria = criteria.and(where("year").lessThanOrEquals(c.yearTo()));
        }
        return criteria;
    }

    // 查不到時是空的 Mono；轉成錯誤訊號，Controller 才會回傳 404 ProblemDetail
    private Mono<Car> findCar(Long id) {
        return carRepository.findById(id).switchIfEmpty(Mono.error(() -> new CarNotFoundException(id)));
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
