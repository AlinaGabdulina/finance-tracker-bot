package com.lina.finance_tracker_bot.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lina.finance_tracker_bot.exeptions.FinanceTrackerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Сервис для работы с курсами валют
 */
@Service
public class CurrencyService {

    @Autowired
    private CbrClient cbrClient;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Получает курс указанной валюты
     * @param currency код валюты (USD, EUR, CNY)
     * @return строку с курсом валюты или сообщение об ошибке
     */
    public String getRate(String currency) {
        try {
            String currencyUpper = currency.toUpperCase();

            if ("USD".equals(currencyUpper)) {
                return getUSDRate();
            } else if ("EUR".equals(currencyUpper)) {
                return getEURRate();
            } else if ("CNY".equals(currencyUpper)) {
                return getCNYRate();
            } else {
                return "Неизвестная валюта. Используйте USD, EUR или CNY";
            }
        } catch (Exception e) {
            return "Ошибка получения курса: " + e.getMessage();
        }
    }

    /**
     * Получает курс доллара США
     * @return курс USD в рублях
     * @throws FinanceTrackerException если произошла ошибка при получении или парсинге
     */
    public String getUSDRate() throws FinanceTrackerException {
        try {
            String json = cbrClient.getCurrencyRatesJson();
            JsonNode root = objectMapper.readTree(json);
            JsonNode usdNode = root.path("Valute").path("USD");
            return usdNode.path("Value").asText() + " руб.";
        } catch (Exception e) {
            throw new FinanceTrackerException("Ошибка парсинга USD курса: " + e.getMessage(), e);
        }
    }

    /**
     * Получает курс евро
     * @return курс EUR в рублях
     * @throws FinanceTrackerException если произошла ошибка при получении или парсинге
     */
    public String getEURRate() throws FinanceTrackerException {
        try {
            String json = cbrClient.getCurrencyRatesJson();
            JsonNode root = objectMapper.readTree(json);
            JsonNode eurNode = root.path("Valute").path("EUR");
            return eurNode.path("Value").asText() + " руб.";
        } catch (Exception e) {
            throw new FinanceTrackerException("Ошибка парсинга EUR курса: " + e.getMessage(), e);
        }
    }

    /**
     * Получает курс китайского юаня
     * @return курс CNY в рублях
     * @throws FinanceTrackerException если произошла ошибка при получении или парсинге
     */
    public String getCNYRate() throws FinanceTrackerException {
        try {
            String json = cbrClient.getCurrencyRatesJson();
            JsonNode root = objectMapper.readTree(json);
            JsonNode cnyNode = root.path("Valute").path("CNY");
            return cnyNode.path("Value").asText() + " руб.";
        } catch (JsonProcessingException e) {
            throw new FinanceTrackerException("Ошибка парсинга CNY курса: " + e.getMessage(), e);
        }
    }
}