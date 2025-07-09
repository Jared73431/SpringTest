package com.example.demo.reactiveStream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;

public class ReactiveStreamDemo {

    public static void main(String[] args) {
        System.out.println("=== 基礎 Publisher-Subscriber 示例 ===");
        basicPublisherSubscriber();

        System.out.println("\n=== 自定義 Subscriber 示例 ===");
        customSubscriber();

        System.out.println("\n=== Processor 示例 ===");
        processorExample();

        System.out.println("\n=== 背壓處理示例 ===");
        backpressureExample();

        System.out.println("\n=== 錯誤處理示例 ===");
        errorHandlingExample();
    }

    // 基礎的 Publisher-Subscriber
    static void basicPublisherSubscriber() {
        // 創建 Publisher
        SubmissionPublisher<String> publisher = new SubmissionPublisher<>();

        // 創建 Subscriber
        Flow.Subscriber<String> subscriber = new Flow.Subscriber<String>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                System.out.println("訂閱成功！");
                subscription.request(1); // 請求第一個元素
            }

            @Override
            public void onNext(String item) {
                System.out.println("接收到: " + item);
                subscription.request(1); // 請求下一個元素
            }

            @Override
            public void onError(Throwable throwable) {
                System.err.println("發生錯誤: " + throwable.getMessage());
            }

            @Override
            public void onComplete() {
                System.out.println("數據流完成！");
            }
        };

        // 訂閱
        publisher.subscribe(subscriber);

        // 發送數據
        publisher.submit("Hello");
        publisher.submit("Reactive");
        publisher.submit("Stream");

        // 關閉 Publisher
        publisher.close();

        // 等待處理完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 自定義 Subscriber
    static void customSubscriber() {
        SubmissionPublisher<Integer> publisher = new SubmissionPublisher<>();

        // 自定義 Subscriber - 只處理偶數
        EvenNumberSubscriber subscriber = new EvenNumberSubscriber();
        publisher.subscribe(subscriber);

        // 發送數據
        for (int i = 1; i <= 10; i++) {
            publisher.submit(i);
        }

        publisher.close();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Processor 示例
    static void processorExample() {
        SubmissionPublisher<String> publisher = new SubmissionPublisher<>();

        // 創建一個將字符串轉換為大寫的 Processor
        StringUppercaseProcessor processor = new StringUppercaseProcessor();

        // 創建最終的 Subscriber
        Flow.Subscriber<String> subscriber = new SimpleSubscriber<>("處理後");

        // 連接：Publisher -> Processor -> Subscriber
        publisher.subscribe(processor);
        processor.subscribe(subscriber);

        // 發送數據
        publisher.submit("hello");
        publisher.submit("world");
        publisher.submit("reactive");

        publisher.close();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 背壓處理示例
    static void backpressureExample() {
        // 創建一個緩衝區較小的 Publisher
        SubmissionPublisher<Integer> publisher = new SubmissionPublisher<>(
                ForkJoinPool.commonPool(), 5); // 緩衝區大小為 5

        // 創建一個處理較慢的 Subscriber
        SlowSubscriber subscriber = new SlowSubscriber();
        publisher.subscribe(subscriber);

        // 快速發送大量數據
        CompletableFuture.runAsync(() -> {
            for (int i = 1; i <= 20; i++) {
                try {
                    int lag = publisher.submit(i);
                    System.out.println("提交 " + i + ", 延遲: " + lag);
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.err.println("提交失敗: " + e.getMessage());
                }
            }
            publisher.close();
        });

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 錯誤處理示例
    static void errorHandlingExample() {
        SubmissionPublisher<String> publisher = new SubmissionPublisher<>();

        // 創建會拋出異常的 Subscriber
        ErrorProneSubscriber subscriber = new ErrorProneSubscriber();
        publisher.subscribe(subscriber);

        publisher.submit("正常數據");
        publisher.submit("error"); // 這會觸發錯誤
        publisher.submit("這不會被處理");

        publisher.close();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// 自定義 Subscriber - 只處理偶數
class EvenNumberSubscriber implements Flow.Subscriber<Integer> {
    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        System.out.println("EvenNumberSubscriber 訂閱成功");
        subscription.request(1);
    }

    @Override
    public void onNext(Integer item) {
        if (item % 2 == 0) {
            System.out.println("處理偶數: " + item);
        } else {
            System.out.println("跳過奇數: " + item);
        }
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        System.err.println("EvenNumberSubscriber 錯誤: " + throwable.getMessage());
    }

    @Override
    public void onComplete() {
        System.out.println("EvenNumberSubscriber 完成");
    }
}

// 字符串轉大寫的 Processor
class StringUppercaseProcessor extends SubmissionPublisher<String>
        implements Flow.Processor<String, String> {

    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(String item) {
        // 轉換為大寫並提交給下游
        submit(item.toUpperCase());
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        closeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        close();
    }
}

// 通用的簡單 Subscriber
class SimpleSubscriber<T> implements Flow.Subscriber<T> {
    private Flow.Subscription subscription;
    private String name;

    public SimpleSubscriber(String name) {
        this.name = name;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        System.out.println(name + " 訂閱成功");
        subscription.request(1);
    }

    @Override
    public void onNext(T item) {
        System.out.println(name + " 接收: " + item);
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        System.err.println(name + " 錯誤: " + throwable.getMessage());
    }

    @Override
    public void onComplete() {
        System.out.println(name + " 完成");
    }
}

// 慢速 Subscriber - 模擬背壓
class SlowSubscriber implements Flow.Subscriber<Integer> {
    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        System.out.println("SlowSubscriber 訂閱成功");
        subscription.request(1);
    }

    @Override
    public void onNext(Integer item) {
        try {
            // 模擬慢速處理
            Thread.sleep(500);
            System.out.println("SlowSubscriber 處理: " + item);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        System.err.println("SlowSubscriber 錯誤: " + throwable.getMessage());
    }

    @Override
    public void onComplete() {
        System.out.println("SlowSubscriber 完成");
    }
}

// 會拋出異常的 Subscriber
class ErrorProneSubscriber implements Flow.Subscriber<String> {
    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        System.out.println("ErrorProneSubscriber 訂閱成功");
        subscription.request(1);
    }

    @Override
    public void onNext(String item) {
        if ("error".equals(item)) {
            throw new RuntimeException("處理 " + item + " 時發生錯誤");
        }
        System.out.println("ErrorProneSubscriber 處理: " + item);
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        System.err.println("ErrorProneSubscriber 錯誤: " + throwable.getMessage());
    }

    @Override
    public void onComplete() {
        System.out.println("ErrorProneSubscriber 完成");
    }
}

// 批量處理 Subscriber
class BatchSubscriber implements Flow.Subscriber<Integer> {
    private Flow.Subscription subscription;
    private List<Integer> batch = new ArrayList<>();
    private final int batchSize;

    public BatchSubscriber(int batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        System.out.println("BatchSubscriber 訂閱成功，批次大小: " + batchSize);
        subscription.request(batchSize);
    }

    @Override
    public void onNext(Integer item) {
        batch.add(item);
        if (batch.size() >= batchSize) {
            processBatch();
            batch.clear();
            subscription.request(batchSize);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        System.err.println("BatchSubscriber 錯誤: " + throwable.getMessage());
    }

    @Override
    public void onComplete() {
        if (!batch.isEmpty()) {
            processBatch();
        }
        System.out.println("BatchSubscriber 完成");
    }

    private void processBatch() {
        System.out.println("處理批次: " + batch);
    }
}
