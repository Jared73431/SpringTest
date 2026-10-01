package com.example.demo.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.service.ProductService;

import jakarta.validation.Valid;

/**
 * 商品 REST API：輸入輸出都使用 ProductDTO，不直接暴露 Entity。
 * 不在這裡 try/catch，Service 拋出的例外由 GlobalExceptionHandler 統一轉成 ProblemDetail。
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * GET /api/products：取得所有商品。200。
     * @return 所有商品列表
     */
    @GetMapping
    public List<ProductDTO> getAllProducts() {
        return productService.getAllProducts();
    }

    /**
     * GET /api/products/{id}：根據ID取得商品。200；商品不存在 → 404。
     * Service 回傳 Optional，由這裡決定「查無資料」要轉成 ResourceNotFoundException。
     * @param id 商品ID
     * @return 商品資訊
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable String id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("商品", id));
    }

    /**
     * POST /api/products：建立新商品（未提供 ID 時自動產生）。
     * 201 + Location；驗證失敗 → 400；商品ID已存在 → 409。
     * @param productDTO 商品資訊
     * @return 建立的商品資訊
     */
    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody ProductDTO productDTO) {
        ProductDTO createdProduct = productService.createProduct(productDTO);
        return ResponseEntity.created(URI.create("/api/products/" + createdProduct.getId()))
                .body(createdProduct);
    }

    /**
     * PUT /api/products/{id}：更新商品（以 URL 的 id 為準，Body 中的 id 會被忽略）。
     * 200；驗證失敗 → 400；商品不存在 → 404；同時被其他交易修改（樂觀鎖衝突）→ 409。
     * @param id 商品ID
     * @param productDTO 更新的商品資訊
     * @return 更新後的商品資訊
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable String id, @Valid @RequestBody ProductDTO productDTO) {
        ProductDTO updatedProduct = productService.updateProduct(id, productDTO);
        return ResponseEntity.ok(updatedProduct);
    }

    /**
     * DELETE /api/products/{id}：刪除商品。204；商品不存在 → 404。
     * @param id 商品ID
     * @return 無內容
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/products/category/{category}：根據類別查找商品。200；查無資料時回傳空陣列。
     * @param category 商品類別
     * @return 該類別的商品列表
     */
    @GetMapping("/category/{category}")
    public List<ProductDTO> getProductsByCategory(@PathVariable String category) {
        return productService.getProductsByCategory(category);
    }

    /**
     * GET /api/products/price-range（Query 參數 minPrice、maxPrice）：查找價格區間內的商品。
     * 200；參數缺少或格式錯誤 → 400。
     * @param minPrice 最低價格
     * @param maxPrice 最高價格
     * @return 符合價格區間的商品列表
     */
    @GetMapping("/price-range")
    public List<ProductDTO> getProductsByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice) {
        return productService.getProductsByPriceRange(minPrice, maxPrice);
    }

    /**
     * GET /api/products/low-stock（Query 參數 threshold）：查找庫存低於閾值的商品。200；參數缺少或格式錯誤 → 400。
     * @param threshold 庫存閾值
     * @return 低庫存商品列表
     */
    @GetMapping("/low-stock")
    public List<ProductDTO> getLowStockProducts(@RequestParam Integer threshold) {
        return productService.getLowStockProducts(threshold);
    }
}
