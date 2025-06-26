package com.example.demo.batch.processor;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.example.demo.batch.dto.BookDTO;
import com.example.demo.batch.entity.Book;

import lombok.extern.log4j.Log4j2;

/**
 * 書籍數據處理器
 * 功能：
 * 1. 將 Book entity 轉換為 BookDTO
 * 2. 進行數據清理和轉換
 * 3. 添加業務邏輯處理
 * 4. 分類書籍類型
 */
@Log4j2
@Component
public class BookItemProcessor implements ItemProcessor<Book, BookDTO> {

    @Override
    public BookDTO process(final Book book) throws Exception {
        // 數據清理
        String cleanTitle = cleanTitle(book.getTitle());
        String cleanAuthor = cleanAuthor(book.getAuthor());
        String normalizedPublisher = normalizePublisher(book.getPublisher());

        // 分類書籍
        String category = categorizeBook(book);

        // 價格處理（四捨五入到兩位小數）
        Double processedCost = Math.round(book.getCost() * 100.0) / 100.0;

        // 創建處理後的 BookDTO
        BookDTO processedBook = new BookDTO(
                book.getId(),
                book.getISBN(),
                cleanTitle,
                cleanAuthor,
                book.getYear(),
                normalizedPublisher,
                processedCost,
                category,
                true // 標記為已處理
        );

        log.info("Processing book: [{}] -> Category: [{}], Cost: [{}]",
                book.getTitle(), category, processedCost);

        return processedBook;
    }

    /**
     * 清理書籍標題
     */
    private String cleanTitle(String title) {
        if (title == null) return "Unknown Title";

        return title.trim()
                .replaceAll("\\s+", " ") // 移除多餘空格
                .replaceAll("[^\\w\\s\\-:;,.()?!]", "") // 移除特殊字符
                .toUpperCase(); // 轉換為大寫
    }

    /**
     * 清理作者姓名
     */
    private String cleanAuthor(String author) {
        if (author == null) return "Unknown Author";

        author = author.trim().replaceAll("\\s+", " ").toLowerCase();

        StringBuilder sb = new StringBuilder();
        for (String word : author.split(" ")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    /**
     * 標準化出版社名稱
     */
    private String normalizePublisher(String publisher) {
        if (publisher == null) return "Unknown Publisher";

        return publisher.trim()
                .replaceAll("\\s+", " ")
                .toUpperCase();
    }

    /**
     * 根據年份、價格和其他因素分類書籍
     */
    private String categorizeBook(Book book) {
        // 根據出版年份分類
        if (book.getYear() != null) {
            if (book.getYear() >= 2020) {
                return "NEW_RELEASE";
            } else if (book.getYear() >= 2010) {
                return "RECENT";
            } else if (book.getYear() >= 2000) {
                return "CLASSIC";
            } else {
                return "VINTAGE";
            }
        }

        // 根據價格分類（如果年份不可用）
        if (book.getCost() != null) {
            if (book.getCost() > 100) {
                return "PREMIUM";
            } else if (book.getCost() > 50) {
                return "STANDARD";
            } else {
                return "BUDGET";
            }
        }

        return "UNCATEGORIZED";
    }
}
