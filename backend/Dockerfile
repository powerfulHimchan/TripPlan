FROM gradle:8.12-jdk21 AS builder

WORKDIR /workspace
COPY settings.gradle.kts build.gradle.kts ./
COPY src ./src
RUN gradle bootJar --no-daemon && cp build/libs/*.jar build/app.jar

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=builder /workspace/build/app.jar ./app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
