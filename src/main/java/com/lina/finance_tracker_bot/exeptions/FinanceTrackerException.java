package com.lina.finance_tracker_bot.exeptions;

/**
 * Исключение для ошибок финансового трекер-бота.
 */
public class FinanceTrackerException extends Exception {
    /**
     * @param message описание ошибки
     * @param cause причина исключения (может быть null)
     */
    public FinanceTrackerException(String message, Throwable cause) {
        super(message, cause);
    }
}
