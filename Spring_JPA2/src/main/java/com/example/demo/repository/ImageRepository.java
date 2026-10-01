package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Image;

/**
 * 圖片的 Repository。內建的 findAll() 會連同 data（圖片內容）一起載入，
 * 因此列表另外提供只取摘要欄位的 {@link #findAllSummaries()}。
 */
@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    /**
     * 只查詢摘要欄位，圖片大小由資料庫計算，不把每張圖片的內容讀進記憶體。
     * HQL 的 octet_length() 不接受 byte[]，因此使用 PostgreSQL 原生 SQL；
     * 別名加上雙引號，避免 PostgreSQL 轉成小寫而對不上 ImageSummary 的 getter。
     * 原生 SQL 寫的是資料表與欄位名稱（images、content_type），而非 Entity 屬性名稱，
     * 換資料庫時需要一併檢查；回傳型別使用介面投影 {@link ImageSummary}，不需要額外建立類別。
     */
    @Query(value = """
            SELECT id, name, content_type AS "contentType", octet_length(data) AS "size", upload_date AS "uploadDate"
            FROM images
            ORDER BY id
            """, nativeQuery = true)
    List<ImageSummary> findAllSummaries();
}
