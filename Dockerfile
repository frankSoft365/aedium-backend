FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/aedium-backend-0.0.1-SNAPSHOT.jar app.jar

ENV TZ=Asia/Shanghai

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]