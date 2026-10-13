# Spring Kafka2 - Order 服務（兩個服務透過 Kafka 溝通）

[Spring_Kafka](../Spring_Kafka) 在一個應用程式裡練習 Kafka 的基礎；這裡拆成兩個獨立的服務，各自有自己的資料庫，只透過 Kafka 的事件溝通：

```text
           ┌──────────── order-events ────────────┐
           │                                      ↓
  Order 服務（8113）                         Shipping 服務（8114）
  資料庫 Kafka2Order                         資料庫 Kafka2Shipping
           ↑                                      │
           └─────────── shipment-events ←─────────┘
```

1. `POST /api/orders`：Order 服務寫入訂單（`PENDING`），送出 `OrderPlacedEvent`，回應 202
2. Shipping 服務收到事件，決定出貨或拒絕（一次超過 10 件），寫入自己的資料庫，回覆 `ShipmentEvent`
3. Order 服務收到回覆，把訂單改成 `SHIPPED` 或 `REJECTED`

| 模組 | 說明 |
|---|---|
| **Spring_Kafka2_Order**（本模組） | 建立訂單、送出事件、接收出貨結果；**整套系統的 Docker Compose 在這裡** |
| [Spring_Kafka2_Shipping](../Spring_Kafka2_Shipping) | 接收訂單事件、出貨或拒絕、回覆結果 |

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（`spring-boot-starter-kafka`、`starter-jdbc`、`starter-flyway`） |
| Kafka | 4.2.1（KRaft） |
| PostgreSQL | 15（兩個服務各一個） |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、Awaitility、Mockito、Testcontainers 2.0（Kafka、PostgreSQL） |

## 課程地圖

| 課 | 測試類別 | 主題 |
|---|---|---|
| 1 | [L01_PublishEventTest](src/test/java/com/example/demo/lesson/L01_PublishEventTest.java) | 建立訂單、送出事件；兩個服務之間的約定是 JSON，不是 Java 類別 |
| 2 | [L02_ShipmentReplyTest](src/test/java/com/example/demo/lesson/L02_ShipmentReplyTest.java) | 回覆也是事件；最終一致性（PENDING → SHIPPED / REJECTED）；不存在的訂單送到 DLT |
| 3 | [L03_ConsistencyTest](src/test/java/com/example/demo/lesson/L03_ConsistencyTest.java) | 資料庫與 Kafka 是兩個系統：重複的回覆只處理一次、事件送不出去就不建立訂單 |

Shipping 服務的課程見 [Spring_Kafka2_Shipping](../Spring_Kafka2_Shipping)。

測試只啟動一個服務，另一個服務用 Kafka 的 client 扮演：直接送出 / 讀取 JSON 字串，也就是兩個服務之間真正的約定。

## 專案結構

```text
src/main/java/com/example/demo/
├── config/KafkaConfig                  topic、錯誤處理（重試、shipment-events-dlt）
├── event/
│   ├── OrderPlacedEvent                送出的事件
│   ├── ShipmentEvent                   收到的回覆（Order 服務自己的一份）
│   └── ProcessedEventRepository        處理過的事件（冪等）
└── order/
    ├── OrderController                 /api/orders
    ├── OrderService                    建立訂單 + 送出事件；套用出貨結果
    ├── OrderRepository                 JdbcClient
    ├── ShipmentEventListener           consumer group「order-service」
    └── OrderNotFoundException
src/main/resources/db/migration/V1__orders.sql    orders、processed_event
```

## 執行方式

### 測試

需要 Docker（Testcontainers 啟動 Kafka 與 PostgreSQL，不會連到本機的資料庫）。

```bash
./gradlew test
```

### Docker Compose（整套系統）

```bash
docker compose up --build -d
# Windows 保留了 8113 / 8114 時：ORDER_PORT=8313 SHIPPING_PORT=8314 docker compose up --build -d

curl -X POST http://localhost:8113/api/orders -H "Content-Type: application/json" \
     -d '{"customer":"alice","item":"book","quantity":2}'      # 202，status: PENDING
curl http://localhost:8113/api/orders/{id}                        # 一兩秒後：SHIPPED
curl http://localhost:8114/api/shipments/{id}                     # Shipping 服務的紀錄

docker compose --profile tools up -d kafka-ui                     # http://localhost:9100
docker compose down -v
```

會啟動：Kafka、Order 服務 + 它的 PostgreSQL、Shipping 服務 + 它的 PostgreSQL。

### 本機執行

先啟動 Kafka（`D:\dockercompose\kafka`），並建立資料庫 `Kafka2Order`、`Kafka2Shipping`，再分別在兩個資料夾執行：

```bash
./gradlew bootRun
```

### 用 Postman 測試

以下步驟已用 Postman CLI 對 Docker 中的整套系統實際執行過。Environment 變數：`orderUrl` = `http://localhost:8113`、`shippingUrl` = `http://localhost:8114`。

回覆是**非同步**的：建立訂單後立刻查詢會是 `PENDING`，等一兩秒再查。

| 請求 | 設定 | 結果 |
|---|---|---|
| `POST {{orderUrl}}/api/orders` | Body **raw** / **JSON**：`{"customer":"alice","item":"book","quantity":2}`；Scripts → Post-response：`pm.collectionVariables.set("orderId", pm.response.json().id);` | 202，`PENDING` |
| `GET {{orderUrl}}/api/orders/{{orderId}}` | | `SHIPPED` |
| `GET {{shippingUrl}}/api/shipments/{{orderId}}` | | `SHIPPED` |
| `POST {{orderUrl}}/api/orders` | quantity 改成 `20`，Post-response 存成 `rejectedId` | 202，`PENDING` |
| `GET {{orderUrl}}/api/orders/{{rejectedId}}` | | `REJECTED`，reason：`quantity 20 exceeds 10` |
| `POST {{orderUrl}}/api/orders` | quantity 改成 `0` | 400 |
| `GET {{orderUrl}}/api/orders/00000000-0000-0000-0000-000000000000` | | 404 |

## API

| 方法 | 路徑 | 說明 | 回應 |
|---|---|---|---|
| POST | `/api/orders` | 建立訂單並送出事件：`{"customer", "item", "quantity"}` | 202（訂單，`PENDING`）；400；503（Kafka 送不出去，訂單沒有建立） |
| GET | `/api/orders/{orderId}` | 查詢訂單 | 200；404 |

## 教學

### 1. 約定是 JSON，不是 Java 類別（第 1 課）

[Spring_Kafka](../Spring_Kafka) 的 producer 與 consumer 在同一個應用程式，Spring 在標頭 `__TypeId__` 記下類別名稱，consumer 據此轉回物件。兩個服務時這樣做會出問題：

| | 共用類別名稱（`__TypeId__`） | 只約定 JSON 欄位（本模組） |
|---|---|---|
| 類別 | 兩邊的 package、類別名稱必須一樣，通常要共用一個 jar | 各自一份 record，名稱、位置都可以不同 |
| 修改 | 改共用 jar，兩個服務一起重新部署 | 只要 JSON 欄位對得上，各自修改、部署 |

設定：

```properties
# producer：不寫 __TypeId__
spring.kafka.producer.properties.spring.json.add.type.headers=false
# consumer：不看標頭，一律轉成自己的類別
spring.kafka.consumer.properties.spring.json.use.type.headers=false
spring.kafka.consumer.properties.spring.json.value.default.type=com.example.demo.event.ShipmentEvent
```

約定變多、變複雜時，可以用 Schema Registry 集中管理格式（見最後的進階主題）。

### 2. 回覆也是事件；最終一致性（第 2 課）

Order 服務不呼叫 Shipping 服務的 API，而是等它的回覆事件。從送出到收到回覆之間，訂單是 `PENDING`：兩個服務的資料暫時不一致，之後才一致，這叫**最終一致性**。

- API 回應 **202 Accepted**，回傳的訂單是 `PENDING`；前端顯示「處理中」，稍後再查詢
- Shipping 服務暫時停掉，訂單就一直是 `PENDING`；恢復後接著處理，事件不會遺失

**拒絕是回覆，不是錯誤**：一次超過 10 件時，Shipping 服務回覆 `REJECTED` 與原因。如果用拋出例外表示拒絕，訊息會進 Dead Letter Topic，Order 服務永遠不知道結果。

### 3. 資料庫與 Kafka 是兩個系統（第 3 課）

**收：重複的回覆只處理一次**

Kafka 是「至少送達一次」，同一個回覆可能送達兩次。「記住處理過的 eventId」和「更新訂單」放在**同一個資料庫交易**：

```java
@Transactional
public void applyShipmentResult(ShipmentEvent event) {
    if (!processedEvents.markProcessed(event.eventId())) {   // INSERT ... ON CONFLICT DO NOTHING
        return;                                              // 處理過了，略過
    }
    orderRepository.updateStatus(...);
}
```

為什麼要同一個交易：如果先記下「處理過」、更新訂單卻失敗，重試時會被當成重複而略過，這個回覆就遺失了。

**送：寫入訂單 + 送出事件**

「寫入資料庫」和「送到 Kafka」沒辦法放在同一個交易：

| 情況 | 結果 |
|---|---|
| 寫入成功、送出成功、commit 成功 | 正常 |
| 送出失敗（Kafka 連不上） | 拋出例外 → 資料庫 rollback → 回應 503，訂單沒有建立（測試驗證過） |
| 送出成功、但之後 commit 失敗 | 事件已經送出，訂單卻不存在：Shipping 服務出貨了一筆不存在的訂單 ⚠️ |

最後一種情況機率很低，本模組不處理，只說明解決方法：**Outbox pattern**。

```text
同一個資料庫交易：INSERT orders + INSERT outbox（要送出的事件）
                ↓
另一個程式（排程或 Debezium）讀 outbox → 送到 Kafka → 標記已送出
```

事件和訂單在同一個交易，要嘛都有、要嘛都沒有；送到 Kafka 失敗就再送一次（所以 consumer 要冪等）。

## 沒有實作的進階主題

| 主題 | 解決什麼問題 |
|---|---|
| **Outbox pattern** | 「寫入資料庫 + 送出事件」的一致性（見上一節） |
| **Saga** | 跨服務的流程中某一步失敗時，用「補償」事件把前面的步驟復原（例如出貨失敗 → 退款） |
| **Schema Registry / Avro** | 集中管理事件格式，producer 改了欄位時先檢查相容性 |
| **Kafka 交易 / Exactly-once** | Kafka 內部「讀 → 處理 → 送出」只發生一次；寫到資料庫時仍然要靠冪等 |

## 面試時能說明的重點

**Q：兩個服務透過 Kafka 溝通，和直接呼叫 API 有什麼不同？**
呼叫 API 要等對方回應，對方停掉就失敗；透過 Kafka，送出事件就結束，對方之後再處理，停掉也不會遺失。代價是結果要等一段時間才知道（最終一致性），API 要回應 202 並提供查詢狀態的方式。

**Q：為什麼每個服務有自己的資料庫？**
服務之間只透過事件或 API 溝通，各自修改資料表、各自部署，不會互相影響。代價是不能用一個資料庫交易包住兩個服務的資料，要靠事件與冪等達成最終一致。

**Q：兩個服務要共用事件的類別嗎？**
不共用。約定是 JSON 的欄位，兩邊各自一份類別，關掉 `__TypeId__` 標頭；共用類別會讓兩個服務綁在一起部署。

**Q：怎麼避免重複處理？**
consumer 用事件的 id 記住處理過的事件，而且和業務資料寫在同一個資料庫交易：要嘛都成功、要嘛都 rollback，重試時才會正確地再處理一次。

**Q：寫入資料庫和送出事件怎麼保持一致？**
先寫入、再送出，送出失敗就 rollback，大部分情況兩邊一致；剩下「送出成功、commit 失敗」的情況用 Outbox pattern：事件先和業務資料寫在同一個交易，再由另一個程式送到 Kafka。

## 開發時遇到的問題

- **測試中 spy `KafkaTemplate` 時的泛型**：Spring Boot 建立的 Bean 宣告為 `KafkaTemplate<?, ?>`，`@MockitoSpyBean KafkaTemplate<String, Object>` 找不到可以包裝的 Bean，要宣告成 `KafkaTemplate<?, ?>`，設定行為時再轉型。
