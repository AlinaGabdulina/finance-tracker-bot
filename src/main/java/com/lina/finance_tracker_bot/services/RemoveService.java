package com.lina.finance_tracker_bot.services;

import com.lina.finance_tracker_bot.modelSqlLite.Transaction;
import com.lina.finance_tracker_bot.modelSqlLite.User;
import com.lina.finance_tracker_bot.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис для управления удалением данных в финансовом трекере:
 * - полной очистки истории транзакций;
 * - удаления отдельных категорий транзакций.
 */
@Service
public class RemoveService {

    private final TransactionRepository transactionRepository; // Репозиторий для работы с транзакциями
    private final UserInfoService userInfoService; // Сервис для работы с информацией о пользователях
    private final UserStateService userStateService; // Сервис для управления состояниями пользователей

    /**
     * Конструктор сервиса.
     *
     * @param transactionRepository репозиторий транзакций
     * @param userInfoService сервис получения/создания пользователей
     * @param userStateService сервис управления состояниями пользователей
     */
    public RemoveService(TransactionRepository transactionRepository,
                         UserInfoService userInfoService,
                         UserStateService userStateService) {
        this.transactionRepository = transactionRepository;
        this.userInfoService = userInfoService;
        this.userStateService = userStateService;
    }

    /**
     * Очищает всю историю транзакций пользователя.
     *
     * @param chatId ID чата пользователя
     * @param username имя пользователя
     * @return сообщение о результате операции
     */
    public String clearCommand(Long chatId, String username) {
        try {
            User user = userInfoService.getOrCreateUser(chatId, username);
            List<Transaction> userTransactions = transactionRepository.findByUserChatId(chatId);

            if (userTransactions.isEmpty()) {
                return "📭 История уже пуста";
            }

            transactionRepository.deleteAll(userTransactions);
            return "🗑️ История очищена! Удалено " + userTransactions.size() + " записей";

        } catch (Exception e) {
            return "❌ Ошибка очистки: " + e.getMessage();
        }
    }

    /**
     * Подготавливает список категорий для удаления и активирует ожидание выбора категории.
     *
     * @param chatId ID чата пользователя
     * @param username имя пользователя
     * @return сообщение со списком категорий и инструкцией
     */
    public String deleteCommand(Long chatId, String username) {
        try {
            User user = userInfoService.getOrCreateUser(chatId, username);
            List<String> categories = getUserCategories(chatId);

            if (categories.isEmpty()) {
                return "📝 У вас пока нет категорий для удаления";
            }

            StringBuilder categoriesList = new StringBuilder("🗑️ Выберите категорию для удаления:\n\n");
            for (int i = 0; i < categories.size(); i++) {
                String category = categories.get(i);
                String formattedCategory = category.substring(0, 1).toUpperCase() + category.substring(1);
                categoriesList.append((i + 1)).append(". ").append(formattedCategory).append("\n");
            }

            categoriesList.append("\n💡 Ответьте номером категории для удаления");
            userStateService.setAwaitingCategoryDeletion(chatId); // устанавливаем ожидание выбора

            return categoriesList.toString();

        } catch (Exception e) {
            return "❌ Ошибка: " + e.getMessage();
        }
    }

    /**
     * Обрабатывает выбор категории для удаления и выполняет удаление транзакций по выбранной категории.
     *
     * @param chatId ID чата пользователя
     * @param categoryNumber номер выбранной категории
     * @param username имя пользователя
     * @return сообщение о результате удаления
     */
    public String categorySelection(Long chatId, int categoryNumber, String username) {
        try {
            if (!userStateService.isAwaitingCategoryDeletion(chatId)) {
                return "❌ Неверный номер категории";
            }

            userStateService.clearAwaitingCategoryDeletion(chatId); // сбрасываем флаг ожидания

            User user = userInfoService.getOrCreateUser(chatId, username);
            List<String> categories = getUserCategories(chatId);

            if (categoryNumber < 1 || categoryNumber > categories.size()) {
                return "❌ Неверный номер категории";
            }

            String selectedCategory = categories.get(categoryNumber - 1);
            List<Transaction> transactionsToDelete = transactionRepository.findByUserChatId(chatId).stream()
                    .filter(t -> t.getCategory().equalsIgnoreCase(selectedCategory))
                    .toList();

            if (!transactionsToDelete.isEmpty()) {
                transactionRepository.deleteAll(transactionsToDelete);
            }

            String formattedCategory = selectedCategory.substring(0, 1).toUpperCase() + selectedCategory.substring(1);
            String result = "✅ Категория \"" + formattedCategory + "\" удалена!\nУдалено записей: " + transactionsToDelete.size();

            List<String> updatedCategories = getUserCategories(chatId);
            if (updatedCategories.isEmpty()) {
                result += "\n📝 Теперь у вас нет категорий";
            } else {
                String categoriesMessage = formatCategories(updatedCategories);
                result += "\n📁 Обновленные категории:\n" + categoriesMessage;
            }

            return result;

        } catch (Exception e) {
            userStateService.clearAwaitingCategoryDeletion(chatId);
            return "❌ Ошибка удаления: " + e.getMessage();
        }
    }

    /**
     * Получает список уникальных категорий транзакций пользователя.
     *
     * @param chatId ID чата пользователя
     * @return список категорий в нижнем регистре
     */
    private List<String> getUserCategories(Long chatId) {
        return transactionRepository.findByUserChatId(chatId).stream()
                .map(Transaction::getCategory)
                .map(String::toLowerCase)
                .distinct()
                .toList();
    }

    /**
     * Формирует отформатированный список категорий для вывода пользователю.
     *
     * @param categories список категорий
     * @return строка с пронумерованным списком категорий
     */
    private String formatCategories(List<String> categories) {
        if (categories.isEmpty()) {
            return "📝 У вас пока нет сохраненных категорий";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            String formattedCategory = category.substring(0, 1).toUpperCase() + category.substring(1);
            sb.append((i + 1)).append(". ").append(formattedCategory).append("\n");
        }
        return sb.toString();
    }
}
