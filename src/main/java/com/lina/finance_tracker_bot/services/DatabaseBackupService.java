package com.lina.finance_tracker_bot.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Сервис для автоматического создания и отправки резервных копий базы данных.
 * Выполняет бэкап файла БД и отправляет его в Telegram-чат администратора.
 */
@Service
public class DatabaseBackupService {

    @Value("${backup.admin-chat-id}")
    private String adminChatId; // ID чата Telegram для отправки бэкапов

    private final TelegramLongPollingBot bot; // Бот для отправки файлов в Telegram

    private static final String DB_FILE = "./finance_bot.db"; // Путь к основному файлу БД
    private static final String BACKUP_FILE = "./finance_bot_backup.db"; // Путь к файлу бэкапа

    /**
     * Конструктор сервиса.
     *
     * @param bot экземпляр Telegram-бота для отправки файлов
     */
    public DatabaseBackupService(TelegramLongPollingBot bot) {
        this.bot = bot;
    }

    /**
     * Запускает автоматическое создание и отправку бэкапа базы данных.
     * Метод выполняется по расписанию (каждые 30 секунд).
     */
    @Scheduled(fixedRate = 3600000) // Каждый час
    public void autoBackup() {
        createAndSendBackup();
    }

    /**
     * Создаёт резервную копию базы данных и отправляет её в Telegram.
     * Если исходный файл БД отсутствует — операция пропускается.
     * При ошибках вывода записывает сообщение в системный err.
     */
    public void createAndSendBackup() {
        try {
            File originalDb = new File(DB_FILE);
            if (!originalDb.exists()) return;

            Files.copy(originalDb.toPath(), new File(BACKUP_FILE).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            sendBackupToTelegram();

        } catch (IOException e) {
            System.err.println("Ошибка создания бэкапа: " + e.getMessage());
        }
    }

    /**
     * Отправляет созданный бэкап базы данных в Telegram-чат администратора.
     * Если файл бэкапа отсутствует — операция пропускается.
     * При ошибке отправки записывает сообщение в системный err.
     */
    private void sendBackupToTelegram() {
        File backupFile = new File(BACKUP_FILE);
        if (!backupFile.exists()) return;

        SendDocument document = new SendDocument();
        document.setChatId(adminChatId);
        document.setDocument(new InputFile(backupFile, "finance_bot_backup.db"));
        document.setCaption("🤖 Автобэкап базы данных");

        try {
            bot.execute(document);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки бэкапа: " + e.getMessage());
        }
    }
}
