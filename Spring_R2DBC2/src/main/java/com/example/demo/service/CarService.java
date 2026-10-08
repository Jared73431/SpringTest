package com.example.demo.service;

import static org.springframework.data.relational.core.query.Criteria.where;

import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;

import com.example.demo.dto.CarRequest;
import com.example.demo.dto.CarResponse;
import com.example.demo.dto.CarSearchCriteria;
import com.example.demo.entity.Car;
import com.example.demo.exception.CarNotFoundException;
import com.example.demo.repository.CarRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CarService {

	private final CarRepository carRepository;
	private final R2dbcEntityTemplate template;

	public CarService(CarRepository carRepository, R2dbcEntityTemplate template) {
		this.carRepository = carRepository;
		this.template = template;
	}

	public Flux<CarResponse> getAllCars() {
		return carRepository.findAll().map(CarResponse::from);
	}

	public Mono<CarResponse> getCarById(Long id) {
		return findCar(id).map(CarResponse::from);
	}

	// createdAt / updatedAt 由 R2DBC Auditing 填入（修正前由這裡手動設定 LocalDateTime.now()）
	public Mono<CarResponse> createCar(CarRequest request) {
		Car car = new Car();
		apply(request, car);
		return carRepository.save(car).map(CarResponse::from);
	}

	// R2DBC 沒有 dirty checking：修改 Entity 之後一定要 save()（JPA 在交易結束時會自動更新）
	public Mono<CarResponse> updateCar(Long id, CarRequest request) {
		return findCar(id)
				.flatMap(car -> {
					apply(request, car);
					return carRepository.save(car);
				})
				.map(CarResponse::from);
	}

	public Mono<Void> deleteCar(Long id) {
		return findCar(id).flatMap(carRepository::delete);
	}

	/**
	 * 修正前依序判斷 make → model → year → 價格區間 → 年份區間，只使用第一個有值的條件，其餘被默默忽略
	 * （make=Toyota&amp;year=2021 只依 make 搜尋；只給 minPrice 時回傳全部）。
	 * 現在用 Criteria 把有給的條件全部用 AND 組合。
	 */
	public Flux<CarResponse> search(CarSearchCriteria criteria) {
		return template.select(Car.class)
				.matching(Query.query(toCriteria(criteria)).sort(Sort.by("id")))
				.all()
				.map(CarResponse::from);
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

	// 價格統一成 2 位小數（和 DECIMAL(10, 2) 相同）：否則新增時回應的是請求的寫法（30000），之後查詢是資料庫的 30000.00。
	// @Digits 已限制最多 2 位小數，setScale(2) 不會改變數值
	private static void apply(CarRequest request, Car car) {
		car.setMake(request.make());
		car.setModel(request.model());
		car.setYear(request.year());
		car.setColor(request.color());
		car.setPrice(request.price() == null ? null : request.price().setScale(2));
	}
}
