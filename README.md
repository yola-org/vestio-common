# Vestio Events

Reusable Java types and AppSync Events publishing for `vestio-backend` and
`lambda-image-optimizer`.

**Artifact:** `com.vestio:vestio-events:0.1.0`
**Java namespace:** `com.vestio.events`
**Java:** 25

## Included APIs

| Package | Types |
| --- | --- |
| `com.vestio.events.entity` | `Notification`, `NotificationOutbox` |
| `com.vestio.events.entity.enums` | `NotificationType`, `ListingStatus`, `OfferStatus`, `Condition`, `ContactType` |
| `com.vestio.events.event` | `BaseEvent`, `NotificationEvent`, `NotificationCreated`, `ListingCreatedEvent` |
| `com.vestio.events.publisher` | `AppSyncEventsPublisher`, `RealtimeChannelsNamespace` |
| `com.vestio.events.repository` | `NotificationRepository` |

The shared classes were extracted from the backend notification implementation.
This is a normal Java library: it does not start an application, connect to a
database, fetch credentials, run migrations, or register Spring beans on its own.
Applications keep their notification orchestration, transaction boundaries,
retry policy and database migrations.

## Build and test

```bash
./gradlew build
```

The build creates the library JAR, sources JAR, and Javadoc JAR in `build/libs/`.
Tests verify the persisted entity mapping (including notifications with no
listing ID), the JSON event contract, IAM request signing, and publish failures.
They use an in-memory database and mocked HTTP; no AWS credentials are needed.

```bash
./gradlew generatePomFileForMavenJavaPublication
```

This writes Maven metadata to `build/publications/mavenJava/pom-default.xml`.
Hibernate is supplied by the consuming application. The library's public API
exposes Jakarta Persistence, Jackson 3, Spring Data JPA, and AWS credentials types;
Gradle/Maven metadata declares the corresponding dependencies. The AWS SDK BOM
keeps the consuming application's SDK components aligned at `2.59.2`.

## Consume from GitHub Packages

The publishing target is
`https://maven.pkg.github.com/yola-org/vestio-events`.
The repository/package must be created and published before remote resolution
works. Add this to the consuming application's Gradle build:

```groovy
repositories {
    mavenCentral()
    maven {
        name = 'VestioEvents'
        url = uri('https://maven.pkg.github.com/yola-org/vestio-events')
        content { includeGroup 'com.vestio' }
        credentials {
            username = providers.gradleProperty('gpr.user')
                    .orElse(providers.environmentVariable('GITHUB_ACTOR')).getOrNull()
            password = providers.gradleProperty('gpr.key')
                    .orElse(providers.environmentVariable('GITHUB_TOKEN')).getOrNull()
        }
    }
}
dependencies {
    implementation 'com.vestio:vestio-events:0.1.0'
}
```

GitHub requires authentication for Maven package downloads, including public
packages. Use a classic token with `read:packages` and repository access, or an
authorized workflow token. Supply credentials through environment variables or
untracked user-level `~/.gradle/gradle.properties`; never commit tokens.
See [GitHub's Gradle registry documentation](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry).

### Spring Boot entity and repository scanning

When migrating a consumer, remove its copied notification entities/repository and
change imports to this library. Ensure entity and repository scanning includes
the shared packages as well as the application's own packages:

```java
import com.vestio.events.entity.Notification;
import com.vestio.events.repository.NotificationRepository;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(basePackages = {"org.app.vestiobackend.entity", "com.vestio.events.entity"})
@EnableJpaRepositories(basePackages = {"org.app.vestiobackend.repository", "com.vestio.events.repository"})
```

Lambda uses `com.vestio.imageoptimizer` as its application package instead.
Keep the existing Hibernate runtime and Liquibase-managed schema. Do not scan
both the old copied entities and the shared entities, since they map the same
tables. Neither consumer has been changed as part of this package's creation.

### Register the publisher

```java
import com.vestio.events.publisher.AppSyncEventsPublisher;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

var publisher = new AppSyncEventsPublisher(
        URI.create(System.getenv("APPSYNC_EVENTS_HTTP_ENDPOINT")),
        System.getenv("APPSYNC_EVENTS_REGION"),
        JsonMapper.builder().build(),
        DefaultCredentialsProvider.builder().build(),
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());

// Register this as a singleton bean; close it when the application shuts down.
// Observe the returned future so failures reach the application's retry policy.
publisher.dispatchNotificationEvent(notification).join();
```

The endpoint must be an HTTPS AppSync Events `/event` URL. The publisher signs
requests with IAM, routes to `/<namespace>/<userId>`, and treats HTTP or per-event
publication failures as exceptional completion. Publisher calls do not provide
database transactions, an outbox, or delivery retries; each consumer owns those.

## Notification contract

`Notification` has `id`, `userId`, `type`, `params`, `readAt`, and `createdAt`.
The user ID is the application's canonical Cognito UUID. Listing IDs are optional
notification params; the outbox's listing correlation is nullable too.

`NotificationEvent` extends `BaseEvent` and uses the saved notification UUID as
its event UUID. Its event type stays `notification-event`. The test fixture in
`src/test/resources/contracts/notification-event.json` defines the canonical
shape, including a notification unrelated to a listing.

```json
{
  "id": "7e5560a7-9008-48b5-bb39-bfe64198e536",
  "eventType": "notification-event",
  "occurredAt": "2026-11-20T12:00:00Z",
  "notification": {
    "id": "7e5560a7-9008-48b5-bb39-bfe64198e536",
    "userId": "b2ab6e97-6c4c-47dc-87ed-2017fe8caf09",
    "type": "OFFER_CREATED",
    "params": {"offerId": "d4384a88-87aa-421a-99fe-3c5896076ada"},
    "readAt": null,
    "createdAt": "2026-11-20T12:00:00"
  }
}
```

## Publish a version

`.github/workflows/ci.yml` verifies branches and pull requests. A version tag
also runs the publishing job with `contents: read` and `packages: write`, using
the workflow's `GITHUB_TOKEN`; no publishing token needs to be committed.

1. Create the `yola-org/vestio-events` GitHub repository and push this source.
2. Set `version` in `gradle.properties` to the next semantic version.
3. Push a matching tag, for example `git tag v0.1.0` and `git push origin v0.1.0`.
4. Wait for the `publish` job, then verify the package version in GitHub Packages.

The tag must match the declared project version. Released Maven coordinates are
immutable; publish a new version for changes. Manual workflow dispatch verifies
only; publishing requires a version tag.

For local consumption before a remote publication:

```bash
./gradlew publishToMavenLocal
```

Then temporarily enable `mavenLocal()` in a consumer. The verification performed
during package creation used an isolated temporary Maven repository instead of
modifying the user's normal Maven repository.
