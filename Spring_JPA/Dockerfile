FROM openjdk:11-jdk-slim

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8015

ENTRYPOINT ["java", "-jar", "app.jar"]