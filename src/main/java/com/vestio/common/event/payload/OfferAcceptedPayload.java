package com.vestio.common.event.payload;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Offer acceptance details shared by the buyer notification and initial chat message. */
public record OfferAcceptedPayload(UUID offerId, UUID chatId, BigDecimal amount, String currency,
                                   String ownerName, String buyerName, String listingTitle) implements NotificationPayload {
    public OfferAcceptedPayload {
        Objects.requireNonNull(offerId, "Offer ID is required");
        Objects.requireNonNull(chatId, "Chat ID is required");
        Objects.requireNonNull(amount, "Offer amount is required");
        Objects.requireNonNull(currency, "Currency is required");
        Objects.requireNonNull(ownerName, "Owner name is required");
        Objects.requireNonNull(buyerName, "Buyer name is required");
        Objects.requireNonNull(listingTitle, "Listing title is required");
        if (amount.signum() <= 0) throw new IllegalArgumentException("Offer amount must be positive");
        if (!currency.matches("[A-Z]{3}")) throw new IllegalArgumentException("Currency must be a three-letter uppercase code");
    }
}
