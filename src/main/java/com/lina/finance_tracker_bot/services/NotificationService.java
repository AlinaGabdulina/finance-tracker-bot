package com.lina.finance_tracker_bot.services;

import com.lina.finance_tracker_bot.bot.MessageSender;
import com.lina.finance_tracker_bot.modelSqlLite.Notification;
import com.lina.finance_tracker_bot.modelSqlLite.User;
import com.lina.finance_tracker_bot.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Сервис для управления уведомлениями пользователей
 */
@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserInfoService userInfoService;
    private final MessageSender messageSender;
    private final UserStateService userStateService;

    public NotificationService(NotificationRepository notificationRepository,
                               UserInfoService userInfoService,
                               MessageSender messageSender,
                               UserStateService userStateService) {
        this.notificationRepository = notificationRepository;
        this.userInfoService = userInfoService;
        this.messageSender = messageSender;
        this.userStateService = userStateService;
    }

    /**
     * Показывает активные уведомления пользователя
     * @param chatId ID чата пользователя
     * @return форматированный список уведомлений или сообщение об их отсутствии
     */
    public String showUserNotifications(Long chatId) {
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        List<Notification> notifications = notificationRepository
                .findActiveByUserChatIdOrderByNotificationTimeAsc(chatId, now);

        if (notifications.isEmpty()) {
            return "📋 У вас нет активных уведомлений\n\n" +
                    "💡 Чтобы создать уведомление через меню, используйте кнопку \"Создать уведомление\".";
        }

        StringBuilder sb = new StringBuilder("📋 Ваши активные уведомления:\n\n");
        for (int i = 0; i < notifications.size(); i++) {
            Notification n = notifications.get(i);
            sb.append(i + 1).append(". ⏰ ")
                    .append(n.getNotificationTime().format(UserStateService.TIME_FORMATTER))
                    .append(" - ").append(n.getMessage()).append("\n");
        }

        sb.append("\n💡 Всего активных уведомлений: ").append(notifications.size());
        sb.append("\n🗑️ Для удаления используйте кнопку \"Удалить уведомление\"");
        return sb.toString();
    }

    /**
     * Создает новое уведомление для пользователя
     * @param chatId ID чата пользователя
     * @param messageText текст команды в формате "/notify HH:mm Текст"
     * @param username имя пользователя
     * @return результат создания уведомления
     */
    public String handleNotifyCommand(Long chatId, String messageText, String username) {
        try {
            User user = userInfoService.getOrCreateUser(chatId, username);

            String[] parts = messageText.trim().split("\\s+", 3);
            if (parts.length < 3) {
                return showUserNotifications(chatId);
            }

            LocalTime time;
            try {
                time = LocalTime.parse(parts[1], UserStateService.TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                return "❌ Неверный формат времени. Используйте: HH:mm (например: 20:30)";
            }

            LocalTime now = LocalTime.now();
            if (time.isBefore(now)) {
                return "❌ Нельзя установить уведомление на прошедшее время!\n" +
                        "⏰ Сейчас: " + now.format(UserStateService.TIME_FORMATTER) +
                        "\n📅 Вы указали: " + time.format(UserStateService.TIME_FORMATTER);
            }

            String notificationMessage = parts[2];
            Notification notification = new Notification(time, notificationMessage, user);
            notificationRepository.save(notification);

            return "✅ Уведомление создано!\n" +
                    "⏰ Время: " + time.format(UserStateService.TIME_FORMATTER) + "\n" +
                    "📝 Текст: " + notificationMessage + "\n\n" +
                    showUserNotifications(chatId);

        } catch (Exception e) {
            return "❌ Ошибка создания уведомления: " + e.getMessage();
        }
    }

    /**
     * Подготавливает список уведомлений для удаления
     * @param chatId ID чата пользователя
     * @param username имя пользователя
     * @return форматированный список уведомлений для удаления
     */
    public String handleDeleteNotificationCommand(Long chatId, String username) {
        try {
            LocalTime now = LocalTime.now().withSecond(0).withNano(0);
            List<Notification> notifications = notificationRepository
                    .findActiveByUserChatIdOrderByNotificationTimeAsc(chatId, now);

            if (notifications.isEmpty()) {
                return "📋 У вас нет уведомлений для удаления";
            }

            StringBuilder notificationsList = new StringBuilder("🗑️ Выберите уведомление для удаления:\n\n");
            for (int i = 0; i < notifications.size(); i++) {
                Notification n = notifications.get(i);
                notificationsList.append((i + 1))
                        .append(". ⏰ ")
                        .append(n.getNotificationTime().format(UserStateService.TIME_FORMATTER))
                        .append(" - ")
                        .append(n.getMessage())
                        .append("\n");
            }

            notificationsList.append("\n💡 Ответьте номером уведомления для удаления");
            userStateService.setAwaitingNotificationDeletion(chatId);

            return notificationsList.toString();

        } catch (Exception e) {
            return "❌ Ошибка: " + e.getMessage();
        }
    }

    /**
     * Удаляет выбранное пользователем уведомление
     * @param chatId ID чата пользователя
     * @param notificationNumber номер уведомления для удаления
     * @param username имя пользователя
     * @return результат удаления уведомления
     */
    public String handleNotificationSelection(Long chatId, int notificationNumber, String username) {
        try {
            if (!userStateService.isAwaitingNotificationDeletion(chatId)) {
                return "❌ Неверный номер уведомления";
            }

            userStateService.clearAwaitingNotificationDeletion(chatId);

            LocalTime now = LocalTime.now().withSecond(0).withNano(0);
            List<Notification> notifications = notificationRepository
                    .findActiveByUserChatIdOrderByNotificationTimeAsc(chatId, now);

            if (notificationNumber < 1 || notificationNumber > notifications.size()) {
                return "❌ Неверный номер уведомления";
            }

            Notification selectedNotification = notifications.get(notificationNumber - 1);
            notificationRepository.delete(selectedNotification);

            String result = "✅ Уведомление удалено!\n" +
                    "⏰ Было: " + selectedNotification.getNotificationTime().format(UserStateService.TIME_FORMATTER) +
                    " - " + selectedNotification.getMessage();

            List<Notification> updatedNotifications = notificationRepository
                    .findActiveByUserChatIdOrderByNotificationTimeAsc(chatId, LocalTime.now().withSecond(0).withNano(0));

            if (updatedNotifications.isEmpty()) {
                result += "\n📋 Теперь у вас нет уведомлений";
            } else {
                result += "\n\n" + showUserNotifications(chatId);
            }

            return result;

        } catch (Exception e) {
            userStateService.clearAwaitingNotificationDeletion(chatId);
            return "❌ Ошибка удаления: " + e.getMessage();
        }
    }

    /**
     * Проверяет и отправляет уведомления, время которых наступило
     * Вызывается автоматически каждую минуту
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkAndSendNotifications() {
        try {
            LocalTime currentTime = LocalTime.now().withSecond(0).withNano(0);
            List<Notification> notificationsToSend = notificationRepository.findNotificationsByTime(currentTime);

            for (Notification notification : notificationsToSend) {
                String message = "🔔 Напоминание (" + notification.getNotificationTime().format(UserStateService.TIME_FORMATTER) + "):\n" +
                        notification.getMessage();

                messageSender.sendTextWithTtl(
                        notification.getUser().getChatId(),
                        message,
                        MessageSender.REMINDER_TTL
                );

                notificationRepository.delete(notification);
            }

        } catch (Exception e) {
            System.err.println("Ошибка проверки или отправки уведомлений: " + e.getMessage());
        }
    }
}