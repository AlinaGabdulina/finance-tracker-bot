package com.lina.finance_tracker_bot.services;

import com.lina.finance_tracker_bot.exeptions.FinanceTrackerException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Клиент для получения HTML страницы со случайными фактами
 */
@Component
public class FactClient {

    @Autowired
    private OkHttpClient client;

    private static final String FACT_URL = "https://randstuff.ru/fact/";

    /**
     * Получает HTML страницу со случайным фактом
     * @return HTML содержимое страницы
     * @throws FinanceTrackerException если произошла ошибка при запросе
     */
    public String getFactHtml() throws FinanceTrackerException {
        var request = new Request.Builder()
                .url(FACT_URL)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();

        try (var response = client.newCall(request).execute()) {
            var body = response.body();
            if (body == null) {
                throw new FinanceTrackerException("Пустое тело ответа", null);
            }
            return body.string();
        } catch (IOException e) {
            throw new FinanceTrackerException("Ошибка получения факта", e);
        }
    }
}