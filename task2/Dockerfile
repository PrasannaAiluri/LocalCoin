# Build the jar first: mvn clean package
FROM eclipse-temurin:8-jre
WORKDIR /app
COPY target/banking-api.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
