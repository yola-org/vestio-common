# Vestio Common

Shared plain Java entities, enums, events, utilities and AppSync Events publishing
for Vestio applications, including `vestio-backend` and `lambda-image-optimizer`.

**Artifact:** `com.vestio:vestio-common:0.1.0`
**Java namespace:** `com.vestio.common`
**Java:** 25

## Included APIs

| Package | Types |
| --- | --- |
| `com.vestio.common.entity` | `Notification`, `NotificationOutbox` |
| `com.vestio.common.entity.enums` | `NotificationType`, `ListingStatus`, `OfferStatus`, `Condition`, `ContactType` |
| `com.vestio.common.event` | `BaseEvent`, `NotificationEvent`, `ListingCreatedEvent` |
| `com.vestio.common.publisher` | `AppSyncEventsPublisher`, `RealtimeChannelsNamespace` |

The shared classes were extracted from the backend notification implementation.
This is a normal Java library: it does not start an application, connect to a
database, fetch credentials, run migrations, or register Spring beans on its own.
The artifact has no Spring dependencies, repositories, Boot starters or auto-configuration.
Applications keep their repositories, notification orchestration, transaction
boundaries, retry policy and database migrations. The shared entities retain
Jakarta Persistence/Hibernate mappings so both applications map the same schema.

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
exposes Jakarta Persistence, Jackson 3, and AWS credentials types;
Gradle/Maven metadata declares the corresponding dependencies. The AWS SDK BOM
keeps the consuming application's SDK components aligned at `2.59.2`.

## Consume from GitHub Packages

The publishing target is
`https://maven.pkg.github.com/yola-org/vestio-common`.
The repository/package must be created and published before remote resolution
works. Add this to the consuming application's Gradle build:

```groovy
repositories {
    mavenCentral()
    maven {
        name = 'VestioCommon'
        url = uri('https://maven.pkg.github.com/yola-org/vestio-common')
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
    implementation 'com.vestio:vestio-common:0.1.0'
}
```

GitHub requires authentication for Maven package downloads, including public
packages. Use a classic token with `read:packages` and repository access, or an
authorized workflow token. Supply credentials through environment variables or
untracked user-level `~/.gradle/gradle.properties`; never commit tokens.
See [GitHub's Gradle registry documentation](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry).

### Integrate the Java classes

Replace copied model/event/publisher classes with imports from this library.
Keep repository interfaces in the applications. Each application configures its
own entity discovery or persistence unit to include `com.vestio.common.entity`,
and keeps its existing Hibernate runtime and Liquibase-managed schema.
Do not map both the old copied entities and the shared entities to the same tables.

### Register the publisher

```java
import com.vestio.common.publisher.AppSyncEventsPublisher;
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

## CI and automatic releases

The workflow follows `yola-org/lambda-image-optimizer` and reuses these actions
from `yola-org/shared-workflows`:

- `reusable-release-gate.yml`, with `require-merged-pr: false`.
- `reusable-version.yml`, with `prefix: v`, `initial: 0.1.0`, and `tag: false`.
- `version-tag`, run after publishing succeeds.

Pull requests, pushes to `main`, and manual workflow dispatch run the normal
build, tests and publication-metadata verification. The dependency check also
rejects Spring dependencies in the library's runtime graph.

After a push or merge into `main`, the release gate allows the release jobs to run once
verification succeeds. CI calculates the next version from existing `v*` tags,
publishes the JAR, sources and Javadoc to GitHub Packages using
`-Pversion=<calculated-version>`, then creates and pushes the matching annotated
tag. No manual tag push or version-file commit is needed. Manual dispatch verifies
only. Active releases are not cancelled by subsequent pushes.

The initial release is `0.1.0` / `v0.1.0`. Subsequent versions follow the shared
calculator: `feat:` increases the minor version, breaking `feat!:` increases the
major version, and other commit titles increase the patch version. Squash-merge
PRs with conventional titles to carry that title into the main commit.

The repository's `gradle.properties` version is the local development default;
CI overrides it with the calculated release version. The publisher uses the
workflow's `GITHUB_TOKEN` with `packages: write`. Tag creation uses
`contents: write`. There is no committed publishing credential.

Published Maven versions are immutable. Direct pushes to `main` also publish
and tag after verification succeeds.

For local consumption before a remote publication:

```bash
./gradlew publishToMavenLocal
```

Then temporarily enable `mavenLocal()` in a consumer. Package-creation checks
used an isolated temporary Maven repository rather than the user's normal one.
