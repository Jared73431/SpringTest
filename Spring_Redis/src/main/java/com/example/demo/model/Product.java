package com.example.demo.model;

import java.math.BigDecimal;

/**
 * 存放在 Redis 的商品（key：product:{id}），以 JSON 存放。
 * 和 User 一樣是 JSON 物件，但改用 RedisJsonStore 存取，不需要專用的 RedisTemplate bean。
 * 金額使用 BigDecimal，避免 double 的精度誤差（0.1 + 0.2 ≠ 0.3）。
 */
public record Product(Long id, String name, BigDecimal price) {
}
