# Spring Eureka - 服務註冊中心

使用 Spring Cloud Netflix Eureka 建立**服務註冊中心（Service Registry）**。搭配兩個模組一起練習：

| 模組 | 角色 | Port |
|---|---|---|
| **Spring_Eureka**（本模組） | 註冊中心 | `8761` |
| [Spring_Eureka_Provider](../Spring_Eureka_Provider) | 服務提供者（書籍 CRUD），名稱 `service-provider` | `8084` |
| [Spring_Eureka_Client](../Spring_Eureka_Client) | 服務消費者，以服務名稱呼叫 Provider，名稱 `service-consumer` | `8085` |

一次啟動全部服務（含兩個 Provider 實例）的 Docker Compose 放在 [Spring_Eureka_Client](../Spring_Eureka_Client/readme.md#docker)。

這是本 Repo 早期的練習，已完成現代化（Spring Boot 2.7 → 4.1），詳見「[現代化紀錄](#現代化紀錄)」。

## 技術版本

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Spring Cloud | 2025.1.3（Oakwood） |
| Gradle | 9.8.0（使用 Gradle Wrapper） |

## 執行方式

```bash
./gradlew bootRun        # Windows：gradlew.bat bootRun
```

- Dashboard：http://localhost:8761（目前註冊的服務與實例）
- 註冊資料 API：http://localhost:8761/eureka/apps（依 `Accept` header 回傳 XML 或 JSON；`curl -H "Accept: application/json"` 取得 JSON）

啟動順序：**Eureka → Provider → Client**。Eureka 沒啟動時，Provider / Client 仍可啟動，但會持續在 log 中嘗試連線，直到 Eureka 可用。

## 測試

```bash
./gradlew test
```

不需要資料庫或 Docker。以隨機 port 啟動 Eureka，確認 dashboard 與 `/eureka/apps` 可以正常回應、尚未有任何服務註冊。

---

## Eureka 教學

### 1. 為什麼需要服務註冊中心

呼叫另一個服務最直接的方式是寫死位址（見 [Spring_Feign_Client](../Spring_Feign_Client)）：

```java
@FeignClient(name = "book-server", url = "http://localhost:8082")
```

當服務開始「會變動」時就不夠用了：

- 服務部署在多台機器上，IP 會因為重新部署而改變
- 同一個服務開了好幾台，要把請求分散出去（負載平衡）
- 某一台掛掉時，要自動不再把請求送給它

服務註冊中心的想法是：**每個服務自己回報「我是誰、我在哪」，呼叫端只記名字**。

```text
               ┌──────────────── Eureka（:8761）────────────────┐
               │  service-provider → 172.25.0.5:8084、172.25.0.6:8084 │
               │  service-consumer → 172.25.0.7:8085                 │
               └──────────────────────────────────────────────────┘
                 ▲ ① 註冊（啟動時）                ▲ ③ 查詢服務清單（定期）
                 │ ② 心跳（每 30 秒）               │
       Provider ×2（service-provider）     Client（service-consumer）
                 ▲                                 │
                 └──────── ④ 直接呼叫（Eureka 不經手流量）───┘
```

1. **註冊**：Provider 啟動時，以 `spring.application.name` 向 Eureka 回報自己的位址
2. **心跳（renew）**：每 30 秒送一次，代表「我還活著」
3. **查詢**：Client 定期向 Eureka 取得完整的服務清單，存在自己的記憶體中
4. **呼叫**：Client 從清單挑一台（LoadBalancer），**直接**送出 HTTP 請求

> Eureka 只負責「通訊錄」，請求本身**不會經過 Eureka**。Eureka 暫時掛掉時，Client 仍可以用記憶體中的舊清單繼續呼叫。

### 2. 本模組的設定

```properties
server.port=8761
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

Eureka Server 本身也內建 Eureka Client（多台 Eureka 互相備援時，彼此要互相註冊、同步清單）。**單機版**不需要這些，所以關閉，否則它會一直嘗試連到 `localhost:8761` 註冊自己。

| 設定 | 用途 |
|---|---|
| `register-with-eureka=false` | 不把自己註冊到註冊中心 |
| `fetch-registry=false` | 不向其他註冊中心抓取服務清單 |

### 3. 時間相關的預設值：為什麼「剛啟動要等一下」

| 項目 | 預設 | 說明 |
|---|---|---|
| 心跳間隔 | 30 秒 | Provider 多久送一次心跳 |
| 租約過期 | 90 秒 | 超過多久沒收到心跳，Eureka 才會把實例移除 |
| Eureka 回應快取 | 30 秒 | Eureka 對查詢結果的快取 |
| Client 抓清單間隔 | 30 秒 | Client 多久向 Eureka 更新一次清單 |
| LoadBalancer 快取 | 35 秒 | Client 端 LoadBalancer 的清單快取 |

這些快取疊加起來，新啟動的實例可能要**數十秒**才會被呼叫端選到；實例**異常**停止（沒有正常下線）時，呼叫端可能要**一兩分鐘**才不再把請求送給它。

- **正常關閉**（例如 `docker compose stop`）時，Provider 會主動通知 Eureka 下線，其他服務很快就不會再選到它
- 本專案的 Client 把「抓清單間隔」與「LoadBalancer 快取」縮短為 5 秒，讓練習時比較快看到效果；正式環境通常保留預設，避免對註冊中心造成負擔
- 被選到已經停止的實例時，Client 會收到連線錯誤，本專案回 `503`（見 [Spring_Eureka_Client](../Spring_Eureka_Client/readme.md#錯誤回應)）

### 4. 自我保護模式（Self-Preservation）

開發時常在 dashboard 看到這段紅字：

```text
EMERGENCY! EUREKA MAY BE INCORRECTLY CLAIMING INSTANCES ARE UP WHEN THEY'RE NOT.
RENEWALS ARE LESSER THAN THRESHOLD AND HENCE THE INSTANCES ARE NOT BEING EXPIRED JUST TO BE SAFE.
```

**原因**：Eureka 會計算「預期應該收到多少心跳」，如果實際收到的心跳**突然大量減少**（低於預期的 85%），Eureka 會判斷「可能是**我自己的網路**出問題，而不是這些服務真的都掛了」，於是**暫停移除任何實例**，避免把還活著的服務全部踢掉。

| | 正式環境 | 開發環境 |
|---|---|---|
| 情境 | 網路短暫中斷，服務其實都還活著 | 頻繁重啟、只開一兩個服務，心跳數本來就少 |
| 自我保護 | **保護**：避免誤刪大量正常實例 | **困擾**：已經關掉的實例一直留在清單上 |

開發時若想讓實例盡快被移除，可以在 Eureka 加上（本專案保留預設，沒有關閉）：

```properties
# 僅限開發環境：關閉自我保護
eureka.server.enable-self-preservation=false
```

### 5. 啟動時的兩個 WARN

```text
WARN c.n.eureka.cluster.PeerEurekaNodes : The replica size seems to be empty. Check the route 53 DNS Registry
WARN ...LoadBalancerCaffeineWarnLogger   : Spring Cloud LoadBalancer is currently working with the default cache...
```

- 第一個：沒有設定其他 Eureka 節點（單機版），正常現象
- 第二個：LoadBalancer 使用預設的快取實作；正式環境建議加入 Caffeine（`com.github.ben-manes.caffeine:caffeine`）。練習專案可以忽略

### 6. Eureka 還需要嗎？

| 部署環境 | 誰負責「找到服務」 | 需要 Eureka 嗎 |
|---|---|---|
| VM / 實體機，IP 會變動 | Eureka（或 Consul、Nacos） | 需要 |
| Docker Compose | compose 內建 DNS（服務名稱） | 不需要（見 [Spring_Feign_Client](../Spring_Feign_Client)） |
| Kubernetes | K8S Service + DNS（也包含負載平衡、健康檢查） | 不需要 |

| | Eureka | Kubernetes Service |
|---|---|---|
| 註冊方式 | 服務啟動後**自己**註冊、送心跳 | K8S 依 Pod 的 label 與 readiness probe **自動**維護 |
| 負載平衡 | **呼叫端**（Spring Cloud LoadBalancer） | 叢集網路（kube-proxy），呼叫端只連一個 Service 名稱 |
| 語言 | 主要是 Java / Spring 生態 | 與語言無關 |
| 程式碼 | 需要 Eureka Client 依賴與設定 | 不需要任何程式碼 |

**現況**：Spring Cloud Netflix Eureka 仍在維護（Spring Cloud 2025.1 有包含），但新專案大多部署在 K8S，越來越少導入；許多既有系統仍在使用，面試也常被問到兩者的差異。

---

## 現代化紀錄

| 步驟 | 內容 |
|---|---|
| 1 | 補上測試：dashboard 與 `/eureka/apps` 可正常回應 |
| 2 | Gradle 7.5.1 → 8.14.3 |
| 3 | Java 11 → 21、Spring Boot 2.7.5 → 3.5.16、Spring Cloud 2021.0.5 → 2025.0.3 |
| 4 | Spring Boot → 4.1.1、Spring Cloud → 2025.1.3、Gradle → 9.8.0 |
| 5 | 設定改用 kebab-case（`register-with-eureka`），加上註解說明；新增 `spring.application.name` |
| 6 | 新增 Dockerfile，由 Spring_Eureka_Client 的 docker-compose 一起啟動；修正 `gradlew` 執行權限 |

- 程式碼本身（`@EnableEurekaServer`）在整個升級過程中都不需要修改
- `registerWithEureka` 與 `register-with-eureka` 都能正確綁定（Spring Boot 的 relaxed binding），官方建議在設定檔使用 kebab-case
