package com.vestio.common.publisher;

import com.vestio.common.entity.Notification;
import com.vestio.common.event.Event;
import com.vestio.common.event.EventType;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AppSyncEventsPublisher implements AutoCloseable {

    private final URI endpoint;
    private final String region;
    private final ObjectMapper objectMapper;
    private final AwsCredentialsProvider credentialsProvider;
    private final HttpClient httpClient;
    private final AwsV4HttpSigner signer = AwsV4HttpSigner.create();

    public AppSyncEventsPublisher(URI endpoint, String region, ObjectMapper objectMapper,
                                  AwsCredentialsProvider credentialsProvider, HttpClient httpClient) {
        if (!"https".equalsIgnoreCase(endpoint.getScheme()) || endpoint.getHost() == null
                || !"/event".equals(endpoint.getPath()) || endpoint.getQuery() != null
                || endpoint.getFragment() != null || endpoint.getUserInfo() != null) {
            throw new IllegalArgumentException("AppSync endpoint must be an HTTPS /event URL");
        }
        this.endpoint = endpoint;
        this.region = Objects.requireNonNull(region);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.credentialsProvider = Objects.requireNonNull(credentialsProvider);
        this.httpClient = Objects.requireNonNull(httpClient);
    }

    @Override
    public void close() {
        if (credentialsProvider instanceof SdkAutoCloseable closeable) {
            closeable.close();
        }
    }

    /** Publishes to the user ID shared by users.id and the authenticated subscription channel. */
    public CompletableFuture<Void> dispatchEvent(Event event, UUID userId) {
        try {
            Objects.requireNonNull(event);
            Objects.requireNonNull(userId);
            String body = objectMapper.writeValueAsString(Map.of(
                    "channel", "/" + (event.type() == EventType.NOTIFICATION ? "notifications" : "chat") + "/" + userId,
                    "events", List.of(objectMapper.writeValueAsString(event))));
            var request = SdkHttpRequest.builder()
                    .uri(endpoint).method(SdkHttpMethod.POST)
                    .putHeader("Content-Type", "application/json").build();
            var signed = signer.sign(r -> r
                    .identity(credentialsProvider.resolveCredentials())
                    .request(request).payload(ContentStreamProvider.fromUtf8String(body))
                    .putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "appsync")
                    .putProperty(AwsV4HttpSigner.REGION_NAME, region));

            var builder = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            // The JDK sets these restricted headers from the URI and body itself.
            signed.request().headers().forEach((name, values) -> {
                if (!name.equalsIgnoreCase("host") && !name.equalsIgnoreCase("content-length")) {
                    values.forEach(value -> builder.header(name, value));
                }
            });
            return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() != 200) {
                            throw new IllegalStateException("AppSync publish failed with HTTP " + response.statusCode());
                        }
                        var result = objectMapper.readTree(response.body());
                        if (result == null || !result.path("failed").isArray()
                                || !result.path("failed").isEmpty()
                                || !result.path("successful").isArray()
                                || result.path("successful").size() != 1) {
                            throw new IllegalStateException("AppSync did not confirm event publication");
                        }
                    });
        } catch (Exception exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    public CompletableFuture<Void> dispatchNotificationEvent(Notification notification) {
        return dispatchEvent(Event.notification(notification, objectMapper), notification.getUserId());
    }
}
