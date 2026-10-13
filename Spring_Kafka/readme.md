# Spring Kafka - 訂單事件

下單時不直接呼叫出貨、通知，而是送出一個「訂單已建立」的**事件**到 Kafka，由需要的人各自處理：

```text
POST /api/orders ──送出事件──→ Kafka topic「orders」 ──→ 出貨（consumer group：shipping）
     （回應 202）                                    └──→ 通知（consumer group：notification）
```

- 下單的一方不等出貨處理完，也不需要知道有誰在處理
- 出貨服務暫時停掉，事件也不會遺失，恢復後接著讀
- 之後新增「積點」，只要再加一個 consumer group，不必修改下單的程式

Kafka 系列：

| 模組 | 主題 |
|---|---|
| **Spring_Kafka**（本模組） | Kafka 的基礎：producer / consumer、partition 與 key、consumer group、offset 與冪等、重試與 Dead Letter Topic |
| Spring_Kafka2（規劃中） | 情境：兩個服務透過 Kafka 溝通 |

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（`spring-boot-starter-kafka`） |
| Spring for Apache Kafka | 4.1.1 |
| Kafka | 4.2.1（`apache/kafka`，KRaft，不需要 ZooKeeper） |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、Awaitility、Mockito、Testcontainers 2.0（`apache/kafka-native`） |

## 名詞

| 名詞 | 意思 | 本模組 |
|---|---|---|
| **Topic** | 一類訊息的名稱（像資料表） | `orders`、`orders-dlt` |
| **Partition** | topic 分成幾份，每份內的訊息有順序 | `orders` 有 3 個 |
| **Key** | 決定訊息進哪個 partition：同一個 key 一定進同一個 | 訂單編號 |
| **Offset** | 訊息在 partition 中的編號；consumer group 記住讀到哪裡 | |
| **Producer** | 送出訊息的一方 | `OrderController`（`KafkaTemplate`） |
| **Consumer group** | 一組一起分工的 consumer；不同 group 各自收到全部的訊息 | `shipping`、`notification` |
| **Broker** | Kafka 伺服器 | Docker 中的 `kafka` |

## 課程地圖

| 課 | 測試類別 | 主題 |
|---|---|---|
| 1 | [L01_ProducerConsumerTest](src/test/java/com/example/demo/lesson/L01_ProducerConsumerTest.java) | 送出事件、收到事件；訊息在 topic 中長什麼樣子 |
| 2 | [L02_PartitionAndKeyTest](src/test/java/com/example/demo/lesson/L02_PartitionAndKeyTest.java) | 同一個 key 一定進同一個 partition；同一筆訂單的事件依序處理 |
| 3 | [L03_ConsumerGroupTest](src/test/java/com/example/demo/lesson/L03_ConsumerGroupTest.java) | 同一個 group 分工、不同 group 各自收到全部 |
| 4 | [L04_OffsetAndIdempotencyTest](src/test/java/com/example/demo/lesson/L04_OffsetAndIdempotencyTest.java) | offset、「至少送達一次」、冪等的 consumer |
| 5 | [L05_ErrorHandlingTest](src/test/java/com/example/demo/lesson/L05_ErrorHandlingTest.java) | 重試、不重試的錯誤、Dead Letter Topic、格式錯誤的訊息（poison pill） |

## 專案結構

```text
src/main/java/com/example/demo/
├── config/KafkaConfig                    topic、錯誤處理（重試、Dead Letter Topic）
├── order/
│   ├── OrderController                   /api/orders：送出事件；查詢出貨、通知、DLT
│   ├── OrderEvent                        事件（PLACED、CANCELLED）
│   └── DeadLetterListener                監看 orders-dlt
├── shipping/
│   ├── ShippingListener                  consumer group「shipping」（3 個 consumer）
│   ├── ShippingService                   出貨狀態；用 eventId 做到冪等
│   └── ShippingRejectedException         業務規則不允許：不重試
└── notification/NotificationListener     consumer group「notification」
```

處理結果存在記憶體中，讓這個模組專注在 Kafka；重新啟動後會消失（Kafka 中的事件還在）。

## 執行方式

### 測試（主要的學習方式）

需要 Docker（Testcontainers 啟動 Kafka）。

```bash
./gradlew test
```

### Docker Compose

```bash
docker compose up --build -d
# Windows 保留了 8112 時：APP_PORT=8312 docker compose up --build -d

curl -X POST http://localhost:8112/api/orders -H "Content-Type: application/json" \
     -d '{"customer":"alice","item":"book","quantity":2}'           # 202，{"orderId": "..."}
curl http://localhost:8112/api/orders/{orderId}/shipment              # WAITING_FOR_SHIPMENT
curl -X POST http://localhost:8112/api/orders/{orderId}/cancel
curl http://localhost:8112/api/orders/{orderId}/shipment              # CANCELLED
curl http://localhost:8112/api/notifications

docker compose --profile tools up -d kafka-ui                         # http://localhost:9100
docker compose down -v
```

### 本機執行

先啟動 Kafka（例如 `D:\dockercompose\kafka`，或本模組的 `docker compose up -d kafka`），再執行：

```bash
./gradlew bootRun      # 連 localhost:9092
```

### 用 Postman 測試

以下步驟已用 Postman CLI 對 Docker 中的服務實際執行過。Environment 變數 `baseUrl` = `http://localhost:8112`。

consumer 是**非同步**處理的：送出事件後立刻查詢，可能還沒處理完（404），等一兩秒再查。

| 請求 | 設定 | 結果 |
|---|---|---|
| `POST {{baseUrl}}/api/orders` | Body **raw** / **JSON**：`{"customer":"alice","item":"book","quantity":2}`；Scripts → Post-response：`pm.collectionVariables.set("orderId", pm.response.json().orderId);` | 202 |
| `GET {{baseUrl}}/api/orders/{{orderId}}/shipment` | | `WAITING_FOR_SHIPMENT` |
| `POST {{baseUrl}}/api/orders/{{orderId}}/cancel` | | 202 |
| `GET {{baseUrl}}/api/orders/{{orderId}}/shipment` | | `CANCELLED` |
| `POST {{baseUrl}}/api/orders` | quantity 改成 `20` | 202（Kafka 收下了，但出貨會拒絕） |
| `GET {{baseUrl}}/api/dead-letters` | | 20 件的訂單在這裡，`reason` 是拒絕的原因 |
| `GET {{baseUrl}}/api/notifications` | | 每個事件一則通知 |

## API

| 方法 | 路徑 | 說明 | 回應 |
|---|---|---|---|
| POST | `/api/orders` | 送出 PLACED 事件：`{"customer", "item", "quantity"}` | 202 `{"orderId"}`；400 |
| POST | `/api/orders/{orderId}/cancel` | 送出 CANCELLED 事件（同一個 key） | 202 |
| GET | `/api/orders/{orderId}/shipment` | 出貨狀態 | 200；404（還沒處理） |
| GET | `/api/notifications` | 送出的通知 | 200 |
| GET | `/api/dead-letters` | 送進 Dead Letter Topic 的訊息與原因 | 200 |

回應 **202 Accepted**（已受理）而不是 201：事件送到 Kafka 了，但出貨還沒處理。

## Kafka 教學

### 1. 兩個 listener（Docker 設定最容易搞錯的地方）

客戶端第一次連上 Kafka 後，Kafka 會回覆「之後請連到這個位址」（advertised listener）。容器內與主機上看到的位址不同，所以要兩個：

| listener | 位址 | 給誰 |
|---|---|---|
| `PLAINTEXT` | `kafka:19092` | 同一個 compose 中的容器（Docker 中的應用程式、kafka-ui） |
| `PLAINTEXT_HOST` | `localhost:9092` | 主機上的程式（IDE 執行的應用程式） |

實測：Docker 中的應用程式（`kafka:19092`）與主機上執行的 jar（`localhost:9092`）都能正常收送。只設定一個時，常見的錯誤是「連得上，但收送訊息時一直連到錯的位址」。

### 2. 送出與接收（第 1 課）

```java
// Producer：topic、key、value
kafkaTemplate.send("orders", orderId, OrderEvent.placed(...)).join();

// Consumer：方法正常結束 → Spring 替我們 commit offset；拋出例外 → 交給錯誤處理
@KafkaListener(topics = "orders", groupId = "shipping")
public void onOrderEvent(OrderEvent event) { ... }
```

訊息的 value 是 JSON（`JacksonJsonSerializer`）。Spring 另外在標頭 `__TypeId__` 記下類別名稱，consumer 據此轉回物件；`spring.json.trusted.packages` 限制只能轉成指定 package 的類別，不信任訊息裡任意指定的類別。

`.join()` 等 Kafka 確認收到（`acks=all`：所有副本都寫入）才回應 202，確保事件沒有遺失。

### 3. Partition 與 key（第 2 課）

- **同一個 key 一定進同一個 partition**（依 key 的雜湊值決定）
- **同一個 partition 裡的訊息依序被讀取**
- 所以「A 建立」一定在「A 取消」之前處理；不同訂單之間沒有順序，但可以平行處理

key 要選「需要保持順序的單位」：訂單的事件用訂單編號。

### 4. Consumer group（第 3 課）

| | 同一個 group | 不同的 group |
|---|---|---|
| 一則訊息 | 只給其中一個 consumer（分工） | 每個 group 各收到一次（廣播） |
| 本模組 | `shipping` 有 3 個 consumer，各分到 1 個 partition | `shipping` 與 `notification` 都收到每個事件 |

一個 partition 同時只分給 group 裡的一個 consumer，所以 **partition 數量就是一個 group 平行處理的上限**。

### 5. Offset、至少一次、冪等（第 4 課）

每個 consumer group 在每個 partition 各記一個 offset（讀到哪裡），存在 Kafka 裡。處理完才 commit：

- 處理到一半當機，重新啟動後從上次 commit 的位置再讀 → **不會遺失**
- 但處理完、還沒 commit 就當機的那一則，會**再處理一次**

所以 consumer 要**冪等**：`ShippingService` 用事件的 `eventId` 記住處理過的事件，同一個事件第二次直接略過。真實系統把「處理過的 eventId」和業務資料存在同一個資料庫交易中。

這和 [Spring_Batch-4](../Spring_Batch-4) 第 6 課的「狀態欄位 + 時間戳」是同一個觀念：重做幾次結果都一樣，就不怕重複。

### 6. 錯誤處理與 Dead Letter Topic（第 5 課）

| 情況 | 處理 |
|---|---|
| 暫時性的錯誤（例如資料庫斷線） | 每 500ms 重試，最多 2 次 |
| 重試後還是失敗 | 送到 `orders-dlt`，繼續處理下一則 |
| 業務規則不允許（`ShippingRejectedException`：一次超過 10 件） | 重試也沒用：不重試，直接送到 `orders-dlt` |
| JSON 壞掉（poison pill） | 根本轉不成物件：直接送到 `orders-dlt` |

**為什麼要有 Dead Letter Topic**：如果一直重試同一則失敗的訊息，同一個 partition 後面的訊息全部卡住。移到 DLT 之後，後面的訊息照常處理；DLT 的訊息由人查看原因（標頭 `kafka_dlt-exception-message`）、修正後再重新送出。

**Poison pill**：沒有 `ErrorHandlingDeserializer` 時，轉換失敗發生在 consumer 讀取訊息的階段，錯誤處理接不到，consumer 會在同一則訊息上無限重試。

### 7. 沒有實作的進階主題

| 主題 | 解決什麼問題 |
|---|---|
| **Transaction（交易）** | 一次送出多則訊息時，要嘛全部成功、要嘛全部不算 |
| **Outbox pattern** | 「寫入資料庫」和「送出事件」不能放在同一個交易：先把事件寫進資料庫的 outbox 表（和業務資料同一個交易），再由另一個程式送到 Kafka，兩邊才不會不一致 |
| **Exactly-once** | 「讀 → 處理 → 送出」只發生一次；只在 Kafka 內部（Kafka Streams、交易）成立，寫到外部系統時仍然要靠冪等 |
| **Schema Registry / Avro** | 把訊息的格式集中管理，避免 producer 改了欄位、consumer 解析失敗 |

## 面試時能說明的重點

**Q：Kafka 和直接呼叫 REST API 有什麼不同？**
REST 是「等對方處理完」；Kafka 是送出事件就結束，對方之後再處理。對方暫時停掉事件也不會遺失；新增處理的人不必修改送出事件的程式。

**Q：Kafka 怎麼保證順序？**
只保證同一個 partition 內的順序。同一個 key 一定進同一個 partition，所以需要順序的事件用同一個 key，例如同一筆訂單用訂單編號。

**Q：Consumer group 是什麼？**
同一個 group 的 consumer 分工處理，每則訊息只處理一次；不同的 group 各自收到全部的訊息。partition 數量是一個 group 平行處理的上限。

**Q：訊息會不會重複？怎麼處理？**
預設是「至少一次」：處理完、還沒 commit offset 就當機，重新啟動後會再處理一次。所以 consumer 要冪等，例如用事件的 id 記住處理過的事件。

**Q：處理失敗怎麼辦？**
暫時性的錯誤重試；重試後還是失敗、或重試也沒用的錯誤，送到 Dead Letter Topic，不要卡住後面的訊息。格式錯誤的訊息要用 `ErrorHandlingDeserializer`，否則會無限重試。

## 開發時遇到的問題

- **[Learning] Spring Kafka 4 的 Dead Letter Topic 預設名稱是 `<topic>-dlt`**：舊版是 `<topic>.DLT`。一開始建立的是 `orders.DLT`，錯誤處理卻把訊息送到 `orders-dlt`，監看程式收不到。
- **自己組 producer 設定時，Testcontainers 的位址不在裡面**：用 `KafkaProperties` 組 producer 設定時，只拿得到設定檔的 `localhost:9092`；`@ServiceConnection` 提供的 container 位址在 `KafkaConnectionDetails`。改用 Spring Boot 建好的 `ProducerFactory` 的 `copyWithConfigurationOverride(...)`，只換 serializer，位址等設定都沿用。
- **Jackson 3**：JSON 的 serializer 是 `JacksonJsonSerializer` / `JacksonJsonDeserializer`（舊的 `JsonSerializer` 是 Jackson 2 的版本）。
