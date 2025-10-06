# -----------------------------------------------------
# STAGE 1: BUILDER
# -----------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy POM file first to leverage Docker cache for dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# -----------------------------------------------------
# STAGE 2: RUNNER (Final Image)
# -----------------------------------------------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR file from the builder stage
# !!! ตรวจสอบและแก้ไขชื่อไฟล์ .jar ที่นี่ !!!
COPY --from=builder /app/target/*.jar app.jar

# Configuration
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]