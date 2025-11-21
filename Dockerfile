# --- ЭТАП 1: Сборка (Builder) ---
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app
 
# Копируем файлы Gradle для кэширования зависимостей
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Копируем исходный код
COPY src src

# Даем права и собираем
RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon -x test

# --- ЭТАП 2: Запуск (Runner) ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Копируем ТОЛЬКО jar-файл из этапа сборки (builder)
# Обратите внимание на --from=builder
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
