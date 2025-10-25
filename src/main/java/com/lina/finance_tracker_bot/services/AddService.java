package com.lina.finance_tracker_bot.services;

import com.lina.finance_tracker_bot.modelSqlLite.Transaction;
import com.lina.finance_tracker_bot.modelSqlLite.TransactionType;
import com.lina.finance_tracker_bot.modelSqlLite.User;
import com.lina.finance_tracker_bot.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddService {
    private final TransactionRepository transactionRepository;
    private final UserInfoService userService;

    public AddService(TransactionRepository transactionRepository, UserInfoService userService) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    /**
     * Старый парсер-команда (оставлен для совместимости)
     * /add 500 Еда или /add Еда 500
     */
    public String AddCommand(Long chatId, String messageText, String username) {
        try {
            // если пришёл в формате "/add ..." — простой вызов разборщика
            String[] parts = messageText.trim().split("\\s+");
            if (parts.length >= 3 && parts[0].equalsIgnoreCase("/add")) {
                // ожидаем: /add <amount> <category> или /add <category> <amount>
                String first = parts[1];
                String second = parts[2];
                if (isNumeric(first)) {
                    return addExpense(chatId, second, Double.parseDouble(first), username);
                } else if (isNumeric(second)) {
                    return addExpense(chatId, first, Double.parseDouble(second), username);
                }
            }
            return "❌ Неверный формат команды /add. Используйте: /add 500 Еда";
        } catch (Exception e) {
            return "❌ Ошибка добавления: " + e.getMessage();
        }
    }

    /**
     * Добавить трату (диалоговый режим)
     */
    public String addExpense(Long chatId, String category, double amount, String username) {
        try {
            // Получаем или создаём пользователя
            User user = userService.getOrCreateUser(chatId, username);

            // Нормализуем категорию (в нижний регистр)
            String normalizedCategory = category.toLowerCase();

            // Сохраняем новую транзакцию (расход)
            Transaction transaction = new Transaction(amount, normalizedCategory, TransactionType.EXPENSE, user);
            transactionRepository.save(transaction);

            // Получаем все транзакции пользователя
            List<Transaction> transactions = transactionRepository.findByUserChatId(chatId);

            // Собираем уникальные категории расходов
            java.util.Set<String> expenseCategories = new java.util.HashSet<>();
            for (Transaction t : transactions) {
                if (t.getType() == TransactionType.EXPENSE) {
                    expenseCategories.add(t.getCategory().toLowerCase());
                }
            }

            // Формируем ответ пользователю
            StringBuilder sb = new StringBuilder();
            sb.append("✅ 💸 Расход добавлен: ")
                    .append(String.format("%.2f", amount))
                    .append(" руб. (")
                    .append(capitalize(normalizedCategory))
                    .append(")\n\n");

            if (!expenseCategories.isEmpty()) {
                sb.append("📉 Расходы по категориям:\n");
                int i = 1;
                for (String cat : expenseCategories) {
                    String displayCategory = cat.substring(0, 1).toUpperCase() + cat.substring(1);
                    sb.append("  • ").append(displayCategory).append("\n");
                }
            } else {
                sb.append("❗ Пока нет категорий расходов.");
            }

            return sb.toString();

        } catch (Exception e) {
            return "❌ Ошибка добавления траты: " + e.getMessage();
        }
    }



    //Добавить доход (диалоговый режим)

    public String addIncome(Long chatId, String source, double amount, String username) {
        try {
            User user = userService.getOrCreateUser(chatId, username);
            String normalizedSource = source.toLowerCase();
          Transaction transaction = new Transaction(amount, normalizedSource, TransactionType.INCOME, user);
            transactionRepository.save(transaction);
      List<Transaction> transactions = transactionRepository.findByUserChatId(chatId);
        java.util.Set<String> incomeCategories = new java.util.HashSet<>();
            for (Transaction t : transactions) {
                if (t.getType() == TransactionType.INCOME) {
                    incomeCategories.add(t.getCategory().toLowerCase());
                }
            }

            StringBuilder sb = new StringBuilder();
            StringBuilder append = sb.append("✅ 💰 Доход добавлен: ").append(String.format("%.2f", amount))
                    .append(" руб. (")
                    .append(capitalize(normalizedSource))
                    .append(")\n\n");

            if (!incomeCategories.isEmpty()) {
                sb.append("📁 Ваши категории доходов:\n");
                int i = 1;
                for (String category : incomeCategories) {
                    sb.append(i++).append(". ").append(capitalize(category)).append("\n");
                }
            } else {
                sb.append("❗ Пока нет категорий доходов.");
            }

            return sb.toString();

        } catch (Exception e) {
            return "❌ Ошибка добавления дохода: " + e.getMessage();
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

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }



}