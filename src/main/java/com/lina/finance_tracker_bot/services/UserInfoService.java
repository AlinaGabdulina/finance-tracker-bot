package com.lina.finance_tracker_bot.services;

import com.lina.finance_tracker_bot.modelSqlLite.User;
import com.lina.finance_tracker_bot.repository.UserRepository;
import com.lina.finance_tracker_bot.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с информацией о пользователях финансового трекера.
 * Обеспечивает получение и создание пользователей, а также работу с их категориями транзакций.
 */
@Service
public class UserInfoService {

    private final UserRepository userRepository; // Репозиторий для работы с пользователями
    private final TransactionRepository transactionRepository; // Репозиторий для работы с транзакциями

    /**
     * Конструктор сервиса.
     *
     * @param userRepository репозиторий пользователей
     * @param transactionRepository репозиторий транзакций
     */
    public UserInfoService(UserRepository userRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Получает существующего пользователя по ID чата или создаёт нового.
     *
     * @param chatId ID чата пользователя в Telegram
     * @param username имя пользователя в Telegram
     * @return существующий или вновь созданный объект пользователя
     */
    public User getOrCreateUser(Long chatId, String username) {
        Optional<User> existingUser = userRepository.findByChatId(chatId);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }
        User newUser = new User(chatId, username);
        return userRepository.save(newUser);
    }

    /**
     * Получает список уникальных категорий транзакций пользователя.
     * Категории возвращаются в нижнем регистре для унификации.
     *
     * @param chatId ID чата пользователя
     * @return список уникальных категорий транзакций пользователя
     */
    public List<String> getUserCategories(Long chatId) {
        return transactionRepository.findByUserChatId(chatId).stream()
                .map(transaction -> transaction.getCategory())
                .map(String::toLowerCase)
                .distinct()
                .toList();
    }
}
