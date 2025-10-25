package com.lina.finance_tracker_bot.bot;

import com.lina.finance_tracker_bot.services.*;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Здесь только маршрутизация сообщений / callback'ов и UI (inline-кнопки).
 */
@Component
public class MessageHandler {

    private final UserInfoService userInfoService;
    private final ReportService reportService;
    private final FactService factService;
    private final CurrencyService currencyService;
    private final AddService addService;
    private final RemoveService removeService;
    private final MessageSender messageSender;
    private final NotificationService notificationService;
    private final UserStateService userStateService;

    public MessageHandler(UserInfoService userInfoService,
                          ReportService reportService,
                          FactService factService,
                          CurrencyService currencyService,
                          AddService addService,
                          RemoveService removeService,
                          NotificationService notificationService,
                          MessageSender messageSender,
                          UserStateService userStateService) {
        this.userInfoService = userInfoService;
        this.reportService = reportService;
        this.factService = factService;
        this.currencyService = currencyService;
        this.addService = addService;
        this.removeService = removeService;
        this.messageSender = messageSender;
        this.notificationService = notificationService;
        this.userStateService = userStateService;
    }

    // Обработка обычных текстовых сообщений
    public void handleMessage(Long chatId, String messageText, String username) {
        // 1) если ожидается выбор категории для удаления
        if (userStateService.isAwaitingCategoryDeletion(chatId)) {
            try {
                int num = Integer.parseInt(messageText.trim());
                String res = removeService.categorySelection(chatId, num, username);
                userStateService.clearAwaitingCategoryDeletion(chatId);
                messageSender.sendTextWithTtl(chatId, res, MessageSender.NOTIFICATION_CREATED_TTL);
            } catch (NumberFormatException e) {
                messageSender.sendTextWithTtl(chatId, "⚠️ Введите номер категории (например: 1).", MessageSender.NOTIFICATION_CREATED_TTL);

            }
            return;
        }

        // 2) если ожидается выбор уведомления для удаления
        if (userStateService.isAwaitingNotificationDeletion(chatId)) {
            try {
                int num = Integer.parseInt(messageText.trim());
                String res = notificationService.handleNotificationSelection(chatId, num, username);
                userStateService.clearAwaitingNotificationDeletion(chatId);
                messageSender.sendText(chatId, res);
            } catch (NumberFormatException e) {
                messageSender.sendTextWithTtl(chatId, "⚠️ Введите номер уведомления (например: 1).", MessageSender.NOTIFICATION_CREATED_TTL);
            }
            return;
        }

        // 3) если пользователь в режиме диалога (add_expense, add_income, create_notification)
        if (userStateService.hasState(chatId)) {
            String state = userStateService.getState(chatId);
            switch (state) {
                case "add_expense" -> {
                    // обработка ввода / делегирование AddService
                    // формат: "Еда 500" или "500 Еда"
                    String[] parts = messageText.trim().split("\\s+");
                    if (parts.length != 2) {
                        if (parts.length < 2)
                            messageSender.sendTextWithTtl(chatId, "⚠️ Введите категорию и сумму через пробел. Пример: Еда 500", MessageSender.NOTIFICATION_CREATED_TTL);
                        else
                            messageSender.sendTextWithTtl(chatId, "⚠️ За раз можно добавить только одну трату.", MessageSender.NOTIFICATION_CREATED_TTL);
                        return;
                    }
                    String first = parts[0];
                    String second = parts[1];
                    String category;
                    double amount;
                    if (isNumeric(first)) {
                        amount = Double.parseDouble(first.replace(",", "."));
                        category = second;
                    } else if (isNumeric(second)) {
                        amount = Double.parseDouble(second.replace(",", "."));
                        category = first;
                    } else {
                        messageSender.sendTextWithTtl(chatId, "⚠️ Одно поле должно быть числом (сумма). Пример: 500 Еда", MessageSender.NOTIFICATION_CREATED_TTL);
                        return;
                    }
                    userStateService.clearState(chatId);
                    String result = addService.addExpense(chatId, category, amount, username);
                    messageSender.sendTextWithTtl(chatId, result, MessageSender.NOTIFICATION_CREATED_TTL);
                    return;
                }
                case "add_income" -> {
                    String[] parts = messageText.trim().split("\\s+");
                    if (parts.length != 2) {
                        if (parts.length < 2)
                            messageSender.sendTextWithTtl(chatId, "⚠️ Введите источник и сумму через пробел. Пример: Зарплата 2000", MessageSender.NOTIFICATION_CREATED_TTL);
                        else
                            messageSender.sendTextWithTtl(chatId, "⚠️ За раз можно добавить только один доход.", MessageSender.NOTIFICATION_CREATED_TTL);
                        return;
                    }
                    String first = parts[0];
                    String second = parts[1];
                    String source;
                    double amount;
                    if (isNumeric(first)) {
                        amount = Double.parseDouble(first.replace(",", "."));
                        source = second;
                    } else if (isNumeric(second)) {
                        amount = Double.parseDouble(second.replace(",", "."));
                        source = first;
                    } else {
                        messageSender.sendTextWithTtl(chatId, "⚠️ Одно поле должно быть числом (сумма). Пример: 2000 Зарплата", MessageSender.NOTIFICATION_CREATED_TTL);
                        return;
                    }
                    userStateService.clearState(chatId);
                    String result = addService.addIncome(chatId, source, amount, username);
                    messageSender.sendTextWithTtl(chatId, result, MessageSender.NOTIFICATION_CREATED_TTL);
                    return;
                }
                case "create_notification" -> {
                    // формат: "20:30 Текст напоминания"
                    String[] parts = messageText.trim().split("\\s+", 2);
                    if (parts.length < 2) {
                        messageSender.sendTextWithTtl(chatId, "⚠️ Формат неверный. Пример: 20:30 Записать траты за день", MessageSender.NOTIFICATION_CREATED_TTL);
                        return;
                    }
                    try {
                        // проверим формат времени
                        java.time.LocalTime.parse(parts[0], UserStateService.TIME_FORMATTER);
                    } catch (Exception e) {
                        messageSender.sendTextWithTtl(chatId, "❌ Неверный формат времени. Используйте HH:mm (например: 09:00)", MessageSender.NOTIFICATION_CREATED_TTL);
                        return;
                    }
                    userStateService.clearState(chatId);
                    String result = notificationService.handleNotifyCommand(chatId, "/notify " + messageText, username);
                    messageSender.sendTextWithTtl(chatId, result, MessageSender.NOTIFICATION_CREATED_TTL);
                    return;
                }
                default -> {
                    userStateService.clearState(chatId);
                    messageSender.sendText(chatId, "❌ Неизвестное состояние, попробуйте снова.");
                }
            }
        }

        // Если ни одна логика не сработала — подсказка
        if ("/start".equals(messageText)) {
            showMainMenu(chatId);
            return;
        }

        messageSender.sendText(chatId, "Используйте меню (кнопки). Нажмите /start, если нужно.");
    }

    // Обработка callback-данных от inline-кнопок
    public void handleCallback(Long chatId, String data, String username) {
        switch (data) {
            case "add_expense" -> {
                userStateService.setState(chatId, "add_expense");
                messageSender.sendTextWithTtl(chatId, "Введите категорию и сумму траты\n💡 Пример: Еда 500 или 500 Еда", MessageSender.NOTIFICATION_CREATED_TTL);
            }
            case "add_income" -> {
                userStateService.setState(chatId, "add_income");
                messageSender.sendTextWithTtl(chatId, "Введите источник и сумму дохода\n💡 Пример: Зарплата 2000", MessageSender.NOTIFICATION_CREATED_TTL);
            }
            case "open_reports" -> showReportsMenu(chatId);
            case "open_rates" -> showRatesMenu(chatId);
            case "delete_category" -> {
                // вызываем отображение списка и ставим флаг через RemoveService
                String msg = removeService.deleteCommand(chatId, username);
                messageSender.sendTextWithTtl(chatId, msg, MessageSender.NOTIFICATION_CREATED_TTL);
            }
            case "clear_history" -> {
                String res = removeService.clearCommand(chatId, username);
                messageSender.sendText(chatId, res);
            }
            case "open_notifications" -> showNotificationsMenu(chatId);
            case "open_fact" -> {
                String fact = factService.getRandomFact();
                messageSender.sendTextWithTtl(chatId, fact, MessageSender.DEFAULT_TTL);
            }

            // отчёты
            case "report_all" -> {
                String r = reportService.generateGeneralReport(chatId);
                messageSender.sendTextWithTtl(chatId, r, MessageSender.NOTIFICATION_CREATED_TTL);
            }
            case "report_today" -> {
                String r = reportService.generateTodayReport(chatId);
                messageSender.sendTextWithTtl(chatId, r, MessageSender.NOTIFICATION_CREATED_TTL);
            }
            case "report_week" -> {
                String r = reportService.generateWeekReport(chatId);
                messageSender.sendTextWithTtl(chatId, r, MessageSender.NOTIFICATION_CREATED_TTL);
            }
            case "report_year" -> {
                String r = reportService.generateYearReport(chatId);
                messageSender.sendTextWithTtl(chatId, r, MessageSender.NOTIFICATION_CREATED_TTL);
            }

            // курсы валют
            case "rate_usd" -> handleRateCommand(chatId, "/rate usd");
            case "rate_eur" -> handleRateCommand(chatId, "/rate eur");
            case "rate_cny" -> handleRateCommand(chatId, "/rate cny");

            // уведомления (submenu)
            case "notify_show" -> {
                String s = notificationService.showUserNotifications(chatId);
                messageSender.sendTextWithTtl(chatId, s, MessageSender.NOTIFICATION_CREATED_TTL);
            }
            case "notify_create" -> {
                userStateService.setState(chatId, "create_notification");
                messageSender.sendTextWithTtl(chatId, "🕓 Введите время и текст уведомления\n💡 Пример: 20:30 Записать траты за день", MessageSender.NOTIFICATION_CREATED_TTL);
            }
            case "notify_delete" -> {
                String msg = notificationService.handleDeleteNotificationCommand(chatId, username);
                // NotificationService установит awaiting через UserStateService
                messageSender.sendTextWithTtl(chatId, msg, MessageSender.NOTIFICATION_CREATED_TTL);
            }

            case "back_main" -> showMainMenu(chatId);
            default -> messageSender.sendText(chatId, "❓ Неизвестная callback-команда");
        }
    }

    private boolean isNumeric(String s) {
        try {
            Double.parseDouble(s.replace(",", "."));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /* ---------- UI (inline menus) ---------- */

    public void showMainMenu(Long chatId) {
        userInfoService.getOrCreateUser(chatId, null);
        SendMessage message = new SendMessage(chatId.toString(), "📋 Главное меню:");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(
                InlineKeyboardButton.builder().text("➕ Добавить трату").callbackData("add_expense").build(),
                InlineKeyboardButton.builder().text("💰 Добавить доход").callbackData("add_income").build()
        ));
        rows.add(List.of(
                InlineKeyboardButton.builder().text("📊 Отчёты").callbackData("open_reports").build(),
                InlineKeyboardButton.builder().text("💱 Курсы валют").callbackData("open_rates").build()
        ));
        rows.add(List.of(
                InlineKeyboardButton.builder().text("🗑 Удалить категорию").callbackData("delete_category").build(),
                InlineKeyboardButton.builder().text("🧹 Очистить историю").callbackData("clear_history").build()
        ));
        rows.add(List.of(
                InlineKeyboardButton.builder().text("🔔 Уведомления").callbackData("open_notifications").build(),
                InlineKeyboardButton.builder().text("\uD83D\uDCDA Случайный факт").callbackData("open_fact").build()
        ));

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        messageSender.sendMessage(message);
    }

    private void showReportsMenu(Long chatId) {
        SendMessage message = new SendMessage(chatId.toString(), "📊 Выберите отчёт:");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(InlineKeyboardButton.builder().text("📘 Общий отчёт").callbackData("report_all").build()));
        rows.add(List.of(InlineKeyboardButton.builder().text("📅 Отчёт за сегодня").callbackData("report_today").build()));
        rows.add(List.of(InlineKeyboardButton.builder().text("📅 Отчёт за неделю").callbackData("report_week").build()));
        rows.add(List.of(InlineKeyboardButton.builder().text("📆 Отчёт за год").callbackData("report_year").build()));
        rows.add(List.of(InlineKeyboardButton.builder().text("⬅️ Назад").callbackData("back_main").build()));

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
        messageSender.SendMessageWithAutoDelete(message, 5 * 60);
    }

    private void showRatesMenu(Long chatId) {
        SendMessage message = new SendMessage(chatId.toString(), "💱 Выберите валюту:");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(InlineKeyboardButton.builder().text("🇺🇸 Курс доллара").callbackData("rate_usd").build()));
        rows.add(List.of(InlineKeyboardButton.builder().text("🇪🇺 Курс евро").callbackData("rate_eur").build()));
        rows.add(List.of(InlineKeyboardButton.builder().text("🇨🇳 Курс юаня").callbackData("rate_cny").build()));
        rows.add(List.of(InlineKeyboardButton.builder().text("⬅️ Назад").callbackData("back_main").build()));

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
        messageSender.SendMessageWithAutoDelete(message, MessageSender.NOTIFICATION_CREATED_TTL);
    }

    private void showNotificationsMenu(Long chatId) {
        SendMessage message = new SendMessage(chatId.toString(), "🔔 Уведомления:");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(InlineKeyboardButton.builder().text("📋 Показать уведомления").callbackData("notify_show").build()));
        rows.add(List.of(InlineKeyboardButton.builder().text("➕ Создать уведомление").callbackData("notify_create").build()));
        rows.add(List.of(InlineKeyboardButton.builder().text("🗑 Удалить уведомление").callbackData("notify_delete").build()));
        rows.add(List.of(InlineKeyboardButton.builder().text("⬅️ Назад").callbackData("back_main").build()));
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        messageSender.SendMessageWithAutoDelete(message, 5 * 60);

    }

    private void handleRateCommand(Long chatId, String messageText) {
        String currency = messageText.replace("/rate", "").trim().toUpperCase();
        String rate = currencyService.getRate(currency);
        messageSender.sendTextWithTtl(chatId, "Курс " + currency + ": " + rate, MessageSender.NOTIFICATION_CREATED_TTL);
    }
}
