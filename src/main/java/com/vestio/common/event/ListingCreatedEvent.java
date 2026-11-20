package com.vestio.common.event;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Builder
@Getter
@Setter
public class ListingCreatedEvent extends BaseEvent{

    /** The id of the listing owner. */
    private UUID userId;
    private UUID listingId;
    private String listingTitle;

    @Override
    public String getEventType() {
        return "listing-created";
    }

}
