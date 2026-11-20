package com.vestio.common.publisher;

public enum RealtimeChannelsNamespace {
    NOTIFICATIONS_NAMESPACE("notifications"),
    CHAT_NAMESPACE("chat");

    public final String channelName;

    RealtimeChannelsNamespace(String channelName) {
        this.channelName = channelName;
    }
}
