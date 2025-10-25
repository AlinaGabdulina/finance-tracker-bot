package com.lina.finance_tracker_bot.services;

import com.lina.finance_tracker_bot.modelSqlLite.Transaction;
import com.lina.finance_tracker_bot.modelSqlLite.TransactionType;
import com.lina.finance_tracker_bot.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для генерации финансовых отчетов
 */
@Service
public class ReportService {
    private final TransactionRepository transactionRepository;

    public ReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Обрабатывает команду отчета и возвращает соответствующий отчет
     * @param chatId ID чата пользователя
     * @param messageText текст команды
     * @return сформированный отчет или сообщение об ошибке
     */
    public String handleReportCommand(Long chatId, String messageText) {
        if (messageText.equals("/report")) {
            return generateGeneralReport(chatId);
        } else if (messageText.equals("/report for today")) {
            return generateTodayReport(chatId);
        } else if (messageText.equals("/report for week")) {
            return generateWeekReport(chatId);
        } else if (messageText.equals("/report for the year")) {
            return generateYearReport(chatId);
        }
        return "❌ Неизвестный тип отчета";
    }

    /**
     * Генерирует общий отчет за все время
     * @param chatId ID чата пользователя
     * @return отчет за все время или сообщение об отсутствии данных
     */
    public String generateGeneralReport(Long chatId) {
        try {
            List<Transaction> transactions = transactionRepository.findByUserChatId(chatId);
            if (transactions.isEmpty()) {
                return "📊 У вас пока нет операций";
            }
            return formatReport(transactions, "все время");
        } catch (Exception e) {
            return "❌ Ошибка формирования отчета: " + e.getMessage();
        }
    }

    /**
     * Генерирует отчет за сегодня
     * @param chatId ID чата пользователя
     * @return отчет за сегодня или сообщение об отсутствии данных
     */
    public String generateTodayReport(Long chatId) {
        try {
            LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
            List<Transaction> todayTransactions = transactionRepository.findTodayTransactions(chatId, startOfDay);

            if (todayTransactions.isEmpty()) {
                return "📊 За сегодня операций нет";
            }
            return formatReport(todayTransactions, "сегодня");
        } catch (Exception e) {
            return "❌ Ошибка формирования отчета за сегодня: " + e.getMessage();
        }
    }

    /**
     * Генерирует отчет за неделю
     * @param chatId ID чата пользователя
     * @return отчет за неделю или сообщение о недостатке данных
     */
    public String generateWeekReport(Long chatId) {
        try {
            Optional<Transaction> firstTransaction = transactionRepository.findFirstByUserChatIdOrderByDateAsc(chatId);
            if (firstTransaction.isEmpty()) {
                return "📊 У вас пока нет операций";
            }

            long daysWithData = java.time.Duration.between(firstTransaction.get().getDate(), LocalDateTime.now()).toDays() + 1;
            if (daysWithData < 7) {
                return "📊 У вас всего " + daysWithData + " дней данных. Недельный отчет будет доступен после 7 дней использования";
            }

            LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
            LocalDateTime startDate = firstTransaction.get().getDate().isAfter(weekAgo) ?
                    firstTransaction.get().getDate() : weekAgo;

            List<Transaction> weekTransactions = transactionRepository.findByUserAndDateRange(chatId, startDate, LocalDateTime.now());

            if (weekTransactions.isEmpty()) {
                return "📊 За неделю операций нет";
            }

            long days = java.time.Duration.between(startDate, LocalDateTime.now()).toDays() + 1;
            String periodInfo = "неделю (" + days + " дней)";
            return formatReport(weekTransactions, periodInfo);

        } catch (Exception e) {
            return "❌ Ошибка формирования отчета за неделю: " + e.getMessage();
        }
    }

    /**
     * Генерирует отчет за год
     * @param chatId ID чата пользователя
     * @return отчет за год или сообщение о недостатке данных
     */
    public String generateYearReport(Long chatId) {
        try {
            Optional<Transaction> firstTransaction = transactionRepository.findFirstByUserChatIdOrderByDateAsc(chatId);
            if (firstTransaction.isEmpty()) {
                return "📊 У вас пока нет операций";
            }

            LocalDateTime yearAgo = LocalDateTime.now().minusYears(1);
            LocalDateTime startDate = firstTransaction.get().getDate().isAfter(yearAgo) ?
                    firstTransaction.get().getDate() : yearAgo;

            if (firstTransaction.get().getDate().isAfter(yearAgo)) {
                long daysWithData = java.time.Duration.between(firstTransaction.get().getDate(), LocalDateTime.now()).toDays() + 1;
                return "📊 У вас всего " + daysWithData + " дней данных. Годовой отчет будет доступен после года использования";
            }

            List<Transaction> yearTransactions = transactionRepository.findByUserAndDateRange(chatId, startDate, LocalDateTime.now());

            if (yearTransactions.isEmpty()) {
                return "📊 За год операций нет";
            }

            long days = java.time.Duration.between(startDate, LocalDateTime.now()).toDays() + 1;
            String periodInfo = "год (" + days + " дней)";
            return formatReport(yearTransactions, periodInfo);

        } catch (Exception e) {
            return "❌ Ошибка формирования отчета за год: " + e.getMessage();
        }
    }

    /**
     * Форматирует список транзакций в читаемый отчет
     * @param transactions список транзакций для отчета
     * @param periodName название периода отчета
     * @return форматированный отчет
     */
    private String formatReport(List<Transaction> transactions, String periodName) {
        double totalIncome = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .mapToDouble(Transaction::getAmount)
                .sum();

        double totalExpense = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .mapToDouble(Transaction::getAmount)
                .sum();

        java.util.Map<String, Double> incomeByCategory = new java.util.HashMap<>();
        java.util.Map<String, Double> expenseByCategory = new java.util.HashMap<>();

        for (Transaction transaction : transactions) {
            String category = transaction.getCategory().toLowerCase();
            double amount = transaction.getAmount();

            if (transaction.getType() == TransactionType.INCOME) {
                incomeByCategory.put(category, incomeByCategory.getOrDefault(category, 0.0) + amount);
            } else {
                expenseByCategory.put(category, expenseByCategory.getOrDefault(category, 0.0) + amount);
            }
        }

        StringBuilder report = new StringBuilder();
        report.append("📊 Отчет за ").append(periodName).append(":\n\n");
        report.append("💰 Доходы: ").append(String.format("%.2f", totalIncome)).append(" руб.\n");
        report.append("💸 Расходы: ").append(String.format("%.2f", totalExpense)).append(" руб.\n");
        report.append("⚖️ Баланс: ").append(String.format("%.2f", totalIncome - totalExpense)).append(" руб.\n\n");

        if (!incomeByCategory.isEmpty()) {
            report.append("📈 Доходы по категориям:\n");
            for (java.util.Map.Entry<String, Double> entry : incomeByCategory.entrySet()) {
                String category = entry.getKey();
                String displayCategory = category.substring(0, 1).toUpperCase() + category.substring(1);
                report.append("  • ").append(displayCategory).append(": ").append(String.format("%.2f", entry.getValue())).append(" руб.\n");
            }
            report.append("\n");
        }

        if (!expenseByCategory.isEmpty()) {
            report.append("📉 Расходы по категориям:\n");
            for (java.util.Map.Entry<String, Double> entry : expenseByCategory.entrySet()) {
                String category = entry.getKey();
                String displayCategory = category.substring(0, 1).toUpperCase() + category.substring(1);
                report.append("  • ").append(displayCategory).append(": ").append(String.format("%.2f", entry.getValue())).append(" руб.\n");
            }
        }

        report.append("\nВсего операций: ").append(transactions.size());
        return report.toString();
    }
}