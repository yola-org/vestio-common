package com.vestio.common.entity.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum NotificationType {
    LISTING_CREATED("listing-created"),
    LISTING_PUBLISHED("listing-published"),
    LISTING_UPDATED("listing-updated"),
    LISTING_DELETED("listing-deleted"),
    OFFER_SENT("offer-sent"),
    OFFER_ACCEPTED("offer-accepted"),
    // Retained for notifications already stored by older consumers.
    OFFER_CREATED("offer-created"),
    OFFER_UPDATED("offer-updated"),
    OFFER_DELETED("offer-deleted");

    private final String value;

    NotificationType(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }
}
