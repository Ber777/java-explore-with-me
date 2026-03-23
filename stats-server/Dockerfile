FROM amazoncorretto:21
WORKDIR /app
COPY stats-service/target/*.jar app.jar
EXPOSE 9090
USER 1001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]