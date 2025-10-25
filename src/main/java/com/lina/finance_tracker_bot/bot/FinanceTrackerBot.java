package com.lina.finance_tracker_bot.bot;

import com.lina.finance_tracker_bot.services.NotificationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Основной класс Telegram бота для отслеживания финансов
 */
@Component
public class FinanceTrackerBot extends TelegramLongPollingBot {

    private final MessageHandler messageHandler;
    private final NotificationService notificationService;
    private final MessageDeletion messageDeletion;
    private final MessageSender messageSender;

    /**
     * Конструктор бота
     * @param botToken токен бота из конфигурации
     * @param messageHandler обработчик сообщений
     * @param notificationService сервис уведомлений
     * @param messageDeletion сервис удаления сообщений
     * @param messageSender сервис отправки сообщений
     */
    public FinanceTrackerBot(@Value("${bot.token}") String botToken,
                             MessageHandler messageHandler,
                             NotificationService notificationService,
                             MessageDeletion messageDeletion,
                             MessageSender messageSender) {
        super(botToken);
        this.messageHandler = messageHandler;
        this.notificationService = notificationService;
        this.messageDeletion = messageDeletion;
        this.messageSender = messageSender;
    }

    /**
     * Возвращает имя пользователя бота
     * @return имя бота
     */
    @Override
    public String getBotUsername() {
        return "finance_tracker_bot";
    }

    /**
     * Инициализация бота после создания бина
     */
    @PostConstruct
    public void init() {
        this.messageSender.setTelegramBot(this);
        this.messageDeletion.setTelegramBot(this);
    }

    /**
     * Обрабатывает входящие обновления от Telegram
     * @param update объект обновления от Telegram API
     */
    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                Long chatId = update.getMessage().getChatId();
                String username = update.getMessage().getFrom().getUserName();
                messageHandler.handleMessage(chatId, messageText, username);
            } else if (update.hasCallbackQuery()) {
                String data = update.getCallbackQuery().getData();
                Long chatId = update.getCallbackQuery().getMessage().getChatId();
                String username = update.getCallbackQuery().getFrom().getUserName();
                messageHandler.handleCallback(chatId, data, username);
            }
        } catch (Exception e) {
            System.err.println("Ошибка в onUpdateReceived: " + e.getMessage());
        }
    }

    /**
     * Завершает работу бота при уничтожении бина
     */
    @PreDestroy
    public void destroy() {
        messageDeletion.shutdown();
    }
}