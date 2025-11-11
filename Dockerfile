# Stage 1: Build the application
FROM openjdk:21-ea-34-jdk-slim-bookworm
WORKDIR /app
COPY build/libs/hahl-0.0.1-SNAPSHOT.jar /app/hahl.jar
CMD ["java", "-jar", "/app/hahl.jar"]