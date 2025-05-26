package com.example.demo.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.ProductDTO;
import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepository;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    /**
     * 獲取所有商品
     * @return 所有商品的DTO列表
     */
    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * 根據ID獲取商品
     * @param id 商品ID
     * @return 商品DTO的Optional包裝
     */
    public Optional<ProductDTO> getProductById(String id) {
        return productRepository.findById(id)
                .map(ProductDTO::new);
    }

    /**
     * 創建新商品
     * @param productDTO 商品DTO
     * @return 創建後的商品DTO
     * @throws IllegalArgumentException 如果商品ID已存在
     */
    public ProductDTO createProduct(ProductDTO productDTO) {
        // 檢查ID是否已存在
        if (productDTO.getId() != null && productRepository.existsById(productDTO.getId())) {
            throw new IllegalArgumentException("商品ID已存在: " + productDTO.getId());
        }

        // 如果沒有提供ID，生成一個
        if (productDTO.getId() == null) {
            productDTO.setId(UUID.randomUUID().toString());
        }

        Product product = productRepository.save(productDTO.toEntity());
        return new ProductDTO(product);
    }

    /**
     * 更新商品
     * @param id 商品ID
     * @param productDTO 商品DTO
     * @return 更新後的商品DTO
     * @throws IllegalArgumentException 如果商品不存在
     */
    public ProductDTO updateProduct(String id, ProductDTO productDTO) {
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("商品不存在: " + id);
        }

        productDTO.setId(id);
        Product product = productRepository.save(productDTO.toEntity());
        return new ProductDTO(product);
    }

    /**
     * 刪除商品
     * @param id 商品ID
     * @throws IllegalArgumentException 如果商品不存在
     */
    public void deleteProduct(String id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("商品不存在: " + id);
        }

        productRepository.deleteById(id);
    }

    /**
     * 根據類別查找商品
     * @param category 商品類別
     * @return 該類別的商品DTO列表
     */
    public List<ProductDTO> getProductsByCategory(String category) {
        return productRepository.findByCategory(category).stream()
                .map(ProductDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * 查找價格區間內的商品
     * @param minPrice 最低價格
     * @param maxPrice 最高價格
     * @return 符合價格區間的商品DTO列表
     */
    public List<ProductDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return productRepository.findByPriceBetween(minPrice, maxPrice).stream()
                .map(ProductDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * 查找庫存低於閾值的商品
     * @param threshold 庫存閾值
     * @return 低庫存商品DTO列表
     */
    public List<ProductDTO> getLowStockProducts(Integer threshold) {
        return productRepository.findByStockLessThan(threshold).stream()
                .map(ProductDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * 檢查商品是否存在
     * @param id 商品ID
     * @return 是否存在
     */
    public boolean existsById(String id) {
        return productRepository.existsById(id);
    }
}
