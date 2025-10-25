package com.lina.finance_tracker_bot.bot;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Сервис для управления автоматическим удалением сообщений
 */
@Service
public class MessageDeletion {

    private TelegramLongPollingBot bot;
    private final ConcurrentLinkedQueue<MessageToDelete> eraseQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public MessageDeletion() {
    }

    /**
     * Устанавливает экземпляр бота для отправки запросов
     * @param bot экземпляр Telegram бота
     */
    public void setTelegramBot(TelegramLongPollingBot bot) {
        this.bot = bot;
        startMessageDeletionWorker();
    }

    /**
     * Добавляет сообщение в очередь на удаление через указанное время
     * @param chatId ID чата
     * @param messageId ID сообщения
     * @param ttlSeconds время жизни сообщения в секундах
     */
    public void scheduleMessageForDeletion(Long chatId, Integer messageId, Long ttlSeconds) {
        if (ttlSeconds > 0) {
            eraseQueue.add(new MessageToDelete(chatId, messageId, System.currentTimeMillis() / 1000, ttlSeconds));
        }
    }

    /**
     * Немедленно удаляет указанное сообщение
     *
     * @param chatId    ID чата
     * @param messageId ID сообщения
     */
    public void deleteMessageImmediately(Long chatId, Integer messageId) {
        if (bot == null) {
            System.err.println("Ошибка: TelegramBot не установлен");
            return;
        }
        try {
            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(chatId.toString());
            deleteMessage.setMessageId(messageId);
            bot.execute(deleteMessage);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка удаления сообщения: " + e.getMessage());
        }
    }

    private void startMessageDeletionWorker() {
        if (!scheduler.isShutdown()) {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    processMessageDeletion();
                } catch (Exception e) {
                    System.err.println("Ошибка в worker удаления сообщений: " + e.getMessage());
                }
            }, 10, 10, TimeUnit.SECONDS);
        }
    }

    private void processMessageDeletion() {
        if (bot == null) return;

        long currentTime = System.currentTimeMillis() / 1000;

        while (!eraseQueue.isEmpty()) {
            MessageToDelete message = eraseQueue.peek();

            if (message == null) {
                eraseQueue.poll();
                continue;
            }

            long deleteTime = message.sentDate + message.ttl;

            if (currentTime >= deleteTime) {
                eraseQueue.poll();
                deleteMessageImmediately(message.chatId, message.messageId);
            } else {
                break;
            }
        }
    }

    /**
     * Внутренний класс для хранения информации о сообщениях для удаления
     */
    private static class MessageToDelete {
        final Long chatId;
        final Integer messageId;
        final Long sentDate;
        final Long ttl;

        MessageToDelete(Long chatId, Integer messageId, Long sentDate, Long ttl) {
            this.chatId = chatId;
            this.messageId = messageId;
            this.sentDate = sentDate;
            this.ttl = ttl;
        }
    }

    /**
     * Корректно завершает работу сервиса
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}