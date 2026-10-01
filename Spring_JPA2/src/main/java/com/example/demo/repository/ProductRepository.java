package com.example.demo.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Product;

/**
 * 商品的 Repository，示範 derived query 的常見條件關鍵字（Between、LessThan、Containing）。
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    // 根據商品類別查找
    List<Product> findByCategory(String category);

    // 根據價格範圍查找
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // 查找庫存低於指定值的商品
    List<Product> findByStockLessThan(Integer stockThreshold);

    // 按名稱模糊查詢
    // Containing 會自動在參數前後加上 % 產生 LIKE '%keyword%'（區分大小寫，需忽略大小寫可加 IgnoreCase）
    List<Product> findByNameContaining(String keyword);

    // 自定義查詢：查找某個類別中價格最高的n個商品
    // JPQL 沒有 LIMIT 語法，筆數 n 由呼叫端傳入的 Pageable（例如 PageRequest.of(0, n)）決定
    @Query("SELECT p FROM Product p WHERE p.category = :category ORDER BY p.price DESC")
    List<Product> findTopPriceProductsByCategory(@Param("category") String category, Pageable pageable);
}
