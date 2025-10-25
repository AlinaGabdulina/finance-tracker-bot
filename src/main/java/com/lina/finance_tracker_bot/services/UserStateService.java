package com.lina.finance_tracker_bot.services;

import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Сервис централизованного хранения состояний пользователей и ожиданий.
 * - хранит "режимы" ввода (например "add_expense", "create_notification")
 * - хранит ожидания выбора номера для удаления категорий/уведомлений
 * - автоматически очищает флаги по TTL (по умолчанию 5 минут)
 */
@Service
public class UserStateService {

    public static final long DEFAULT_TTL_MS = 5 * 60 * 1000L; // 5 минут
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // состояния ввода: chatId -> state
    private final ConcurrentMap<Long, String> states = new ConcurrentHashMap<>();
    // время истечения состояния: chatId -> epochMillis
    private final ConcurrentMap<Long, Long> stateExpirations = new ConcurrentHashMap<>();

    // ожидание выбора категории для удаления
    private final Set<Long> awaitingCategoryDeletion = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<Long, Long> categoryDeletionExpirations = new ConcurrentHashMap<>();

    // ожидание выбора уведомления для удаления
    private final Set<Long> awaitingNotificationDeletion = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<Long, Long> notificationDeletionExpirations = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public UserStateService() {
        // проверка истёкших флагов каждые 10 секунд
        scheduler.scheduleAtFixedRate(this::cleanupExpired, 10, 10, TimeUnit.SECONDS);
    }

    /* ------------------ States (dialog inputs) ------------------ */

    public void setState(Long chatId, String state) {
        setState(chatId, state, DEFAULT_TTL_MS);
    }

    public void setState(Long chatId, String state, long ttlMs) {
        if (chatId == null || state == null) return;
        long expireAt = System.currentTimeMillis() + Math.max(0, ttlMs);
        states.put(chatId, state);
        stateExpirations.put(chatId, expireAt);
    }

    public String getState(Long chatId) {
        return states.get(chatId);
    }

    public boolean hasState(Long chatId) {
        return states.containsKey(chatId);
    }

    public void clearState(Long chatId) {
        states.remove(chatId);
        stateExpirations.remove(chatId);
    }

    /* ------------------ Category deletion awaiting ------------------ */

    public void setAwaitingCategoryDeletion(Long chatId) {
        setAwaitingCategoryDeletion(chatId, DEFAULT_TTL_MS);
    }

    public void setAwaitingCategoryDeletion(Long chatId, long ttlMs) {
        if (chatId == null) return;
        awaitingCategoryDeletion.add(chatId);
        categoryDeletionExpirations.put(chatId, System.currentTimeMillis() + Math.max(0, ttlMs));
    }

    public boolean isAwaitingCategoryDeletion(Long chatId) {
        return awaitingCategoryDeletion.contains(chatId);
    }

    public void clearAwaitingCategoryDeletion(Long chatId) {
        awaitingCategoryDeletion.remove(chatId);
        categoryDeletionExpirations.remove(chatId);
    }

    /* ------------------ Notification deletion awaiting ------------------ */

    public void setAwaitingNotificationDeletion(Long chatId) {
        setAwaitingNotificationDeletion(chatId, DEFAULT_TTL_MS);
    }

    public void setAwaitingNotificationDeletion(Long chatId, long ttlMs) {
        if (chatId == null) return;
        awaitingNotificationDeletion.add(chatId);
        notificationDeletionExpirations.put(chatId, System.currentTimeMillis() + Math.max(0, ttlMs));
    }

    public boolean isAwaitingNotificationDeletion(Long chatId) {
        return awaitingNotificationDeletion.contains(chatId);
    }

    public void clearAwaitingNotificationDeletion(Long chatId) {
        awaitingNotificationDeletion.remove(chatId);
        notificationDeletionExpirations.remove(chatId);
    }

    /* ------------------ Cleanup ------------------ */

    private void cleanupExpired() {
        long now = System.currentTimeMillis();

        // states
        for (Map.Entry<Long, Long> e : stateExpirations.entrySet()) {
            if (e.getValue() <= now) {
                Long chatId = e.getKey();
                states.remove(chatId);
                stateExpirations.remove(chatId);
            }
        }

        // category deletion
        for (Map.Entry<Long, Long> e : categoryDeletionExpirations.entrySet()) {
            if (e.getValue() <= now) {
                Long chatId = e.getKey();
                awaitingCategoryDeletion.remove(chatId);
                categoryDeletionExpirations.remove(chatId);
            }
        }

        // notification deletion
        for (Map.Entry<Long, Long> e : notificationDeletionExpirations.entrySet()) {
            if (e.getValue() <= now) {
                Long chatId = e.getKey();
                awaitingNotificationDeletion.remove(chatId);
                notificationDeletionExpirations.remove(chatId);
            }
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
