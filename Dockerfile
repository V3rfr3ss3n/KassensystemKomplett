FROM eclipse-temurin:24.0.2_12-jre-ubi9-minimal
WORKDIR /app
COPY kassensystem-backend/target/kassensystem-backend-1.0-SNAPSHOT.jar app.jar
RUN mkdir -p /data && chown 10001:0 /data
USER 10001
ENV KASSENSYSTEM_DB_PATH=/data/kassensystem.db
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
