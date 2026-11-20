package com.vestio.common.event.payload;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Payload for an OFFER_SENT notification addressed to the listing owner. */
public record OfferSentPayload(UUID offerId, BigDecimal amount, UUID senderId,
                               String senderName, String listingTitle) {
    public OfferSentPayload {
        Objects.requireNonNull(offerId, "Offer ID is required");
        Objects.requireNonNull(amount, "Offer amount is required");
        Objects.requireNonNull(senderId, "Sender ID is required");
        Objects.requireNonNull(senderName, "Sender name is required");
        Objects.requireNonNull(listingTitle, "Listing title is required");
        if (amount.signum() <= 0) throw new IllegalArgumentException("Offer amount must be positive");
    }

    public Map<String, Object> toMap() {
        return Map.of("offerId", offerId.toString(), "amount", amount,
                "senderId", senderId.toString(), "senderName", senderName, "listingTitle", listingTitle);
    }
}
