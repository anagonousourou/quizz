# Use official OpenJDK runtime
FROM amazoncorretto:21.0.9-alpine

# Set working directory inside container
WORKDIR /app

# Copy the JAR file
COPY target/quizz-1.0-SNAPSHOT.jar .

# Expose the port (if your app listens on 8080)
EXPOSE 8080

# Run the app (no need for CMD if you're using `java -jar`)
CMD ["java", "-jar", "quizz-1.0-SNAPSHOT.jar"]
