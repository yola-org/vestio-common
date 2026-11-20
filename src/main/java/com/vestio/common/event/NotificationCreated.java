package com.vestio.common.event;

import com.vestio.common.entity.Notification;

/**
 * Signals that a notification was persisted in the current transaction.
 * Consumers own transaction handling, outbox persistence and delivery timing.
 */
public record NotificationCreated(Notification notification) {
}
