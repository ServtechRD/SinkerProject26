# ---------- build stage ----------
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle
RUN ./gradlew dependencies --no-daemon || true

COPY . .
RUN ./gradlew build -x test -x check --no-daemon

# ---------- run stage ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
