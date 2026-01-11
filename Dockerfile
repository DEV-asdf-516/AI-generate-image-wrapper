FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx350m", "-Xms256m", "-XX:+UseSerialGC", "-XX:MaxRAM=450m", "-Xss512k", "-jar", "app.jar"]