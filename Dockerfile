FROM eclipse-temurin:17-jre
WORKDIR /app
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
# ./gradlew clean build -x test
# docker build -t nooaahh/rebook-trade .