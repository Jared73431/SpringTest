# Spring WebFlux - Reactive 程式設計

WebFlux 難懂的原因，通常不是 API 太多，而是它**推翻了寫同步程式時的幾個直覺**：呼叫方法不代表會執行、`try/catch` 接不到錯誤、晚來的訂閱者會漏掉資料、一個請求不會一直待在同一個執行緒上……

這個模組依「反直覺的地方」分成 12 課（第 0～11 課）。除了最後一課，每一課都是一個測試類別，**測試名稱就是結論**，可以逐課執行、修改、觀察結果。

這是本 Repo 的練習專案，已完成現代化（Spring Boot 3.5 → 4.1），詳見「[現代化紀錄](#現代化紀錄)」。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1（`spring-boot-starter-webflux`，以 Netty 執行） |
| Reactor | 3.8（Spring Boot 管理） |
| Gradle | 9.8.0（使用 Gradle Wrapper） |
| 測試 | JUnit 5、StepVerifier（reactor-test）、WebTestClient |

不需要資料庫或其他外部服務。資料庫的 Reactive 存取（R2DBC）請見 [Spring_R2DBC2](../Spring_R2DBC2)，WebClient 呼叫外部 API 請見 [Spring_HttpClient](../Spring_HttpClient)。

## 課程地圖

| 課 | 測試類別 | 直覺以為 | 實際上 |
|---|---|---|---|
| 0 | [L00_FlowApiTest](src/test/java/com/example/demo/flow/L00_FlowApiTest.java) | 資料是 Publisher 推過來的 | Subscriber 用 `request(n)` 要多少才送多少（觀察者模式做不到） |
| 1 | [L01_LazinessTest](src/test/java/com/example/demo/reactor/L01_LazinessTest.java) | 呼叫回傳 Mono 的方法就會執行 | 只是組裝出一份食譜，subscribe 才會執行，而且每次 subscribe 都重新執行 |
| 2 | [L02_HotColdTest](src/test/java/com/example/demo/reactor/L02_HotColdTest.java) | 每個訂閱者都會拿到完整的資料 | Hot 的來源只有一份，晚來的訂閱者會漏掉之前的資料 |
| 3 | [L03_IgnoredReturnValueTest](src/test/java/com/example/demo/reactor/L03_IgnoredReturnValueTest.java) | `repository.save(user);` 會存檔 | 回傳值沒接到 chain 上就不會執行，而且沒有任何錯誤 |
| 4 | [L04_FlatMapTest](src/test/java/com/example/demo/reactor/L04_FlatMapTest.java) | flatMap 是回傳 Mono 時用的 map | flatMap 同時執行，順序不保證 |
| 5 | [L05_ErrorAndEmptyTest](src/test/java/com/example/demo/reactor/L05_ErrorAndEmptyTest.java) | try/catch 接住錯誤；查不到就是 null | 錯誤和「沒有資料」都是訊號，要用操作子處理 |
| 6 | [L06_SchedulerTest](src/test/java/com/example/demo/reactor/L06_SchedulerTest.java) | 一個請求一個執行緒 | 程式跑在「送資料過來的那個執行緒」上 |
| 7 | [L07_BackpressureTest](src/test/java/com/example/demo/reactor/L07_BackpressureTest.java) | subscribe 會慢慢要 | 預設 `request(Long.MAX_VALUE)`，全部都要 |
| 8 | [L08_CreateTest](src/test/java/com/example/demo/reactor/L08_CreateTest.java) | 先把資料放進 List，再變成 Flux | 用 `generate` 逐筆計算，用 `create` 包裝 callback / listener 形式的舊 API |
| 9 | [L09_ContextTest](src/test/java/com/example/demo/reactor/L09_ContextTest.java) | 用 ThreadLocal 傳 traceId | 會換執行緒，ThreadLocal 會遺失，要用 Context |
| 10 | [L10_WebEndpointsTest](src/test/java/com/example/demo/web/L10_WebEndpointsTest.java) | — | WebFlux 端點：Annotated vs Functional、JSON / NDJSON / SSE、Sinks 廣播、404、阻塞呼叫 |
| 11 | （本文件） | WebFlux 比較快 | 單一請求不會比較快；Java 21 之後，大多數情況 MVC + Virtual Threads 就夠了 |

建議依照順序閱讀，後面的課會用到前面的觀念。

## 專案結構

```text
src/main/java/com/example/demo/
├── flow/                      第 0 課：JDK Flow API 的 Subscriber / Processor
│   ├── RecordingSubscriber      一次要一筆、記錄收到的資料（其他 Subscriber 的基礎）
│   ├── EvenNumberSubscriber     只保留偶數
│   ├── ErrorProneSubscriber     收到 "error" 時拋出例外
│   ├── SlowSubscriber           慢速處理，觀察背壓
│   ├── BatchSubscriber          一次 request(n) 筆
│   └── UppercaseProcessor       Processor：同時是 Subscriber 與 Publisher
└── web/                       第 10 課：WebFlux 端點
    ├── GreetingController       Annotated Controller
    ├── GreetingRouter           Functional Endpoint（RouterFunction）
    ├── GreetingService          兩種寫法共用的 Service
    ├── StreamController         JSON / NDJSON / SSE
    ├── MessageController        訊息：Sinks 廣播、404 的正確與錯誤寫法
    ├── MessageService           Sinks.many().multicast() + 記憶體儲存
    └── BlockingController       阻塞呼叫的錯誤與正確寫法

src/test/java/com/example/demo/
├── flow/L00_FlowApiTest
├── reactor/L01 ~ L09            第 1～9 課：只有測試，不需要正式程式碼
└── web/L10_WebEndpointsTest
```

## 執行方式

### 測試（主要的學習方式）

```bash
./gradlew test                       # 全部
./gradlew test --tests '*L01*'       # 只跑第 1 課
```

建議的學習方式：先讀測試名稱猜結果，再看測試內容，然後**改改看**。例如把 `concatMap` 換成 `flatMap`，看測試怎麼失敗。

### Docker

```bash
docker build -t spring-webflux .
docker run --rm -p 8094:8094 spring-webflux
```

### 本機執行

```bash
./gradlew bootRun
```

### 端點

| 方法 | URL | 說明 |
|---|---|---|
| GET | `/api/greetings/{name}` | Annotated Controller 回傳 `Mono` |
| GET | `/api/fn/greetings/{name}` | 相同功能，用 Functional Endpoint 寫 |
| GET | `/api/numbers?count=5` | 同一個 `Flux`，依 `Accept` 回傳 JSON 陣列或 NDJSON；`count` 必須在 1～100，否則回傳 400 ProblemDetail |
| GET | `/api/ticks` | SSE 無限串流，每秒一筆 |
| POST | `/api/messages` | 發送訊息（`{"text": "..."}`），廣播給所有 SSE 連線；回傳 201 與 `Location` |
| GET | `/api/messages` | `Accept: application/json`：歷史訊息；`Accept: text/event-stream`：即時接收新訊息 |
| GET | `/api/messages/{id}` | ✅ 查不到時回傳 404 ProblemDetail |
| GET | `/api/messages/{id}/unchecked` | ❌ 教學用：查不到時回傳 200 且沒有內容 |
| GET | `/api/blocking/on-event-loop` | ❌ 阻塞呼叫跑在 Netty 的 event loop 上 |
| GET | `/api/blocking/on-bounded-elastic` | ✅ 阻塞呼叫交給 `boundedElastic` |

用 curl 觀察差異（`-N` 關閉 curl 的緩衝，才看得到逐筆送達）：

```bash
# JSON：等全部產生完（5 × 200ms）才一次回傳
curl -H 'Accept: application/json' 'localhost:8094/api/numbers?count=5'

# NDJSON：每 200ms 送出一行
curl -N -H 'Accept: application/x-ndjson' 'localhost:8094/api/numbers?count=5'

# SSE：每秒一筆，按 Ctrl+C 中斷（伺服器端會取消訂閱，interval 隨之停止）
curl -N localhost:8094/api/ticks

# 比較 thread 欄位
curl localhost:8094/api/blocking/on-event-loop        # {"result":"完成","thread":"reactor-http-epoll-9"}
curl localhost:8094/api/blocking/on-bounded-elastic   # {"result":"完成","thread":"boundedElastic-1"}
```

訊息廣播：開兩個終端機接收，再從第三個終端機發送：

```bash
# 終端機 1、2：接收（一定要指定 Accept，curl 預設的 */* 會被當成 JSON，回傳歷史訊息）
curl -N -H 'Accept: text/event-stream' localhost:8094/api/messages

# 終端機 3：發送（Windows 的 Git Bash 直接在命令列打中文可能不是 UTF-8，可以改用 --data-binary @檔案）
curl -H 'Content-Type: application/json' -d '{"text":"hello"}' localhost:8094/api/messages

# 歷史訊息、404 與 200 的對照
curl -H 'Accept: application/json' localhost:8094/api/messages
curl -i localhost:8094/api/messages/99            # 404 ProblemDetail
curl -i localhost:8094/api/messages/99/unchecked  # 200，沒有內容
```

接收端的實際輸出（連線後先收到一個註解事件 `:connected`）：

```text
:connected

id:1
event:message
data:{"id":1,"text":"大家好","time":"2026-10-06T07:58:16.260335865Z"}
```

## 設定

| 設定 | 預設值 | 說明 |
|---|---|---|
| `server.port` | `8094` | |
| `spring.webflux.problemdetails.enabled` | `true` | 錯誤回應使用 RFC 9457 ProblemDetail |
| `demo.numbers.delay` | `200ms` | `/api/numbers` 每筆資料的間隔 |
| `demo.ticks.interval` | `1s` | `/api/ticks` 的推送間隔 |
| `demo.blocking.delay` | `200ms` | 模擬阻塞呼叫的時間 |

測試會把 `demo.*` 縮短，讓測試快速完成。

---

## WebFlux 教學

### 預備知識

Reactive 程式是**宣告式**（Declarative）的：描述「要做什麼」，而不是一步一步寫「怎麼做」。如果熟悉 Java 8 的 Stream，就已經會一半了：

```java
// 指令式：怎麼做
List<Integer> result = new ArrayList<>();
for (int i : data) {
    if (i % 2 == 0) result.add(i * 10);
}

// 宣告式：做什麼（Stream 和 Flux 的寫法幾乎一樣）
data.stream().filter(i -> i % 2 == 0).map(i -> i * 10).toList();
Flux.fromIterable(data).filter(i -> i % 2 == 0).map(i -> i * 10);
```

差別在於 Stream 是「拉」：呼叫 `toList()` 時同步算完。Flux 則是資料**隨時間**陸續送達，而且可能來自其他執行緒。

### 先建立心智模型：食譜與廚師

```java
Mono<User> mono = userRepository.findById(id)   // ① 組裝（assembly）：寫食譜
        .map(User::normalize)
        .flatMap(this::enrich);

mono.subscribe(...);                             // ② 訂閱（subscription）：把食譜交給廚師
                                                 // ③ 執行：資料一筆筆流過，最後送出 onComplete 或 onError
```

| 階段 | 發生什麼事 | 誰來做 |
|---|---|---|
| 組裝 | 建立一串操作子物件，**什麼都還沒執行** | 你寫的程式 |
| 訂閱 | 訂閱從最下游一路往上傳到資料來源 | 在 WebFlux 中是**框架**：Controller 回傳 Mono 後，由 WebFlux 訂閱 |
| 執行 | 資料從上游往下流，經過每個操作子 | 送出資料的那個執行緒 |

每一個 Mono / Flux 最後只會送出三種訊號：

```text
onNext(資料)*  然後  onComplete（完成）或 onError（錯誤）二選一
Mono：0 或 1 個 onNext     Flux：0 到無限個 onNext
```

**從 null 到 Mono：**

```java
String getName(String id)            // 可能是 null：呼叫端很容易忘記檢查
Optional<String> getName(String id)  // 可能沒有值：型別提醒呼叫端處理
Mono<String> getName(String id)      // 可能沒有值，而且「還沒發生」：要 subscribe 才會去查
```

`Mono` 可以想成「非同步、延遲執行的 Optional」。`Optional.empty()` 對應 `Mono.empty()`，`Optional.map` 對應 `Mono.map`。

後面每一課的「反直覺」，幾乎都可以用這幾個觀念解釋。

### 第 0 課：Reactive Streams 規範

**先看前身：觀察者模式。**Subject 有變化就呼叫每個 Observer 的 `update`，Observer 只能被動接收。它沒辦法說「我只要 2 筆」或「我忙不過來」，也沒有「資料送完了」或「出錯了」的訊號。

Reactive Streams 在觀察者模式上補了這兩件事：

```text
Reactive Streams = 觀察者模式 + 消費者控制速度（背壓）+ 完成與錯誤的訊號
```

Reactor、RxJava、R2DBC、WebClient 都實作同一個規範：4 個介面。JDK 9 把它收錄為 `java.util.concurrent.Flow`。

| 介面 | 角色 |
|---|---|
| `Publisher` | 資料來源，只有 `subscribe(subscriber)` 一個方法 |
| `Subscriber` | 接收端：`onSubscribe`、`onNext`、`onError`、`onComplete` |
| `Subscription` | 兩者之間的連結：`request(n)`、`cancel()` |
| `Processor` | 同時是 Subscriber 與 Publisher（例如 map 這類的操作子） |

最重要的一點：**Subscriber 沒有呼叫 `request(n)`，Publisher 一筆都不會送。**這就是背壓（backpressure）的基礎：由消費者控制速度。

```java
@Override
public void onSubscribe(Flow.Subscription subscription) {
    this.subscription = subscription;
    subscription.request(1);   // 沒有這一行，什麼都收不到
}

@Override
public void onNext(T item) {
    handle(item);
    subscription.request(1);   // 處理完才要下一筆
}
```

`SubmissionPublisher` 在緩衝區滿了的時候有兩種選擇：

| 方法 | 緩衝區滿了 | 適用 |
|---|---|---|
| `submit` | 阻塞生產者，等消費者跟上 | 資料不能遺失 |
| `offer` | 直接丟棄（可以指定 onDrop 處理） | 最新的資料比較重要，例如即時報價 |

> 實際上很少直接使用 Flow API，它是 Reactor 底層的規範。先理解它，Reactor 的 `request`、背壓、`onBackpressureXxx` 就都說得通了。

### 第 1 課：什麼都不會發生，直到 subscribe

```java
Mono.fromCallable(() -> query());   // 組裝：query() 還沒執行
Mono.just(query());                 // ⚠️ query() 已經執行了：just 的參數是一般 Java 運算式
Mono.defer(() -> Mono.just(query()));  // 延後到訂閱時才執行
```

**方法中 return 之前的程式碼，呼叫時就會執行；只有 Mono 裡面的 lambda 會延後。**

```java
Mono<User> findUser(String id) {
    log.info("查詢 {}", id);              // 呼叫 findUser 就執行（組裝階段）
    return Mono.fromCallable(() -> ...);  // 訂閱時才執行
}
```

**Cold Publisher：每次 subscribe 都從頭執行一次。**同一個 WebClient 的 Mono 被訂閱兩次，就會發出兩次 HTTP 請求。需要共用結果時使用 `cache()`。

### 第 2 課：Cold 與 Hot

| | Cold | Hot |
|---|---|---|
| 資料來源 | 每個訂閱者各自一份，從頭執行 | 只有一份，所有訂閱者共用 |
| 晚來的訂閱者 | 一樣拿到完整的資料 | **只收到訂閱之後的資料** |
| 例子 | HTTP 呼叫、資料庫查詢、`Flux.range` | 股價、聊天訊息、使用者點擊、`Sinks` |

兩個訂閱者，第二個晚 2 秒才訂閱每秒一筆的 `Flux.interval`：

```text
Cold              第一個：0, 1, 2, 3     第二個：0, 1（自己的計時器，從 0 開始）
share()           第一個：0, 1, 2, 3     第二個：2, 3（0、1 已經錯過）
replay(1)         第一個：0, 1, 2, 3     第二個：1, 2, 3（先補最近 1 筆）
```

**把 Cold 變成 Hot：**

| 操作子 | 行為 |
|---|---|
| `publish()` | 得到 `ConnectableFlux`，訂閱只是登記，要呼叫 `connect()` 才開始 |
| `autoConnect(n)` | 第 n 個訂閱者到了自動 connect |
| `refCount(n)` | 同上，而且最後一個訂閱者離開時**取消上游**（例如關閉 WebSocket） |
| `share()` | `publish().refCount()` 的簡寫 |
| `replay(n)` | 保留最近 n 筆給晚來的人 |
| `cache()` | 保留全部（第 1 課） |

**由程式主動推送資料：`Sinks`。**

| Sinks | 行為 |
|---|---|
| `Sinks.many().multicast()` | 多個訂閱者共用；`directBestEffort` 在沒有人訂閱時直接丟掉資料 |
| `Sinks.many().replay()` | 保留歷史資料，`limit(n)` 只保留最近 n 筆，例如聊天室的最近訊息 |
| `Sinks.many().unicast()` | 只允許一個訂閱者 |
| `Sinks.one()` | 只送一個值，相當於可以從外部完成的 Mono |

> **`Mono.just` 也算 Hot？**Reactor 文件把 `just` 歸類為 Hot，因為值在組裝時就決定了。但它和 Sinks 的 Hot 不一樣：`just` 的每個訂閱者都拿得到同一個值，不會漏掉；`Sinks.multicast` 晚來的人會漏掉之前的資料。

> Sinks 取代了 Reactor 3.4 以前的 `EmitterProcessor`、`ReplayProcessor` 等 Processor（已在 3.5 移除）。

### 第 3 課：忽略回傳值的 Bug

這是第 1 課的直接後果，也是 WebFlux 最常見的 Bug：

```java
// ❌ save 回傳的 Mono 沒有人訂閱，資料沒有寫入，而且沒有任何錯誤
Mono<User> register(String name) {
    var user = new User(name);
    repository.save(user);
    return Mono.just(user);
}

// ❌ doOnNext 是「順便做點事」（例如 log），不會訂閱 lambda 回傳的 Mono
return repository.save(user)
        .doOnNext(u -> auditRepository.save(log(u)));

// ✅ 接到回傳的 chain 上
return repository.save(user)
        .flatMap(u -> auditRepository.save(log(u)).thenReturn(u));
```

**原則：每一個 Mono / Flux 都必須接到最後回傳的那條 chain 上**（`flatMap`、`then`、`thenReturn`、`Mono.when`…）。

> 在方法裡另外呼叫 `.subscribe()`（fire-and-forget）雖然會執行，但錯誤沒有人處理、無法確認完成、Context 也不會傳遞，通常不建議。

### 第 4 課：map / flatMap / concatMap

| 操作子 | lambda 回傳 | 同時執行 | 保持順序 |
|---|---|---|---|
| `map` | 一般的值 | — | 是 |
| `flatMap` | Mono / Flux | 是 | **否**，誰先完成誰先出來 |
| `concatMap` | Mono / Flux | 否，一個做完才做下一個 | 是 |
| `flatMapSequential` | Mono / Flux | 是 | 是，先完成的會等前面的 |

`Flux.just(3, 1, 2)`，每筆查詢花 `id × 100ms`：

```text
flatMap            → 1, 2, 3   共 300ms
concatMap          → 3, 1, 2   共 600ms（300 + 100 + 200）
flatMapSequential  → 3, 1, 2   共 300ms
```

如果用 `map` 搭配回傳 Mono 的函式，得到的是 `Flux<Mono<T>>`，裡面的 Mono **都沒有被訂閱**。

兩個互不相關的查詢用 `Mono.zip` 同時執行，總時間是較慢的那一個，不是兩者相加。

### 第 5 課：錯誤與空值都是訊號

**try/catch 接不到錯誤**，因為錯誤發生在訂閱時，不是組裝時：

```java
try {
    mono = Mono.fromCallable(() -> 1 / 0);   // 組裝時沒有錯誤
} catch (ArithmeticException e) { ... }      // 永遠不會執行到
```

**錯誤會終止整個串流**：`Flux.just(1, 2, 0, 4).map(i -> 10 / i)` 只會收到 10、5，然後是 onError，4 不會處理。

| 操作子 | 用途 |
|---|---|
| `onErrorReturn(value)` | 換成預設值（串流仍然結束） |
| `onErrorResume(e -> mono)` | 換成另一個 Mono / Flux |
| `onErrorMap(e -> newE)` | 轉換例外，例如技術例外轉成業務例外 |
| `retry(n)` | 重新訂閱（Cold，所以會從頭執行） |
| `doOnError(e -> log)` | 只記錄，不處理 |

要讓單筆錯誤不影響其他資料，**在 flatMap 內部處理**：

```java
Flux.just(1, 2, 0, 4)
    .flatMap(i -> Mono.fromCallable(() -> 10 / i)
            .onErrorResume(ArithmeticException.class, e -> Mono.empty()))   // → 10, 5, 2
```

**Reactor 不允許 null**：`Mono.just(null)` 會立刻拋出 NPE，用 `Mono.justOrEmpty`。查不到資料時是 `Mono.empty()`：不會呼叫 map，直接完成。

> Controller 回傳空的 Mono，HTTP 回應是 **200 且沒有內容**，不是 404。第 10 課用 `/api/messages/{id}` 與 `/api/messages/{id}/unchecked` 實際證明。需要 404 時用 `switchIfEmpty(Mono.error(...))`。

**`switchIfEmpty` 的參數會提前執行**（和第 1 課的 `Mono.just` 相同）：

```java
.switchIfEmpty(findFromDatabase())                   // ⚠️ 不論是否為空，findFromDatabase() 都會被呼叫
.switchIfEmpty(Mono.defer(() -> findFromDatabase())) // ✅ 只有空的時候才呼叫
```

但不是每個 `switchIfEmpty` 都要包 `defer`。`switchIfEmpty(Mono.error(...))`、`switchIfEmpty(ServerResponse.notFound().build())` 也會提前建立，但只是建立物件，成本很低、沒有副作用，所以不需要。**只有參數本身「會做事」（查資料庫、呼叫 API）時才要 `defer`。**

### 第 6 課：程式跑在哪個執行緒？

**Reactor 本身不會開執行緒。**每個操作子跑在「送資料過來的那個執行緒」上，只有遇到以下情況才會換：

- 計時：`delayElement`、`interval`
- 指定 Scheduler：`publishOn`、`subscribeOn`
- 非同步 I/O：WebClient、R2DBC

| Scheduler | 用途 | 執行緒名稱 |
|---|---|---|
| `Schedulers.parallel()` | CPU 運算、計時 | `parallel-N`（數量 = CPU 核心數，**不能阻塞**） |
| `Schedulers.boundedElastic()` | 包裝阻塞呼叫（JDBC、檔案、舊 SDK） | `boundedElastic-N` |
| Netty event loop | 處理所有 HTTP 連線的讀寫 | `reactor-http-nio-N` / `reactor-http-epoll-N`（**不能阻塞**） |

> `Schedulers.elastic()`（沒有上限）在 Reactor 3.5 已移除，只剩 `boundedElastic()`。

```java
Mono.just(1)
    .map(a)                                  // 原本的執行緒
    .publishOn(Schedulers.boundedElastic())  // publishOn：影響「下面」的操作子
    .map(b);                                 // boundedElastic

Mono.fromCallable(source)                    // boundedElastic
    .map(c)                                  // boundedElastic
    .subscribeOn(Schedulers.boundedElastic()); // subscribeOn：影響資料來源，寫在哪裡都一樣
```

`subscribeOn` 常被建議寫在 chain 的最前面，這是為了可讀性，不是規則。同一條 chain 有多個 `subscribeOn` 時，最靠近資料來源的那一個生效。

**阻塞呼叫的正確包裝方式：**

```java
Mono.fromCallable(() -> legacyBlockingCall())     // 延後執行
    .subscribeOn(Schedulers.boundedElastic());    // 交給專門給阻塞用的執行緒
```

在 parallel 或 event loop 執行緒上呼叫 `block()`，Reactor 會直接拋出例外：
`block()/blockFirst()/blockLast() are blocking, which is not supported in thread parallel-1`。

> 只有「真的需要等待」才會檢查：`Mono.just(1).block()` 已經有結果，不必等待，所以不會拋出例外。測試時要用真的會等待的 Mono（例如 `Mono.delay`），否則會以為沒有問題。`main` 方法或測試中的一般執行緒可以 `block()`。

### 第 7 課：背壓

```java
Flux.range(1, 100).subscribe(...);   // request(Long.MAX_VALUE)：全部給我
Flux.range(1, 100).limitRate(10)...  // 每次最多要 10 筆，消費到 75% 時補要下一批
```

有些來源**無法放慢**，例如計時器、使用者事件、即時報價。下游跟不上時，要選擇策略：

| 策略 | 行為 |
|---|---|
| （預設） | 來不及送就拋出 `OverflowException` |
| `onBackpressureDrop()` | 丟掉下游來不及要的資料 |
| `onBackpressureLatest()` | 只保留最新的一筆 |
| `onBackpressureBuffer(n)` | 先存起來，**一定要設上限**，否則可能耗盡記憶體；滿了之後預設出錯，也可以指定 `DROP_OLDEST` / `DROP_LATEST` |

> `onBackpressureBuffer(n)` 滿了之後的錯誤，排在**緩衝區的資料後面**：下游要先把已存的資料拿走，才會收到錯誤。下游一直不 request，就永遠等不到錯誤。

**Virtual Time**：測試和時間有關的串流時，不必真的等待：

```java
StepVerifier.withVirtualTime(() -> Flux.interval(Duration.ofMinutes(1)).take(60))
        .thenAwait(Duration.ofHours(1))   // 虛擬時間快轉一小時，測試瞬間完成
        .expectNextCount(60)
        .verifyComplete();
```

> 使用 `withVirtualTime` 時，Flux 必須在 Supplier **裡面**建立，計時用的 Scheduler 才會被換成虛擬的。

### 第 8 課：generate 與 create

資料常常不是一開始就準備好的：要一筆一筆計算，或是由舊的 API 用 callback / listener 推過來。

| | `Flux.generate` | `Flux.create` |
|---|---|---|
| 誰決定何時產生 | 下游 request 一筆，才呼叫一次產生器 | 來源自己推，不管下游 |
| 每次可送幾筆 | 最多 1 筆（送 2 筆會出錯） | 任意多筆，可以從其他執行緒送 |
| 背壓 | 天生支援（拉） | 不支援，要選 `OverflowStrategy`（BUFFER / DROP / LATEST / ERROR / IGNORE） |
| 適用 | 依狀態逐筆計算的序列 | 把 callback / listener 形式的舊 API 包成 Flux |

**包裝 listener 形式的 API：**

```java
Flux<Double> prices = Flux.create(sink -> {
    Consumer<Double> listener = sink::next;          // listener 收到資料就推給 Flux
    ticker.addListener(listener);                    // 訂閱時才註冊
    sink.onDispose(() -> ticker.removeListener(listener)); // 取消時取消註冊，避免 listener 洩漏
});
```

**包裝只回呼一次的 callback API**（例如 OkHttp 的 `enqueue`）用 `Mono.create`，callback 的成功與失敗分別呼叫 `sink.success(...)`、`sink.error(...)`。回傳 `CompletableFuture` 的 API 不必自己包，用 `Mono.fromFuture(() -> api.call())` 即可。

### 第 9 課：Context 取代 ThreadLocal

第 6 課看到一個請求會在多個執行緒之間切換，所以 ThreadLocal（MDC、`SecurityContextHolder`）會遺失。Reactor 的做法是 **Context**：綁在「這一次訂閱」上，而不是綁在執行緒上。

```java
Mono.just("請求")
    .publishOn(Schedulers.boundedElastic())
    .flatMap(s -> Mono.deferContextual(ctx -> Mono.just(ctx.get("traceId"))))   // 讀取
    .contextWrite(Context.of("traceId", "abc-123"));                            // 寫入
```

**反直覺的地方：Context 從 Subscriber 往上游傳，所以 `contextWrite` 只對寫在它上面的操作子有效。**在 WebFlux 中通常由 `WebFilter` 在最外層寫入，Controller 裡的程式就都讀得到。

> Spring Boot 搭配 Micrometer Tracing 時，traceId 會自動在 Context 與 MDC 之間傳遞（Context Propagation），通常不需要自己寫。Spring Security 的 `ReactiveSecurityContextHolder` 也是用 Context 實作的。

### 第 10 課：WebFlux 端點

**兩種寫法：**

| | Annotated Controller | Functional Endpoint |
|---|---|---|
| 寫法 | `@RestController` + `@GetMapping` | `RouterFunction` + 處理函式 |
| 學習成本 | 低，幾乎和 Spring MVC 一樣 | 需要熟悉 `ServerRequest` / `ServerResponse` |
| 參數綁定與驗證 | 註解自動處理 | 自己寫 |
| 優點 | 熟悉、簡潔 | 路由集中、用程式組合、不靠反射 |

兩種寫法可以共用同一個 Service，差別只在「路由」。團隊熟悉 MVC 的話，Annotated Controller 通常比較容易維護。

**Functional Endpoint 的寫法：**

```java
// Builder 寫法（Spring 5.2 之後推薦）
route().GET("/api/fn/greetings/{name}", handler::greet)
       .POST("/api/fn/greetings", handler::create)
       .build();

// 舊寫法：網路上較早的文章常見
RouterFunctions.route(GET("/api/fn/greetings/{name}"), handler::greet)
        .andRoute(POST("/api/fn/greetings"), handler::create);
```

- 只有一、兩個路由時，處理函式可以直接寫成 lambda，例如本模組的 `GreetingRouter`。路由變多時，把處理函式抽成獨立的 Handler 類別：Router 只負責路由，Handler 負責處理，兩者可以分開測試。
- **參數要自己驗證**：`Integer.parseInt(request.pathVariable("id"))` 遇到非數字的 id 會丟出 `NumberFormatException`，回應是 500 而不是 400。Annotated Controller 的 `@PathVariable long id` 則會自動回傳 400。

**同一個 Flux，不同的 Content-Type：**

| Accept | 回應 | 適用 |
|---|---|---|
| `application/json` | 等全部產生完，一次回傳 JSON 陣列 | 一般 API |
| `application/x-ndjson` | 每產生一筆就送出一行 JSON | 大量資料、服務之間串流 |
| `text/event-stream` | SSE | 瀏覽器即時推播（`EventSource`） |

**客戶端斷線時，WebFlux 會取消訂閱**，`Flux.interval` 這類無限串流隨之停止，不會在背景一直跑。

**Sinks 廣播（第 2 課的 Hot 在 HTTP 上的樣子）：**`MessageService` 用 `Sinks.many().multicast().directBestEffort()`，`POST /api/messages` 送出的訊息會推給所有正在 SSE 連線的客戶端，**晚連線的人收不到之前的訊息**（歷史訊息另外用 JSON 查詢）。幾個實作細節：

- 多個請求可能同時送出訊息，而 Sinks 不允許同時呼叫 `emitNext`（`FAIL_NON_SERIALIZED`），所以使用 `EmitFailureHandler.busyLooping` 短暫重試。
- SSE 連線後先送一個註解事件 `:connected`，讓客戶端立刻收到回應標頭。`Flux.merge` 會先訂閱 Sinks 再送出註解，所以客戶端收到註解時，之後的訊息一定收得到。
- 訊息只存在記憶體中，多個實例之間不會互通。正式環境需要 Redis Pub/Sub 之類的機制。

**阻塞呼叫：**`/api/blocking/on-event-loop` 功能完全正常，在低流量時看不出問題。但 Netty 只有少量 event loop 執行緒，負責所有連線；阻塞 200ms，同一個執行緒上的所有請求都要等。這類問題通常要到壓力測試或正式環境才會出現，所以要從寫法上避免。

> 可以使用 [BlockHound](https://github.com/reactor/BlockHound) 在測試中偵測阻塞呼叫。

### 第 11 課：什麼時候該用 WebFlux？

**WebFlux 不會讓單一請求變快。**它的優勢是用少量執行緒同時服務大量「正在等待」的連線（等資料庫、等外部 API、長連線推播）。

Java 21 的 Virtual Threads 讓傳統的 Spring MVC 也能做到這點：每個請求一個虛擬執行緒，阻塞時不佔用作業系統執行緒（Spring Boot 中設定 `spring.threads.virtual.enabled=true`）。

| | Spring MVC + Virtual Threads | Spring WebFlux |
|---|---|---|
| 程式風格 | 一般的同步程式 | Mono / Flux 鏈 |
| 學習成本 | 低 | 高（本模組的 12 課） |
| 除錯 | 正常的 stack trace | stack trace 難以閱讀，需要 `checkpoint()` 等工具 |
| 生態系 | JDBC、JPA、所有同步函式庫 | 需要 Reactive 版本（R2DBC、Reactive Redis…） |
| 大量等待中的連線 | ✅（Virtual Threads） | ✅ |
| 背壓、串流組合 | 需自己處理 | ✅ 內建 |
| SSE、NDJSON 串流 | 可以（`SseEmitter`），較麻煩 | ✅ 自然 |

**建議：**

- 一般的 CRUD API：**Spring MVC + Virtual Threads**
- 適合 WebFlux 的情況：
  - 以串流為核心（即時推播、事件串流）
  - 需要大量組合非同步呼叫，並控制背壓
  - 整條呼叫鏈都已經是 Reactive（例如 Spring Cloud Gateway）
- 不要混用：在 WebFlux 中大量使用阻塞的函式庫，等於同時承受兩邊的缺點。

### 速查表

| 我想要… | 使用 |
|---|---|
| 延後執行阻塞或耗時的呼叫 | `Mono.fromCallable` / `Mono.defer` |
| 多個訂閱者共用同一個來源 | `share()`、`publish().autoConnect(n)` |
| 晚來的訂閱者也看得到最近的資料 | `replay(n)`、`Sinks.many().replay().limit(n)` |
| 由程式主動推送資料 | `Sinks.many().multicast()` |
| 把回傳 Mono 的呼叫接到 chain 上 | `flatMap`、`then`、`thenReturn` |
| 同時執行、不在乎順序 | `flatMap` |
| 依序執行 | `concatMap` |
| 同時執行，但結果保持順序 | `flatMapSequential` |
| 同時執行兩個查詢再合併 | `Mono.zip` |
| 查不到時回傳 404 | `switchIfEmpty(Mono.error(...))` |
| 查不到時使用預設值 | `defaultIfEmpty` / `switchIfEmpty(Mono.defer(...))` |
| 失敗時重試 | `retry(n)`、`retryWhen(Retry.backoff(...))` |
| 呼叫阻塞的 API | `fromCallable(...).subscribeOn(Schedulers.boundedElastic())` |
| 包裝 listener / callback 形式的 API | `Flux.create` / `Mono.create` |
| 包裝 `CompletableFuture` | `Mono.fromFuture(() -> ...)` |
| 傳遞 traceId、登入者 | Context（`contextWrite` / `deferContextual`） |
| 測試 WebFlux 端點 | `WebTestClient` |

### StepVerifier 速查表

```java
StepVerifier.create(flux)                    // 建立並準備訂閱
StepVerifier.create(flux, 0)                 // 一開始不 request，之後用 thenRequest(n) 控制
StepVerifier.withVirtualTime(() -> flux)     // 虛擬時間：Flux 要在 Supplier 裡建立
```

| 方法 | 用途 |
|---|---|
| `expectNext(a, b)` | 下一筆（幾筆）資料等於 |
| `expectNextCount(n)` | 接下來有 n 筆資料 |
| `expectNextMatches(predicate)` | 下一筆資料符合條件 |
| `assertNext(item -> ...)` | 對下一筆資料寫 AssertJ 斷言 |
| `expectSubscription()` | 確認已訂閱（virtual time 中 `expectNoEvent` 前必須先呼叫） |
| `expectNoEvent(duration)` | 這段時間內沒有任何訊號（virtual time 中會推進時間） |
| `thenAwait(duration)` | 推進虛擬時間 |
| `thenRequest(n)` / `thenCancel()` | 扮演下游：再要 n 筆 / 取消 |
| `verifyComplete()` / `verifyError(Class)` / `expectErrorMessage(msg).verify()` | 結尾：預期完成 / 預期錯誤 |
| `verify(Duration)` | 等待的上限，避免預期的訊號永遠不來時卡住 |

---

## 延伸閱讀

- [從零開始 Reactive Programming - Spring](https://ithelp.ithome.com.tw/users/20141418/ironman/4617)（2021 iThome 鐵人賽，robertwang，共 32 篇）：從觀察者模式、Java 9 Flow、Reactor 到 WebFlux、R2DBC、RSocket。本模組原本的 Flow API 範例就是依照這個系列練習的，本次也對照它補充了 Hot / Cold、`generate` / `create`、Sinks 等課程。
  - 系列寫於 Reactor 3.4 / Spring Boot 2，部分 API 已經不同：`Schedulers.elastic()` 與各種 Processor 在 Reactor 3.5 移除（改用 `boundedElastic()` 與 `Sinks`），`VirtualTimeScheduler` 通常改用 `StepVerifier.withVirtualTime`，Router 改用 Builder 寫法，資料物件可以用 record 取代 Lombok。
  - 系列最後的 RSocket 本 Repo 沒有對應的模組。
- [Reactor 官方文件](https://projectreactor.io/docs/core/release/reference/)：特別是〈Which operator do I need?〉一節。
- [Spring WebFlux 官方文件](https://docs.spring.io/spring-framework/reference/web/webflux.html)

---

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | Baseline 測試：攔截 console 輸出，鎖定原本 Flow 範例的行為，並證明應用程式實際上以 Spring MVC 執行 |
| 2 | Java 17 → 21、Spring Boot 3.5.3 → 3.5.16、Gradle 8.14.2 → 8.14.3 |
| 3 | Spring Boot 4.1.1、Gradle 9.8.0 |
| 4 | 移除 `starter-web`，改以 WebFlux（Netty）執行 |
| 5 | 第 0 課：原本單一檔案的 `main()` 範例拆成獨立類別並改寫成測試 |
| 6 | Reactor 學習測試、WebFlux 端點與 WebTestClient 測試 |
| 7 | Dockerfile，在容器中驗證所有端點 |
| 8 | 對照 iThome 系列補充：重新編號，新增第 2 課 Hot / Cold、第 8 課 generate / create、觀察者模式的對照、有上限的 buffer、Sinks 廣播與 404 的證明、Router Builder 寫法 |

### 修正前發現的問題

| 問題 | 說明 | 處理 |
|---|---|---|
| 模組名稱與內容不符 | 名為 WebFlux，但只有 JDK Flow API 的範例，沒有 Mono / Flux，也沒有任何端點；根目錄 README 卻寫「Mono / Flux 各種操作子示範」 | 依「反直覺的地方」重新規劃為 12 課 |
| 實際上以 Spring MVC 執行 | 同時引入 `starter-web` 與 `starter-webflux` 時，Spring Boot 選擇 MVC，Netty 不會啟動 | 只保留 `starter-webflux` |
| 背壓範例的輸出與下一個範例混在一起 | 20 筆 × 500ms 需要 10 秒以上，但只固定 `Thread.sleep(10000)` | Subscriber 提供 `CompletableFuture`，測試等待完成，不再使用 sleep |
| 註解「緩衝區大小為 5」 | `SubmissionPublisher` 會把容量調整成 2 的次方（實際為 8） | 用測試證明 |
| 標籤「延遲」 | `submit()` 回傳的是「已送出但尚未消費的估計數量」，不是延遲 | 改正說明 |
| `BatchSubscriber` 沒有被使用 | 死碼 | 補上測試，作為 `request(n)` 的範例 |

### 行為變更

| 項目 | 修改前 | 修改後 |
|---|---|---|
| Web 伺服器 | Tomcat（Spring MVC） | Netty（WebFlux） |
| Port | 8080 | 8094 |
| 範例執行方式 | `ReactiveStreamDemo.main()`，結果印在 console | 測試，結果用斷言驗證 |
| API | 無 | 10 個端點（見「[端點](#端點)」） |

### 升級時學到的事

- **Boot 4 的測試 starter 也模組化了**：WebTestClient 與 StepVerifier 來自 `spring-boot-starter-webflux-test`，`@AutoConfigureWebTestClient` 的套件是 `org.springframework.boot.webtestclient.autoconfigure`。
- **Event loop 執行緒名稱在測試中不同**：正式執行時是 Reactor Netty 的全域資源（`reactor-http-nio-*`，Linux 上是 `reactor-http-epoll-*`）；測試中由 Spring 的 `ReactorResourceFactory` 建立專用資源，名稱是 `webflux-http-*`。一開始以為是 Boot 4 改名，在 Docker 中驗證後才發現不是。斷言執行緒名稱時要考慮這一點。
- **Processor 不能放進 try-with-resources**：資源以相反順序關閉，Processor 會比上游的 Publisher 先關閉，下游一筆資料都收不到。Processor 應該在收到上游的 `onComplete` 時自己關閉。
- **`block()` 只在真的需要等待時才檢查執行緒**：`Mono.just(1).block()` 在 parallel 執行緒上也不會拋出例外。
- **StepVerifier 的 virtual time**：`expectNoEvent` 前要先 `expectSubscription()`，否則測試會一直等待（iThome 系列 Day 18 也遇到同樣的問題）。因此在 `junit-platform.properties` 設定每個測試最多 30 秒，避免卡住整個 build。
- **`onBackpressureBuffer` 的錯誤排在緩衝資料後面**：第一版測試在沒有 request 的情況下等待 OverflowException，結果一直等不到，被上面的 30 秒 timeout 擋下來。
- **錯誤訊息會隨版本改變**：`Sinks.many().unicast()` 拒絕第二個訂閱者的訊息，從舊版的 `allows only a single Subscriber` 變成 `only allow a single Subscriber`。斷言時只比對關鍵字比較穩定。
- **同一個 URL 同時提供 JSON 與 SSE 時**，curl 預設的 `Accept: */*` 會選到 JSON，要接收 SSE 必須明確指定 `Accept: text/event-stream`（瀏覽器的 `EventSource` 會自動帶上）。
