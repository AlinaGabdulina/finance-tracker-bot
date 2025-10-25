    package com.lina.finance_tracker_bot.repository;

    import com.lina.finance_tracker_bot.modelSqlLite.Transaction;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.Param;
    import java.time.LocalDateTime;
    import java.util.List;
    import java.util.Optional;
    /**
     * Репозиторий для работы с транзакциями в базе данных.
     * Предоставляет методы для поиска транзакций по пользователю и временным периодам.
     */
    public interface TransactionRepository extends JpaRepository<Transaction, Long> {
        List<Transaction> findByUserChatId(Long chatId);

        // Получить первую транзакцию пользователя (для определения даты начала)
        Optional<Transaction> findFirstByUserChatIdOrderByDateAsc(Long chatId);

        // Транзакции за определенный период
        @Query("SELECT t FROM Transaction t WHERE t.user.chatId = :chatId AND t.date BETWEEN :startDate AND :endDate")
        List<Transaction> findByUserAndDateRange(@Param("chatId") Long chatId,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

        // Транзакции за сегодня (с 00:00 до сейчас)
        @Query("SELECT t FROM Transaction t WHERE t.user.chatId = :chatId AND t.date >= :startOfDay")
        List<Transaction> findTodayTransactions(@Param("chatId") Long chatId,
                                                @Param("startOfDay") LocalDateTime startOfDay);
    }