FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/reserva-labs-*.jar app.jar
RUN useradd --system --no-create-home app
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
