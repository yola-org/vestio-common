package com.vestio.common.publisher;

import com.vestio.common.event.Event;
import com.vestio.common.event.EventType;
import java.time.Instant;
import com.vestio.common.entity.Notification;
import com.vestio.common.entity.enums.NotificationType;

import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppSyncEventsPublisherTest {

    private final HttpClient httpClient = mock(HttpClient.class);
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final URI endpoint = URI.create("https://example.appsync-api.ca-central-1.amazonaws.com/event");
    private final AppSyncEventsPublisher publisher = new AppSyncEventsPublisher(endpoint, "ca-central-1", mapper,
            StaticCredentialsProvider.create(AwsBasicCredentials.create("test-key", "test-secret")), httpClient);

    @ParameterizedTest
    @EnumSource(EventType.class)
    void signsAndPublishesToUserChannel(EventType namespace) {
        respond(200, "{\"successful\":[{\"identifier\":\"one\"}],\"failed\":[]}");
        UUID userId = UUID.randomUUID();
        publisher.dispatchEvent(new Event(UUID.randomUUID(), namespace, Map.of("message", "hello"), Instant.now()), userId).join();

        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).sendAsync(request.capture(), any(HttpResponse.BodyHandler.class));
        var sent = request.getValue();
        assertThat(sent.uri()).isEqualTo(endpoint);
        assertThat(sent.method()).isEqualTo("POST");
        assertThat(sent.headers().firstValue("Authorization").orElseThrow())
                .contains("AWS4-HMAC-SHA256", "/ca-central-1/appsync/aws4_request");
        assertThat(sent.headers().firstValue("X-Amz-Date")).isPresent();
        var body = mapper.readTree(readBody(sent));
        String expectedNamespace = namespace == EventType.CHAT ? "chat" : "notifications";
        assertThat(body.path("channel").asString()).isEqualTo("/" + expectedNamespace + "/" + userId);
        assertThat(body.path("events").size()).isEqualTo(1);
        assertThat(mapper.readTree(body.path("events").get(0).asString()).path("type").asString())
                .isEqualTo(namespace.getValue());
    }

    @Test
    void publishesNotificationPayloadToUserId() {
        respond(200, "{\"successful\":[{\"identifier\":\"one\"}],\"failed\":[]}");
        UUID userId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(UUID.randomUUID()).userId(userId)
                .type(NotificationType.LISTING_CREATED)
                .payload(Map.of("listingTitle", "A shirt")).build();

        publisher.dispatchNotificationEvent(notification).join();

        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).sendAsync(request.capture(), any(HttpResponse.BodyHandler.class));
        var body = mapper.readTree(readBody(request.getValue()));
        assertThat(body.path("channel").asString()).isEqualTo("/notifications/" + userId);
        var event = mapper.readTree(body.path("events").get(0).asString());
        assertThat(event.path("id").asString()).isEqualTo(notification.getId().toString());
        assertThat(event.path("type").asString()).isEqualTo("notification");
        var payload = event.path("payload");
        assertThat(payload.path("userId").asString()).isEqualTo(notification.getUserId().toString());
        assertThat(payload.path("type").asString()).isEqualTo("listing-created");
        assertThat(payload.path("payload").path("listingTitle").asString()).isEqualTo("A shirt");
    }

    @Test
    void propagatesHttpFailure() {
        respond(403, "{}");
        assertThatThrownBy(() -> publisher.dispatchEvent(new Event(UUID.randomUUID(), EventType.CHAT, Map.of(), Instant.now()), UUID.randomUUID()).join())
                .hasRootCauseMessage("AppSync publish failed with HTTP 403");
    }

    @Test
    void rejectsPartialFailure() {
        respond(200, "{\"successful\":[],\"failed\":[{\"identifier\":\"one\"}]}");
        assertThatThrownBy(() -> publisher.dispatchEvent(new Event(UUID.randomUUID(), EventType.CHAT, Map.of(), Instant.now()), UUID.randomUUID()).join())
                .hasRootCauseMessage("AppSync did not confirm event publication");
    }

    @SuppressWarnings("unchecked")
    private void respond(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
    }

    private String readBody(HttpRequest request) {
        var result = new StringBuilder();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            public void onNext(ByteBuffer buffer) { result.append(StandardCharsets.UTF_8.decode(buffer)); }
            public void onError(Throwable error) { throw new AssertionError(error); }
            public void onComplete() { }
        });
        return result.toString();
    }
}
