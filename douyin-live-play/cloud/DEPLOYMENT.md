# Douyin Cloud deployment map

This is a deployment runbook, not a claim that the functions have been deployed. Deployment requires access to the correct Douyin Open Platform account and Cloud environment.

Official references:
- Unity SDK / Douyin Cloud flow: https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/unity-sdk/live-unity-sdk-support/douyin-cloud
- Cloud event integration guide: https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/douyincloud/guide

## Route mapping

Deploy the four files as separate functions in the same environment and service:

| Route | Source | Purpose |
|---|---|---|
| `POST /start_game` | `cloud/start_game.ts` | Start comment, like, gift tasks using current live context |
| Platform live-data callback route | `cloud/live_data_callback.ts` | Validate platform headers and forward each event with its own `msg_id` |
| `POST /websocket_callback` | `cloud/websocket_callback.ts` | Log connection lifecycle without logging free-form comments |
| `POST /finish_game` | `cloud/finish_game.ts` | Stop the three event tasks |

Use the exact route conventions required by the current Douyin Cloud console. The platform callback route may be configured separately from normal client-callable routes. Do not expose internal callback functions as public anonymous endpoints unless the platform's deployment instructions require it.

## Before deployment

- [ ] Confirm the project is admitted as a live mini-game and the account has the comment, like and gift data permissions.
- [ ] Confirm the cloud service is in the same app/environment used by the Unity client.
- [ ] Confirm the runtime supports the official `@open-dy/node-server-sdk` package and TypeScript module format.
- [ ] Configure the official platform callback to the deployed live-data callback route.
- [ ] Ensure `/websocket_callback` is the WebSocket callback route configured for the client connection.
- [ ] Deploy from a development environment first. Never copy secrets into source files or GitHub Actions logs.

## Acceptance sequence

1. Run `npm install && npm run test:cloud` locally or use the GitHub Actions check.
2. Deploy the four functions to a development environment.
3. Use the Open Platform console's **Development > Self-check tools > OpenAPI self-test tool** where available.
4. Invoke `start_game` in an authorized live context and verify three task-start results all report `err_no = 0`.
5. Send one real like, one real comment and one real gift. Verify the callback receives platform headers, each event retains its own `msg_id`, and the WebSocket client receives the matching payload.
6. Confirm the visible pet reaction occurs before ACK; inspect platform logs for successful fulfillment.
7. Invoke `finish_game`; verify all three stop results report success.
8. Test partial start failure, gateway failure/retry, reconnect and cleanup before production release.

## Deployment status

**Not deployed.** The repository has source code and automated isolated tests only. No cloud environment identifier, service identifier, access token, or platform console session is available to this repository integration. A source-code commit cannot deploy to Douyin Cloud by itself.
