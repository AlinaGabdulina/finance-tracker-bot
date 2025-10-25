package com.lina.finance_tracker_bot.repository;

import com.lina.finance_tracker_bot.modelSqlLite.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
/**
 * Репозиторий для работы с уведомлениями в базе данных.
 * Предоставляет методы для поиска, фильтрации и удаления уведомлений.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Этот метод использоваться для отображения АКТИВНЫХ уведомлений
    @Query("SELECT n FROM Notification n WHERE n.user.chatId = :chatId AND n.notificationTime > :currentTime ORDER BY n.notificationTime ASC")
    List<Notification> findActiveByUserChatIdOrderByNotificationTimeAsc(@Param("chatId") Long chatId, @Param("currentTime") LocalTime currentTime);

    @Query("SELECT n FROM Notification n WHERE n.notificationTime = :currentTime")
    List<Notification> findNotificationsByTime(@Param("currentTime") LocalTime currentTime);

    @Query("SELECT n FROM Notification n WHERE n.user.chatId = :chatId ORDER BY n.createdAt DESC LIMIT :limit")
    List<Notification> findTopByUserChatIdOrderByCreatedAtDesc(@Param("chatId") Long chatId, @Param("limit") int limit);

    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.user.chatId = :chatId AND n.id NOT IN " +
            "(SELECT n2.id FROM Notification n2 WHERE n2.user.chatId = :chatId ORDER BY n2.createdAt DESC LIMIT :keepCount)")
    void deleteOldNotifications(@Param("chatId") Long chatId, @Param("keepCount") int keepCount);
}