package com.lina.finance_tracker_bot.repository;

import com.lina.finance_tracker_bot.modelSqlLite.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
/**
 * Репозиторий для работы с пользователями в базе данных.
 * Предоставляет методы для поиска пользователей по идентификатору чата.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByChatId(Long chatId);
}