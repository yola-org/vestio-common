# Vestio Common

Plain Java entities, enums, event envelopes and IAM-signed AppSync publishing shared by Vestio applications. No Spring dependency or repositories are included.

Artifact: `com.vestio:vestio-common:0.4.0`. Java 25.

## Shared model

- `Notification`: persisted entity with `id`, `userId`, `type` (`NotificationType`), JSON `payload`, optional `listingId`, `readAt`, and `createdAt`.
- `NotificationType`: serializes as `listing-created`, `listing-published`, listing update/delete and offer create/update/delete types. JPA stores enum names.
- `Event`: JSON envelope with `id`, `type` (`EventType`), JSON map `payload`, and `occurredAt`.
- `EventType`: `notification` or `chat`. Notification payloads contain the saved notification; chat payloads may be defined later.
- `NotificationCreated`: internal plain Java application event for coordinating persistence and after-commit listeners.
- `AppSyncEventsPublisher`: reusable HTTP publisher. Applications provide endpoint, region, Jackson mapper, AWS credentials provider and HTTP client.

Applications keep their own notification repositories, transaction listeners and database migrations. There is no notification outbox. Publish after commit; persisted inbox notifications remain available when a live send fails.

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
