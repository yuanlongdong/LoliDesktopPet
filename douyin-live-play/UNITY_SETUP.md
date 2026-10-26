# Unity integration and compile gate

This folder is now arranged as a Unity project scaffold, but it is not yet a verified build. The official LiveOpenSDK is distributed through BGDT and is not a public UPM dependency; do not add a guessed package URL or commit the SDK binaries.

## Required SDK version path

Official documentation:
- Unity SDK install: https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/unity-sdk/unity-sdk-access
- SDK API overview: https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/unity-sdk/live-unity-sdk-support/api-overview
- Douyin Cloud API: https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/unity-sdk/live-unity-sdk-support/douyin-cloud
- Fulfillment ACK: https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/unity-sdk/live-unity-sdk-support/ack-ability

The current public docs describe LiveOpenSDK 2.0.0 APIs, including `LiveOpenSdk.Instance`, `GetDyCloudApi()`, `GetMessageAckService().ReportAck(msgId, msgType)`, and WebSocket callback APIs. The package must be installed through BGDT's `cp` channel. If the console only shows Apply, the platform says that version requires access approval.

## Exact editor steps

1. Install Unity 2022.3 LTS and BGDT from the official developer portal.
2. Open this folder as the Unity project.
3. In Unity, choose **ByteGame > ByteGame Develop Tools > Install LiveOpenSDK**. Confirm `Packages/com.bytedance.liveopensdk` exists and note the installed SDK version.
4. Copy the SDK's `Samples` folder into `Assets/ThirdParty/LiveOpenSDK/Samples` rather than modifying files inside `Packages`.
5. Compile before scene wiring. If any `DouyinLivePlay` script fails, capture the full compiler error and SDK version. Do not suppress errors by adding fake SDK stubs to the release build.
6. In the scene, bind the character Animator, reaction controller, adapter and bootstrap. Required Animator triggers: `Like`, `Comment`, `Gift`, `BigGift`, `Dance`, `Heart`, `Wave`, `Hug`, `Cheer`.
7. Test first in the SDK's supplied `SampleGameScene.unity`, then in the pet scene.
8. Only claim a successful compile after the Unity Editor Console has zero errors. Only claim real integration after a real authorized room produces events and the ACK is visible in platform logs.

## Current limitation

The SDK is access-controlled through BGDT. No SDK package, AppID, cloud EnvID/ServiceID, or user-owned pet animation asset is committed to this repository. These are not safe to invent. Until the real SDK is installed, this project cannot honestly be marked as Unity-compiled.
