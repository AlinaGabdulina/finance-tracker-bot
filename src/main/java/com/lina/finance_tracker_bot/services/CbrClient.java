package com.lina.finance_tracker_bot.services;

import com.lina.finance_tracker_bot.exeptions.FinanceTrackerException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Клиент для взаимодействия с API Центрального банка РФ (ЦБР)
 * для получения актуальных курсов валют в формате JSON.
 */
@Component
public class CbrClient {

    @Autowired
    private OkHttpClient client; // HTTP-клиент для выполнения запросов

    @Value("${cbr.currency.rates.json.url}")
    private String cbrUrl; // URL API ЦБР для получения курсов валют

    /**
     * @return строка с JSON-ответом от API ЦБР (может быть null, если тело ответа пустое)
     * @throws FinanceTrackerException при ошибке выполнения HTTP-запроса или чтении ответа
     */
    public String getCurrencyRatesJson() throws FinanceTrackerException {
        var request = new Request.Builder()
                .url(cbrUrl)
                .build();
        try (var response = client.newCall(request).execute()) {
            var body = response.body();
            if (body == null) {
                return null;
            } else {
                return body.string();
            }
        } catch (IOException e) {
            throw new FinanceTrackerException("Ошибка получения валют", e);
        }
    }
}
