# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

RUN apt-get update && \
    apt-get install -y --no-install-recommends maven && \
    rm -rf /var/lib/apt/lists/*

COPY pom.xml ./
COPY .mvn .mvn

RUN mvn -q -B -DskipTests dependency:go-offline

COPY src src

RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre AS runner
WORKDIR /app

COPY --from=builder /workspace/target/*.jar /app/app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar", "--spring.profiles.active=local"]
