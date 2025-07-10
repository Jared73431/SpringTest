# 使用 OpenJDK 17 作為基礎映像
FROM openjdk:17-jdk-slim

# 設定工作目錄
WORKDIR /app

# 複製 Gradle 建置文件
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# 複製原始碼
COPY src ./src

# 建置應用程式
RUN ./gradlew build -x test

# 複製 JAR 檔案
RUN cp build/libs/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 設定環境變數
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# 啟動應用程式
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]