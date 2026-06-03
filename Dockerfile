FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradle gradle
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .
RUN ./gradlew dependencies --no-daemon
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8096
ENTRYPOINT ["java", "-jar", "app.jar"]
