FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx180m", "-Xms128m", "-XX:+UseSerialGC", "-XX:MaxRAM=256m", "-Xss256k", "-jar", "app.jar"]