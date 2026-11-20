package com.vestio.events.event;

import com.vestio.events.entity.Notification;

/** Internal event; the dispatcher publishes it only after its transaction commits. */
public record NotificationCreated(Notification notification) {
}
