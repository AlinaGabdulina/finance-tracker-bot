package com.lina.finance_tracker_bot.modelSqlLite;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
/**
 * Модель уведомления в системе финансового трекера.
 * Представляет напоминание/оповещение для пользователя.
 */
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalTime notificationTime;
    private String message;
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    public Notification() {}

    public Notification(LocalTime notificationTime, String message, User user) {
        this.notificationTime = notificationTime;
        this.message = message;
        this.user = user;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalTime getNotificationTime() { return notificationTime; }
    public void setNotificationTime(LocalTime notificationTime) { this.notificationTime = notificationTime; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}