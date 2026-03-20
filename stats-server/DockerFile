FROM amazoncorretto:21
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 9090
USER 1001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]