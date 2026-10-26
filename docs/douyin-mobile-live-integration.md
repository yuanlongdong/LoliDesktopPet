# 抖音手机开播真实互动接入

## 当前结论

Android 桌宠已有内部入口 `PetService.submitLiveEvent(...)` 与 `PetReactionMapper`，但它们只是 App 内的事件处理层，不等于抖音直播连接。

核对抖音开放平台公开文档后，必须区分两条产品路线：

1. **普通 Android 悬浮桌宠 APK**：没有通用公开 API 能让独立 App 读取任意抖音直播间的点赞、评论和礼物。仅靠悬浮窗权限不会获得这些数据。不能把普通 APK 伪装成直播玩法客户端来绕过平台边界。
2. **官方直播小玩法**：通过审核的玩法可申请互动数据能力。手机开播需接入云启动能力，由官方直播玩法客户端运行。抖音云将平台实时事件推送给玩法客户端，可在客户端把事件映射为角色动画。

官方互动工具指南目前还注明暂不准入新插件，因此不能把旧的 `douyin-plugin/` 路线视为已可发布。当前优先开发独立目录 `douyin-live-play/` 的官方直播小玩法客户端。

## 官方资料

- [抖音云接入指南](https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/douyincloud/guide)
- [直播玩法简介](https://developer.open-douyin.com/docs/resource/zh-CN/interaction/introduction/live-play/1)
- [创建直播小玩法与提案](https://developer.open-douyin.com/docs/resource/zh-CN/interaction/introduction/zn/toybox-start)
- [Unity SDK 接入](https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/unity-sdk/unity-sdk-access)
- [手机开播云启动适配](https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/unity-sdk/cloud-sync/mobile-live-integration)
- [直播间数据开放说明](https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/server/live/danmu)

抖音官方文档明确要求直播玩法本身具有直播互动内容，不能让普通应用/编辑器借直播数据能力为无关功能服务。手机开播需要按平台流程完成云启动适配、开放能力申请和玩法提审。

## 当前代码路径

- Android 内部事件模型与动作映射：`app/src/main/java/com/dshpet/android/LiveInteraction.kt`
- Android 悬浮窗事件入口：`app/src/main/java/com/dshpet/android/PetService.kt`
- 互动插件试验代码：`douyin-plugin/`（目前暂不准入新插件，不能认为可发布）
- 官方直播小玩法客户端与抖音云代码：`douyin-live-play/`

## 官方事件链路

```
主播在抖音手机端启动已通过审核的直播小玩法
  -> /start_game 启动 live_like / live_comment / live_gift 任务
  -> 抖音平台通过内网回调推送真实事件到 /live_data_callback
  -> 抖音云函数将消息推送到官方 WebSocket 网关
  -> Unity LiveOpenSDK / SampleDyCloudManager 接收消息
  -> DouyinCloudMessageAdapter 解析、过滤平台测试数据并 ACK
  -> PetLiveReactionController 去重、限流、触发 Animator 与台词气泡
  -> /finish_game 停止当前玩法启动的事件任务
```

此方案的互动角色属于抖音直播玩法内部，而不是普通 Android 悬浮窗覆盖在任意直播页面上。当前 APK 继续保持离线，不添加网络权限，也没有接入上述实时事件源。

## 已完成的代码

- 新增 `douyin-live-play/Assets/Scripts/PetLiveReactionController.cs`：标准化事件队列、去重、限流、Animator Trigger 和台词气泡。
- 新增 `douyin-live-play/Assets/Scripts/DouyinCloudMessageAdapter.cs`：对接官方 Unity SDK 示例 `SampleDyCloudManager.OnDyCloudMessage`，解析消息数组，跳过平台测试事件，并在处理后发送 ACK。
- 新增 `douyin-live-play/Assets/Scripts/DouyinLivePlayBootstrap.cs`：接线官方 SDK 初始化、`/start_game`、WebSocket 连接、`/finish_game` 清理等生命周期。
- 新增 `douyin-live-play/cloud/start_game.ts`：按当前直播上下文启动点赞、评论、礼物推送任务。
- 新增 `douyin-live-play/cloud/live_data_callback.ts`：接收抖音云内网回调并向官方 WebSocket 网关推送事件。
- 新增 `douyin-live-play/cloud/websocket_callback.ts`：记录 WebSocket 生命周期状态，不记录弹幕原文。
- 新增 `douyin-live-play/cloud/finish_game.ts`：结束时停止推送任务。
- 新增 `douyin-live-play/README.md`：包含平台申请、Unity 接入、Animator 配置和验收清单。

## 尚未完成的真实运行条件

代码已写入 GitHub 功能分支，但目前并未完成真实运行，因为以下条件依赖开放平台账号和实际权限：

1. 创建直播小玩法，提交玩法提案并获得平台审核。
2. 开通评论、点赞、礼物互动数据能力和手机开播所需云启动能力。
3. 配置抖音云 EnvID / ServiceID 并在开发环境部署这些云函数。
4. Unity 项目安装官方 BGDT / LiveOpenSDK、配置真实 AppID，并把你的角色素材制作为合法可用的 Unity Animator 动画。
5. 在抖音真实直播间验证点赞、评论指令、礼物和结束清理。

以上没有完成前，不能宣称已收到真实直播数据，也不能宣称 Android APK 已与抖音直播端到端接通。当前改动没有合并到 `main`。
