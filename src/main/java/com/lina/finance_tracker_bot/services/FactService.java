package com.lina.finance_tracker_bot.services;

import com.lina.finance_tracker_bot.bot.MessageSender;
import com.lina.finance_tracker_bot.exeptions.FinanceTrackerException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Сервис для получения и обработки случайных фактов.
 * Получает факты с внешнего ресурса, парсит их из HTML и форматирует для отправки пользователю.
 * При ошибках использует заранее заготовленные запасные факты.
 */
@Service
public class FactService {

    private final FactClient factClient; // Клиент для получения HTML-страницы с фактом
    private final MessageSender messageSender; // Сервис для отправки сообщений пользователям

    /**
     * Конструктор сервиса.
     *
     * @param factClient клиент для получения HTML с фактом
     * @param messageSender сервис для отправки сообщений
     */
    public FactService(FactClient factClient, MessageSender messageSender) {
        this.factClient = factClient;
        this.messageSender = messageSender;
    }

    /**
     * Получает случайный факт с внешнего сайта.
     * При ошибке возвращает запасной факт.
     *
     * @return отформатированный текст факта для отправки пользователю
     */
    public String getRandomFact() {
        try {
            String html = factClient.getFactHtml();
            return parseFactFromHtmlManual(html);
        } catch (FinanceTrackerException e) {
            System.err.println("Ошибка получения факта: " + e.getMessage());
            return getBackupFact();
        }
    }

    /**
     * Парсит факт из HTML-кода с помощью регулярных выражений.
     * Ищет содержимое таблицы с классом "text".
     *
     * @param html HTML-код страницы с фактом
     * @return извлечённый и отформатированный факт или запасной факт при ошибке
     */
    private String parseFactFromHtmlManual(String html) {
        try {
            Pattern pattern = Pattern.compile(
                    "<table class=\"text\">\\s*<tbody>\\s*<tr>\\s*<td>\\s*\"?(.*?)\"?\\s*</td>",
                    Pattern.DOTALL
            );
            Matcher matcher = pattern.matcher(html);

            if (matcher.find()) {
                String fact = matcher.group(1).trim();
                if (!fact.isEmpty()) {
                    return formatFact(fact);
                }
            }

            // Если основной паттерн не сработал — пробуем альтернативный поиск
            return findFactAlternative(html);

        } catch (Exception e) {
            System.err.println("Ошибка парсинга факта: " + e.getMessage());
            return getBackupFact();
        }
    }

    /**
     * Альтернативный метод поиска факта в HTML-коде.
     * Пытается найти факт по ключевым словам или любому достаточно длинному тексту в теге <td>.
     *
     * @param html HTML-код страницы
     * @return найденный факт или запасной факт при неудаче
     */
    private String findFactAlternative(String html) {
        try {
            // Поиск по ключевому словосочетанию "Факт дня:"
            if (html.contains("Факт дня:")) {
                int start = html.indexOf("Факт дня:") + "Факт дня:".length();
                int end = html.indexOf("</td>", start);
                if (end > start) {
                    String fact = html.substring(start, end).trim();
                    fact = fact.replaceAll("<[^>]*>", "").trim();
                    if (!fact.isEmpty()) {
                        return formatFact(fact);
                    }
                }
            }

            // Поиск любого подходящего текста в теге <td>
            Pattern altPattern = Pattern.compile("<td>\\s*([^<]+?)\\s*</td>", Pattern.DOTALL);
            Matcher altMatcher = altPattern.matcher(html);

            while (altMatcher.find()) {
                String candidate = altMatcher.group(1).trim();
                if (candidate.length() > 20 && !candidate.contains("script")) {
                    return formatFact(candidate);
                }
            }

        } catch (Exception e) {
            System.err.println("Альтернативный парсинг не сработал: " + e.getMessage());
        }

        return getBackupFact();
    }

    /**
     * Форматирует текст факта для красивого отображения.
     * Убирает лишние пробелы и добавляет оформляющие элементы.
     *
     * @param fact исходный текст факта
     * @return отформатированный текст факта
     */
    private String formatFact(String fact) {
        fact = fact.replaceAll("\\s+", " ").trim();
        return "📚 Случайный факт:\n\n" + fact +
                "\n\n✨ Узнавайте новый факт каждый день!";
    }

    /**
     * Возвращает один из заранее заготовленных запасных фактов.
     * Используется при ошибках получения или парсинга основного факта.
     *
     * @return текст запасного факта в отформатированном виде
     */
    private String getBackupFact() {
        List<String> backupFacts = List.of(
                "🐝 Медоносные пчёлы могут распознавать человеческие лица!",
                "🌌 Млечный Путь столкнётся с галактикой Андромеды через 4 миллиарда лет",
                "🐙 У осьминога три сердца и голубая кровь",
                "📚 Самый длинный роман в мире — «В поисках утраченного времени» Марселя Пруста",
                "🧠 Человеческий мозг генерирует около 23 ватт энергии"
        );

        int randomIndex = (int) (Math.random() * backupFacts.size());
        return formatFact(backupFacts.get(randomIndex));
    }
}
