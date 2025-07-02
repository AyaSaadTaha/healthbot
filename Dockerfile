# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests clean package

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/healthbot-*.jar app.jar

# Environment variables
ENV MONGODB_URI="mongodb+srv://ayasaadtaha:pdkcMEi7G5cE33P3@telegramhealthbot.hdkqmdy.mongodb.net/?retryWrites=true&w=majority&appName=TelegramHealthBot"
ENV BOT_TOKEN="7636548833:AAFT7TI7XtPWGAdrNi0YtGvFhcQZ_InuN5s"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]