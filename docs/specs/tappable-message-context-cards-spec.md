# Spec: Tappable Message Context Cards

Status: Implemented on 2026-07-23.

## Objective

Render appointment and order context links as distinct, accessible cards inside
chat message bubbles. Tapping a card opens the existing appointment or order
detail destination. Ordinary text messages and attachments must retain their
current behavior.

The Laravel API already accepts and returns additive message context links:

```json
{
  "contexts": [
    { "type": "appointment", "id": 7 }
  ]
}
```

No backend or database change is required.

## Tech Stack

- Kotlin, Kotlinx Serialization, Retrofit
- MVVM + Clean Architecture
- Jetpack Compose + Material 3
- Type-safe Navigation Compose routes
- JUnit 5, MockK, and Compose UI tests

## Commands

- Format: `.\gradlew ktlintFormat`
- Formatting check: `.\gradlew ktlintCheck`
- Unit tests: `.\gradlew testDebugUnitTest`
- Instrumented context-card test:
  `.\gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.eyecare.app.presentation.messaging.components.MessageBubbleTest`
- Build: `.\gradlew assembleDebug`

## Project Structure

- `data/remote/dto/MessageDtos.kt`: nullable-safe/additive wire contract
- `data/repository/ChatRepositoryImpl.kt`: DTO-to-domain mapping
- `domain/model/Message.kt`: typed appointment/order/unsupported contexts
- `presentation/messaging/components/MessageBubble.kt`: context-card rendering
- `presentation/messaging/ChatScreen.kt`: navigation callback forwarding
- `presentation/navigation/NavGraph.kt`: existing detail-route integration
- `app/src/test`: DTO and repository-boundary unit coverage
- `app/src/androidTest`: rendering and tap behavior

## Code Style

Use a sealed interface so presentation code cannot confuse appointment and
order IDs:

```kotlin
sealed interface MessageContext {
    val id: Int

    data class Appointment(override val id: Int) : MessageContext
    data class Order(override val id: Int) : MessageContext
    data class Unsupported(val type: String, override val id: Int) : MessageContext
}
```

Compose cards use existing Material color and typography tokens, 8/12/16dp
spacing, a visible icon and text label, and a minimum 48dp tappable height.

## Testing Strategy

- DTO unit test proves `contexts` decodes from the documented API response and
  defaults to an empty list when absent.
- Mapping unit test proves appointment/order types become distinct domain
  variants and unknown types do not crash.
- Compose test proves each supported card is visible and invokes the correct ID
  callback when tapped.
- Full unit suite, formatting check, debug build, and connected-device manual
  verification guard against regressions.

## Boundaries

- Always:
  - Map DTO contexts to domain contexts at the repository boundary.
  - Preserve message body, attachments, timestamp, sender alignment, and colors.
  - Use existing type-safe `AppointmentDetail` and `OrderDetail` routes.
  - Keep unsupported context types non-interactive and non-crashing.
- Ask first:
  - Add backend response fields or database columns.
  - Add product-context navigation.
  - Add dependencies or change the global theme.
- Never:
  - Parse IDs from message body text or emoji.
  - Treat the whole message bubble as the navigation target.
  - Expose authorization data in logs or UI.

## Success Criteria

- Appointment context cards open `AppointmentDetail(context.id)`.
- Order context cards open `OrderDetail(context.id)`.
- Messages without contexts render exactly as before.
- Multiple supported contexts render as separate cards.
- Unknown context types do not crash and do not create misleading navigation.
- Cards expose meaningful accessibility labels and at least a 48dp touch target.
- Unit tests, Compose test, formatting check, and `assembleDebug` pass.

## Implementation Tasks

- [x] Add failing DTO/domain mapping tests for inbound contexts.
- [x] Add the additive DTO field, typed domain variants, and repository mapping.
- [x] Add failing Compose rendering/tap tests.
- [x] Implement the message context card and callback plumbing.
- [x] Connect callbacks to type-safe routes in `NavGraph`.
- [x] Run full verification and validate on the connected phone.

## Final Verification

- `.\gradlew ktlintCheck testDebugUnitTest compileDebugAndroidTestKotlin assembleDebug`
  passed.
- The focused Compose test APK compiled. Execution through
  `connectedDebugAndroidTest` was blocked by the physical phone with
  `INSTALL_FAILED_USER_RESTRICTED`; no code/test assertion failed.
- The rebuilt APK was installed normally. Existing appointment and order
  context messages rendered their cards with `Open appointment 5` and
  `Open order 1` accessibility labels.
- Live taps opened appointment `APT-2026-000005` and order `ORD-DEMO-0001`.
- The mapper accepts the backend's current `App\Models\Appointment` and
  `App\Models\Order` response values as well as normalized request aliases.

## Open Questions

- Product contexts remain deliberately unsupported in this iteration even
  though the backend accepts them; adding a product card is a separate additive
  enhancement.
