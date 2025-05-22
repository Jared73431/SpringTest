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

    // 根据商品类别查找
    List<Product> findByCategory(String category);

    // 根据价格范围查找
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // 查找库存低于指定值的商品
    List<Product> findByStockLessThan(Integer stockThreshold);

    // 按名称模糊查询
    List<Product> findByNameContaining(String keyword);

    // 自定义查询：查找某个类别中价格最高的n个商品
    @Query("SELECT p FROM Product p WHERE p.category = :category ORDER BY p.price DESC")
    List<Product> findTopPriceProductsByCategory(@Param("category") String category, Pageable pageable);
}
