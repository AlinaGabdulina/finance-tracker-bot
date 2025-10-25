package com.lina.finance_tracker_bot.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lina.finance_tracker_bot.bot.FinanceTrackerBot;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Конфигурация Spring для компонентов финансового трекер-бота.
 * Определяет бины, необходимые для работы бота: API Telegram, HTTP-клиент и mapper JSON.
 */
@Configuration
public class FinanceTrackerBotConfiguration {

    /**
     * Создаёт и настраивает TelegramBotsApi, регистрируя в нём экземпляр бота.
     *
     * @param financeTrackerBot экземпляр бота, который будет зарегистрирован в Telegram API
     * @return настроенный объект TelegramBotsApi с зарегистрированным ботом
     * @throws TelegramApiException при ошибке регистрации бота в Telegram API
     */
    @Bean
    public TelegramBotsApi telegramBotsApi(FinanceTrackerBot financeTrackerBot) throws TelegramApiException {
        var api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(financeTrackerBot);
        return api;
    }

    /**
     * Создаёт экземпляр HTTP-клиента OkHttpClient для выполнения HTTP-запросов.

     * @return новый объект OkHttpClient с настройками по умолчанию
     */
    @Bean
    public OkHttpClient OkHttpClient() {
        return new OkHttpClient();
    }

    /**
     * Создаёт экземпляр ObjectMapper для преобразования объектов в JSON и обратно.

     * @return новый объект ObjectMapper с настройками по умолчанию
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
