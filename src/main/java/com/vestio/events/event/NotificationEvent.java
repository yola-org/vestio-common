package com.vestio.events.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Getter;

import java.util.UUID;
import com.vestio.events.entity.Notification;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEvent extends BaseEvent{

    private Notification notification;

    // Use the persisted notification UUID as the realtime event ID.
    @Override
    public UUID getId() {
        return notification.getId();
    }

    @Override
    public String getEventType() {
        return "notification-event";
    }
}
