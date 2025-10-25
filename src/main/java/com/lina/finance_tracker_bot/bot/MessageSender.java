package com.lina.finance_tracker_bot.bot;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
/**
 * Класс для простых сообщений и с авто удалением
 */
@Service
public class MessageSender {

    // Константы для времени жизни сообщений (в секундах)
    public static final long NOTIFICATION_CREATED_TTL = 2*60; // 2 мин
    public static final long REMINDER_TTL = 10 * 60 * 60; // 10 часов
    public static final long DEFAULT_TTL = 24 * 60 * 60; // 24 часа по умолчанию

    private TelegramLongPollingBot bot;
    private final MessageDeletion messageDeletion;

    public MessageSender(MessageDeletion messageDeletion) {
        this.messageDeletion = messageDeletion;
    }

    public void setTelegramBot(TelegramLongPollingBot bot) {
        this.bot = bot;
    }


    // Отправляет простой текстовый ответ пользователю без TTL.
    public void sendText(Long chatId, String text) {
        sendTextWithTtl(chatId, text, 0);
    }

    // Отправляет простой текстовый ответ пользователю с заданным TTL.
     public void sendTextWithTtl(Long chatId, String text, long ttlSeconds) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText(text);
            Message sentMessage = bot.execute(message);

            if (ttlSeconds > 0) {
                messageDeletion.scheduleMessageForDeletion(chatId, sentMessage.getMessageId(), ttlSeconds);
            }
        } catch (TelegramApiException e) {
            System.out.println("Ошибка отправки сообщения: " + e.getMessage()); // Ваша оригинальная ошибка
        }
    }

    // Отправляет готовый объект SendMessage (например, с inline-клавиатурой) без TTL.
    public void sendMessage(SendMessage message) { // Убрали TelegramLongPollingBot bot из параметров
        sendMessageWithTtl(message, 0); // Вызываем метод с TTL, передавая 0
    }

    // Отправляет готовый объект SendMessage с заданным TTL.
     public void SendMessageWithAutoDelete(SendMessage sendMessage, long ttlSeconds) {
        sendMessageWithTtl(sendMessage, ttlSeconds);
    }

    // Приватный вспомогательный метод для отправки SendMessage и планирования удаления
    private void sendMessageWithTtl(SendMessage sendMessage, long ttlSeconds) {
        try {
            Message sentMessage = bot.execute(sendMessage);

            if (ttlSeconds > 0) {
                messageDeletion.scheduleMessageForDeletion(
                        Long.parseLong(sendMessage.getChatId()),
                        sentMessage.getMessageId(),
                        ttlSeconds
                );
            }
        } catch (TelegramApiException e) {
            System.err.println("Ошибка при отправке SendMessage с авто удалением: " + e.getMessage()); // Ваша оригинальная ошибка
        }
    }



}