FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only the pom first and download dependencies.
COPY pom.xml .
RUN mvn dependency:go-offline -B

#Copy the source code and build the jar, skipping tests
# (GitHub Actions runs the tests).
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a normal user, so the app doesn't run as root.
RUN addgroup -S app && adduser -S app -G app

# Take only the finished jar from the build stage.
COPY --from=build /app/target/*.jar app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]