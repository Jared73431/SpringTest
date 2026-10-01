package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Image;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    /**
     * 只查詢摘要欄位，圖片大小由資料庫計算，不把每張圖片的內容讀進記憶體。
     * HQL 的 octet_length() 不接受 byte[]，因此使用 PostgreSQL 原生 SQL；
     * 別名加上雙引號，避免 PostgreSQL 轉成小寫而對不上 ImageSummary 的 getter。
     */
    @Query(value = """
            SELECT id, name, content_type AS "contentType", octet_length(data) AS "size", upload_date AS "uploadDate"
            FROM images
            ORDER BY id
            """, nativeQuery = true)
    List<ImageSummary> findAllSummaries();
}
