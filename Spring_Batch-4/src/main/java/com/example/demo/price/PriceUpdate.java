package com.example.demo.price;

import java.math.BigDecimal;

/** 價格更新檔（CSV）的一行：把這個 ISBN 的價格改成 price */
public record PriceUpdate(String isbn, BigDecimal price) {
}
