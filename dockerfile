# build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN addgroup --system app && adduser --system --ingroup app app
RUN mkdir -p /app/docs && chown app:app /app/docs
COPY --from=build /build/target/*-fat-jar.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
