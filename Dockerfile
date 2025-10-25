FROM gradle:jdk17-alpine AS build

WORKDIR /app

COPY --chown=gradle:gradle gradlew .
COPY --chown=gradle:gradle gradle gradle
COPY --chown=gradle:gradle build.gradle settings.gradle .
 
RUN gradle dependencies

COPY --chown=gradle:gradle src src

RUN gradle clean build -x test

FROM openjdk:17-jdk-slim-buster

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
