package com.example.demo.repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    // 查找某个客户的所有订单
    List<Order> findByCustomerId(String customerId);

    // 查找某个状态的订单
    List<Order> findByStatus(Order.OrderStatus status);

    // 查找某个日期范围的订单
    List<Order> findByOrderDateBetween(Date startDate, Date endDate);

    // 查找超过某个金额的订单
    List<Order> findByTotalAmountGreaterThan(BigDecimal amount);

    // 自定义查询：查找包含特定商品的订单
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.product.id = :productId")
    List<Order> findOrdersContainingProduct(@Param("productId") String productId);
}
