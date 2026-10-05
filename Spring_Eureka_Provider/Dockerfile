# syntax=docker/dockerfile:1

# ============================================================
# 第一階段：build
# 在容器內用 JDK 編譯並打包成可執行 jar，
# 本機不需要先執行 ./gradlew bootJar，也不需要安裝 JDK
# ============================================================
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY src src

# --mount=type=cache：把 Gradle 下載的依賴快取在 BuildKit，
# 下次 build 不必重新下載（快取不會進到最終的 image）
# -x test：測試需要 Docker（Testcontainers），請在本機或 CI 執行
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar --no-daemon -x test

# ============================================================
# 第二階段：執行
# 只保留 JRE 與 jar，不包含 JDK、Gradle 與原始碼，
# 映像較小，可被攻擊的東西也較少
# ============================================================
FROM eclipse-temurin:21-jre
WORKDIR /app

# 使用非 root 使用者執行，降低容器被入侵時的風險
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring

# build.gradle 已停用 plain jar，build/libs 只會有一個可執行 jar
COPY --from=build /workspace/build/libs/*.jar app.jar

# 說明程式使用 8084（僅為文件用途，實際對外開放由 compose 的 ports 決定）
EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
