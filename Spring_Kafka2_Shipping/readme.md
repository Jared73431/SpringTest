# Spring Kafka2 - Shipping 服務

接收 Order 服務的訂單事件，出貨或拒絕，寫入自己的資料庫，再回覆結果：

```text
order-events ──→ OrderPlacedListener ──→ ShippingService ──→ shipment（Kafka2Shipping）
                                                  └──────→ shipment-events（回覆 SHIPPED / REJECTED）
```

整套系統的說明、Docker Compose、Postman 步驟與面試重點在 [Spring_Kafka2_Order](../Spring_Kafka2_Order)。

## 課程地圖

| 課 | 測試類別 | 主題 |
|---|---|---|
| 1 | [L01_ShipAndReplyTest](src/test/java/com/example/demo/lesson/L01_ShipAndReplyTest.java) | 收到訂單事件 → 出貨 → 回覆事件；JSON 轉成自己的類別 |
| 2 | [L02_RejectOrFailTest](src/test/java/com/example/demo/lesson/L02_RejectOrFailTest.java) | 業務上的「拒絕」要回覆；技術上的「失敗」（JSON 壞掉）進 `order-events-dlt` |
| 3 | [L03_IdempotencyAndRetryTest](src/test/java/com/example/demo/lesson/L03_IdempotencyAndRetryTest.java) | 重複的事件只出貨、回覆一次；回覆送不出去 → 整個交易 rollback → 重試 |

## 專案結構

```text
src/main/java/com/example/demo/
├── config/KafkaConfig                  topic、錯誤處理（重試、order-events-dlt）
├── event/
│   ├── OrderPlacedEvent                收到的事件（Shipping 服務自己的一份）
│   ├── ShipmentEvent                   回覆的事件
│   └── ProcessedEventRepository        處理過的事件（冪等）
└── shipment/
    ├── OrderPlacedListener             consumer group「shipping-service」
    ├── ShippingService                 出貨或拒絕 + 回覆，同一個交易
    ├── ShipmentRepository              JdbcClient
    ├── ShipmentController              GET /api/shipments/{orderId}
    └── Shipment
src/main/resources/db/migration/V1__shipment.sql    shipment、processed_event
```

## 執行方式

```bash
./gradlew test       # 需要 Docker（Testcontainers）
./gradlew bootRun    # 需要 Kafka（localhost:9092）與資料庫 Kafka2Shipping；port 8114
```

Docker：整套系統由 [Spring_Kafka2_Order 的 docker-compose.yml](../Spring_Kafka2_Order/docker-compose.yml) 一起啟動（`build: ../Spring_Kafka2_Shipping`）。

## API

| 方法 | 路徑 | 說明 | 回應 |
|---|---|---|---|
| GET | `/api/shipments/{orderId}` | 某筆訂單的出貨結果 | 200；404（還沒處理） |

沒有「建立出貨」的 API：出貨只由 `order-events` 的事件觸發。

## 教學

### 一個交易包住「處理過的紀錄 + 出貨 + 回覆」

```java
@Transactional
public void handle(OrderPlacedEvent event) {
    if (!processedEvents.markProcessed(event.eventId())) return;   // 重複的事件
    shipmentRepository.insert(...);
    kafkaTemplate.send(SHIPMENT_EVENTS, orderId, reply).join();    // 送不出去 → 拋出例外 → rollback
}
```

| 情況 | 結果 |
|---|---|
| 同一個事件送達兩次 | 第二次 `markProcessed` 回傳 false，略過 |
| 回覆送不出去 | 整個交易 rollback（連「處理過」也沒有），offset 不 commit，重試時從頭再處理（測試驗證：寫了兩次、資料庫只有一筆、回覆一則） |
| 回覆送出、但 commit 失敗 | 重試時再出貨、再回覆一次：Order 服務收到兩則結果相同的回覆，套用兩次結果一樣，不影響正確性 |

### 拒絕 vs 失敗

| | 一次超過 10 件 | JSON 壞掉、重試後仍然失敗 |
|---|---|---|
| 性質 | 業務規則，正常的結果 | 技術問題，需要人處理 |
| 處理 | 回覆 `REJECTED` 與原因 | 送到 `order-events-dlt` |
| Order 服務 | 訂單變成 `REJECTED` | 訂單一直是 `PENDING`（要靠監控 DLT 發現） |
