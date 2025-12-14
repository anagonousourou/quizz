# Stage 1: Build stage (builds the JAR)
FROM ubuntu:22.04 AS build

RUN apt-get update && apt-get install -y openjdk-21-jdk
# Set working directory
WORKDIR /app

# Copy Maven/Gradle build files (if needed)
COPY pom.xml ./
COPY .mvn ./.mvn
COPY mvnw ./
COPY src ./src
COPY .gitignore ./

# Build the JAR
RUN chmod +x mvnw
RUN ./mvnw clean package -Pproduction

# Stage 2: Runtime stage
FROM amazoncorretto:21.0.9-alpine

# Set working directory
WORKDIR /app

# Copy the JAR from build stage
COPY --from=build /app/target/quizz-1.0-SNAPSHOT.jar .

# Expose the port
EXPOSE 8080

# Run the app
CMD ["java", "-jar", "quizz-1.0-SNAPSHOT.jar"]
