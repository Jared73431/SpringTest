package com.example.demo.batch.writer;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import com.example.demo.batch.dto.BookDTO;

import lombok.extern.log4j.Log4j2;

/**
 * 書籍數據寫入器
 * 支援多種輸出格式：
 * 1. 控制台輸出
 * 2. CSV 文件輸出
 * 3. 分類統計輸出
 */
@Log4j2
@Component
public class BookItemWriter implements ItemWriter<BookDTO> {


    @Override
    public void write(Chunk<? extends BookDTO> chunk) throws Exception {

        List<? extends BookDTO> books = chunk.getItems();

        log.info("Writing {} processed books", books.size());

        // 統計各分類數量
        long newReleaseCount = books.stream()
                .filter(book -> "NEW_RELEASE".equals(book.category()))
                .count();

        long recentCount = books.stream()
                .filter(book -> "RECENT".equals(book.category()))
                .count();

        long classicCount = books.stream()
                .filter(book -> "CLASSIC".equals(book.category()))
                .count();

        long vintageCount = books.stream()
                .filter(book -> "VINTAGE".equals(book.category()))
                .count();

        // 輸出統計信息
        log.info("=== BATCH PROCESSING STATISTICS ===");
        log.info("New Release: {}, Recent: {}, Classic: {}, Vintage: {}",
                newReleaseCount, recentCount, classicCount, vintageCount);

        // 輸出每本書的詳細信息
        books.forEach(book -> {
            log.info("Processed Book: ID={}, Title={}, Author={}, Category={}, Cost={}",
                    book.id(), book.title(), book.author(), book.category(), book.cost());
        });

        log.info("=== END OF BATCH ===");
    }

    /**
     * 創建 CSV 文件寫入器
     * 將處理後的書籍數據寫入 CSV 文件
     */
    public FlatFileItemWriter<BookDTO> createCsvWriter() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "processed_books_" + timestamp + ".csv";

        return new FlatFileItemWriterBuilder<BookDTO>()
                .name("bookCsvItemWriter")
                .resource(new FileSystemResource(new File("output/" + filename)))
                .delimited()
                .delimiter(",")
                .names("id", "isbn", "title", "author", "year", "publisher", "cost", "category", "isProcessed")
                .headerCallback(writer -> {
                    writer.write("ID,ISBN,Title,Author,Year,Publisher,Cost,Category,IsProcessed");
                })
                .build();
    }

    /**
     * 創建分類報告寫入器
     * 生成按分類統計的報告文件
     */
    public ItemWriter<BookDTO> createCategoryReportWriter() {
        return new ItemWriter<BookDTO>() {
            @Override
            public void write(Chunk<? extends BookDTO> chunk) throws Exception {
                List<? extends BookDTO> books = chunk.getItems();

                // 按分類分組統計
                var categoryStats = books.stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                BookDTO::category,
                                java.util.stream.Collectors.collectingAndThen(
                                        java.util.stream.Collectors.toList(),
                                        list -> new CategoryStat(
                                                list.size(),
                                                list.stream().mapToDouble(BookDTO::cost).average().orElse(0.0),
                                                list.stream().mapToDouble(BookDTO::cost).sum()
                                        )
                                )
                        ));

                // 輸出分類統計報告
                log.info("=== CATEGORY STATISTICS REPORT ===");
                categoryStats.forEach((category, stat) -> {
                    log.info("Category: {} | Count: {} | Avg Price: ${:.2f} | Total Value: ${:.2f}",
                            category, stat.count(), stat.averagePrice(), stat.totalValue());
                });

                // 找出最昂貴和最便宜的書籍
                var mostExpensive = books.stream()
                        .max(java.util.Comparator.comparing(BookDTO::cost))
                        .orElse(null);

                var cheapest = books.stream()
                        .min(java.util.Comparator.comparing(BookDTO::cost))
                        .orElse(null);

                if (mostExpensive != null) {
                    log.info("Most Expensive: {} by {} - ${}",
                            mostExpensive.title(), mostExpensive.author(), mostExpensive.cost());
                }

                if (cheapest != null) {
                    log.info("Cheapest: {} by {} - ${}",
                            cheapest.title(), cheapest.author(), cheapest.cost());
                }

                log.info("=== END OF REPORT ===");
            }
        };
    }

    /**
     * 分類統計內部類
     */
    private record CategoryStat(int count, double averagePrice, double totalValue) {}
}
