# Douyin live-play acceptance checklist

This checklist distinguishes local logic checks from real platform acceptance. A green local check does **not** prove that a Douyin live room is connected.

## 1. Local Unity scene checks

- [ ] Import the official LiveOpenSDK Unity package and its sample code from the version approved for the app.
- [ ] Confirm the project compiles with the imported SDK. The adapter currently targets the sample API names `SampleLiveOpenSdkManager`, `SampleDyCloudManager`, `OnDyCloudMessage`, and `DyCloudSocketMessage`; verify them against the exact package version.
- [ ] Add an Animator with triggers `Like`, `Comment`, `Gift`, `BigGift`, `Dance`, `Heart`, `Wave`, `Hug`, and `Cheer`.
- [ ] Wire `DouyinLivePlayBootstrap`, `DouyinCloudMessageAdapter`, and `PetLiveReactionController` in the scene. Assign the optional bubble and energy UI if present.
- [ ] Use a dedicated development app ID and cloud environment. Never place app secrets or access tokens in Unity assets or source control.

## 2. Logic test cases

These cases should be exercised with a local test harness or controlled SDK test messages, not by fabricating real platform events in production.

| Case | Input | Expected result |
|---|---|---|
| Like | `live_like`, unique ID | Like trigger, bubble, energy increases |
| Comment command | `live_comment`, text contains `比心` | Heart trigger and safe fixed response |
| Arbitrary comment | `live_comment`, ordinary text | Comment trigger; arbitrary text is not echoed |
| Gift | `live_gift`, count 1 | Gift trigger and energy increases |
| Large gift | `live_gift`, count >= 10 | BigGift trigger |
| Duplicate | Same event ID twice | Second copy ignored |
| Unsupported type | Unknown type | Not enqueued |
| Platform test payload | Test flag true | Not enqueued |
| Queue overload | More events than capacity | Queue remains bounded; noisy likes may be dropped |
| Energy goal | Events collectively reach configured goal | Cheer trigger and energy resets |
| Missing Animator trigger | Trigger not configured | Warning logged; app does not crash |
| Shutdown | Stop while connected | Adapter unsubscribes, finish endpoint is attempted, socket and SDK are closed |

## 3. Cloud function checks

- [ ] Deploy `cloud/start_game.ts`, `cloud/live_data_callback.ts`, `cloud/websocket_callback.ts`, and `cloud/finish_game.ts` to the same Douyin Cloud environment and service.
- [ ] Confirm `start_game` starts the three required event tasks and rolls back successfully started tasks if a later task fails.
- [ ] Send a platform-authorized test callback and verify headers, message IDs, payload shape, and gateway response in server logs.
- [ ] Confirm each event is forwarded separately with its own message ID and serialized event payload.
- [ ] Verify callback retries do not create duplicate visible reactions. The Unity controller deduplicates event IDs in a bounded in-memory window; it is not durable across app restarts.
- [ ] Call `finish_game` and verify each task stops. Investigate partial failures rather than treating any HTTP 200 as proof of success.

## 4. Real-room acceptance criteria

Only mark the integration **LIVE VERIFIED** after all of the following are true:

1. The Douyin open platform app and live mini-game category have the required permissions/admission.
2. Cloud environment/service IDs and callback routing are configured in the platform console.
3. A real authorized live room delivers a like, comment, and gift through the official event path.
4. Unity logs show WebSocket open, incoming message IDs/types, the corresponding animation/bubble, and successful fulfillment ACK after the reaction is applied.
5. Repeated events, reconnects, shutdown, and task cleanup are checked.
6. Platform console/log evidence is saved with timestamps.

Until then, use status **CODE PREPARED / NOT LIVE VERIFIED**. A successful cloud-function HTTP response alone is not proof of a live WebSocket connection or real event delivery.

## Current known limitation

This repository folder is an integration layer, not a complete Unity project. It does not include the official SDK package, a configured Unity scene/Animator, platform credentials, deployed cloud functions, or admission approval. Compilation and real-room verification remain pending until those external prerequisites exist.
