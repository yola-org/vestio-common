package com.vestio.common.event.payload;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Display data shared by listing-created and listing-published notifications. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ListingNotificationPayload(String listingTitle) implements NotificationPayload {
}
