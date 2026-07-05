FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/aedium-backend-0.0.1-SNAPSHOT.jar app.jar

RUN apk --no-cache add tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]