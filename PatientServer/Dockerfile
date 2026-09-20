FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src
COPY webapp-runner.jar ./

RUN chmod +x ./gradlew \
    && ./gradlew clean build --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app

ENV PORT=8080

COPY --from=build /workspace/webapp-runner.jar /app/webapp-runner.jar
COPY --from=build /workspace/build/libs/*.war /app/ROOT.war

EXPOSE 8080

CMD ["sh", "-c", "java ${JAVA_OPTS:-} -jar /app/webapp-runner.jar --port ${PORT:-8080} /app/ROOT.war"]
