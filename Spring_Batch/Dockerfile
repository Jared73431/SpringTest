# syntax=docker/dockerfile:1

# ============================================================
# 最簡單的批次：沒有資料庫（H2 在記憶體）、沒有 port，所以不需要 compose
#
#   建置：docker build -t spring-batch .
#   執行：docker run --rm spring-batch      （印出 Hello world! 後容器自己結束）
#   結果：echo $?                            （Job COMPLETED 為 0，FAILED 為 5）
# ============================================================

# ============================================================
# 第一階段：build
# 在容器內用 JDK 編譯並打包成可執行 jar，本機不需要安裝 JDK
# ============================================================
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY src src

# --mount=type=cache：把 Gradle 下載的依賴快取在 BuildKit，下次 build 不必重新下載
# -x test：測試請在本機或 CI 執行
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar --no-daemon -x test

# ============================================================
# 第二階段：執行
# 只保留 JRE 與 jar，不包含 JDK、Gradle 與原始碼
# ============================================================
FROM eclipse-temurin:21-jre
WORKDIR /app

# 使用非 root 使用者執行，降低容器被入侵時的風險
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring

# build.gradle 已停用 plain jar，build/libs 只會有一個可執行 jar
COPY --from=build /workspace/build/libs/*.jar app.jar

# 沒有 EXPOSE：這是做完就結束的批次，不提供任何網路服務
ENTRYPOINT ["java", "-jar", "app.jar"]
