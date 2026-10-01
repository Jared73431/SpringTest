package com.example.demo.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    // 根據商品類別查找
    List<Product> findByCategory(String category);

    // 根據價格範圍查找
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // 查找庫存低於指定值的商品
    List<Product> findByStockLessThan(Integer stockThreshold);

    // 按名稱模糊查詢
    List<Product> findByNameContaining(String keyword);

    // 自定義查詢：查找某個類別中價格最高的n個商品
    @Query("SELECT p FROM Product p WHERE p.category = :category ORDER BY p.price DESC")
    List<Product> findTopPriceProductsByCategory(@Param("category") String category, Pageable pageable);
}
