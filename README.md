# Vestio Common

Plain Java entities, enums, event envelopes and IAM-signed AppSync publishing shared by Vestio applications. No Spring dependency or repositories are included.

Artifact: `com.vestio:vestio-common:0.9.0`. Java 25.

## Shared model

- `Notification`: persisted entity with `id`, `userId`, `type` (`NotificationType`), JSON `payload`, optional `listingId`, `readAt`, and `createdAt`.
- `NotificationType`: serializes as `listing-created`, `listing-published`, listing update/delete and offer create/update/delete types. JPA stores enum names.
- `Event<T>`: generic JSON envelope with `id`, `type` (`EventType`), typed `payload`, and `occurredAt`.
- `ChatEvent`: extends `Event<ChatEvent.Payload>`; its nested static payload record carries the saved message fields.
- `EventType`: `notification` or `chat`. Notification payloads contain the saved notification; chat events carry the concrete saved-message payload.
- `AppSyncEventsPublisher`: reusable HTTP publisher. Applications provide endpoint, region, Jackson mapper, AWS credentials provider and HTTP client.

Applications keep their own notification repositories, services and database migrations. There is no notification outbox. Services save and flush notifications, then call the publisher directly and await the result. Send failures propagate so the surrounding transaction can roll back.

```java
publisher.dispatchNotificationEvent(savedNotification).join();
// Other event payloads use the same envelope:
publisher.dispatchEvent(event, userId).join();
```

Notification events route to `/notifications/<userId>`; chat events route to `/chat/<userId>`. The endpoint must be an HTTPS AppSync Events `/event` URL. IAM signing and response validation are handled by the publisher. Notification event IDs match persisted notification IDs.

## Build and release

Run `./gradlew build generatePomFileForMavenJavaPublication`. Tests cover JSON contracts, optional listing IDs, persistence, request signing and publication failures. The build rejects Spring runtime dependencies.

Pushes and merges to `main` run verification, calculate the next semantic version, publish JAR/sources/Javadoc to GitHub Packages and create the matching `v*` tag after successful publication. Pull requests run verification only. CI supplies `GITHUB_TOKEN` with package write access. Maven versions are immutable.

Consume from `https://maven.pkg.github.com/yola-org/vestio-common` using `gpr.user` and `gpr.key` in user-level Gradle properties, or `GITHUB_ACTOR` and `GITHUB_TOKEN`. Download tokens need `read:packages` and package access.

## Offer notifications

`NotificationType.OFFER_SENT` serializes as `offer-sent`. `OfferSentPayload`
defines the map keys `offerId`, `amount` (numeric), `senderId`, `senderName` and
`listingTitle`. The notification recipient is the listing owner; `listingId`
remains on the notification entity. The outer event type stays `notification`.
Older offer enum values are retained so existing inbox rows remain readable.
The canonical wire fixture is `contracts/offer-sent-event.json`.

## Typed notification payloads

Every payload class implements `NotificationPayload`. `OfferSentPayload` and
`ListingNotificationPayload` are provided by common. `payload.toMap(objectMapper)`
uses the application's configured Jackson mapper; payload classes do not maintain
manual field maps or create their own mappers. Consumer notification services
accept `NotificationPayload`, while the JPA entity and wire envelope retain JSON
maps. Optional fields, annotations and nested objects use normal Jackson behavior.
`NotificationCreated` and internal dispatchers are removed; consumers call the
shared AppSync publisher directly after saving and flushing their notification.

## Timestamp contract

All notification and event timestamps use `Instant` and ISO 8601 UTC strings ending in `Z`. Consumers must migrate timestamp columns to `TIMESTAMP WITH TIME ZONE` before using this version. This changes the Java notification timestamp getters, setters and builder arguments from `LocalDateTime` to `Instant`. Calendar dates remain date-only.

## Offer acceptance

`NotificationType.OFFER_ACCEPTED` serializes as `offer-accepted`. `OfferAcceptedPayload` contains `offerId`, `chatId`, numeric `amount`, `currency`, `ownerName`, `buyerName` and `listingTitle`. The notification is addressed to the buyer; the backend uses the same payload for the initial chat system message.

## Typed chat events

`ChatEvent` uses `type: "chat"` and a concrete `ChatEvent.Payload` record with `id`, `chatId`, nullable `senderId`, nullable `content`, `system`, nullable `systemType`, nullable JSON `params`, and UTC `createdAt`. These keys match the backend `MessageDto`. The event ID is the persisted message ID. `new ChatEvent(payload)` supplies the envelope timestamp; its JSON constructor also validates the type and matching IDs.

Publish through `publisher.dispatchEvent(chatEvent, userId)` using the existing `/chat/<userId>` channel. Deserialize a chat event with `ChatEvent.class` to retain the concrete payload type. Notification factories and their JSON shape remain compatible. Version 0.7.0 changes `Event` from a record to a generic base class; consumers must rebuild, use `Event<?>` for unknown payload types, and `new Event<>(...)` for generic construction. The notification factory still copies its map payload defensively.

## Rejected listing content

`ListingStatus.REJECTED_CONTENT` identifies listings refused by image moderation.
The image Lambda persists this terminal result without retrying confirmed violations.
The backend includes these listings only in their owner's lists and Pending feed;
public browsing and listing details continue to require `AVAILABLE`.
The frontend displays a refusal badge and a placeholder without requesting listing photos.

This status is introduced in `0.9.0` (latest preceding published tag: `v0.8.0`).
Publish common first, then deploy consumers that understand the status before the
Lambda starts writing it. AWS service and database errors remain retryable.
