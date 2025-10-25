# Используем базовый образ OpenJDK
FROM openjdk:17-jdk-slim

# Устанавливаем рабочую директорию внутри контейнера
WORKDIR /app

# Копируем Gradle Wrapper и связанные файлы
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Копируем исходный код
COPY src src

# Предоставляем права на выполнение Gradle Wrapper
RUN chmod +x gradlew

# Собираем проект. 'bootJar' - это задача Spring Boot, которая создает исполняемый JAR.
# --no-daemon предотвращает запуск Gradle Daemon, что хорошо для контейнеров.
RUN ./gradlew bootJar --no-daemon

# Указываем, какой JAR файл запустить
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# Expose the port your application listens on (e.g., 8080 for Spring Boot)
EXPOSE 8080

# Команда для запуска приложения
ENTRYPOINT ["java", "-jar", "app.jar"]