package com.example.demo.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ProductDTO;
import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepository;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    // 获取所有商品
    @GetMapping
    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductDTO::new)
                .collect(Collectors.toList());
    }

    // 根据ID获取商品
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable String id) {
        return productRepository.findById(id)
                .map(product -> ResponseEntity.ok(new ProductDTO(product)))
                .orElse(ResponseEntity.notFound().build());
    }

    // 创建商品
    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductDTO productDTO) {
        // 检查ID是否已存在
        if (productDTO.getId() != null && productRepository.existsById(productDTO.getId())) {
            return ResponseEntity.badRequest().build();
        }

        // 如果没有提供ID，生成一个
        if (productDTO.getId() == null) {
            productDTO.setId(UUID.randomUUID().toString());
        }

        Product product = productRepository.save(productDTO.toEntity());
        return ResponseEntity.created(URI.create("/api/products/" + product.getId()))
                .body(new ProductDTO(product));
    }

    // 更新商品
    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable String id, @RequestBody ProductDTO productDTO) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        productDTO.setId(id);
        Product product = productRepository.save(productDTO.toEntity());
        return ResponseEntity.ok(new ProductDTO(product));
    }

    // 删除商品
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // 根据类别查找商品
    @GetMapping("/category/{category}")
    public List<ProductDTO> getProductsByCategory(@PathVariable String category) {
        return productRepository.findByCategory(category).stream()
                .map(ProductDTO::new)
                .collect(Collectors.toList());
    }

    // 查找价格区间内的商品
    @GetMapping("/price-range")
    public List<ProductDTO> getProductsByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice) {
        return productRepository.findByPriceBetween(minPrice, maxPrice).stream()
                .map(ProductDTO::new)
                .collect(Collectors.toList());
    }

    // 查找库存低于阈值的商品
    @GetMapping("/low-stock")
    public List<ProductDTO> getLowStockProducts(@RequestParam Integer threshold) {
        return productRepository.findByStockLessThan(threshold).stream()
                .map(ProductDTO::new)
                .collect(Collectors.toList());
    }
}
