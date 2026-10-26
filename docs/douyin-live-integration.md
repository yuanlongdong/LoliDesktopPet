# 抖音直播互动桌宠：第一阶段实现说明

## 当前交付

- 新增 `docs/douyin-live-demo.html`：独立、单文件、移动端适配的互动控制台原型。
- 新增 `LiveInteraction.kt`：平台无关事件模型、有界去重队列、限流器和动作映射器。
- `PetService` 增加内部 `ACTION_LIVE_EVENT` 桥接入口，可把已校验事件映射到现有动画与气泡；服务没有新增网络权限。
- 以模拟事件验证点赞、关注、普通礼物、连续礼物、高价值礼物、弹幕点名六类互动。
- 预览角色动作反馈、感谢字幕、可选浏览器语音、事件日志与计数。
- 所有事件明确标注为本地模拟；不接触抖音账号，不抓取直播间，不读取凭据。
- 本功能放在独立分支 `feature/douyin-live-interaction`，不改 `main`，不改变现有 Android 桌宠功能。

## 事件适配契约

真实数据源以后只负责将获准来源的消息转换为统一事件；桌宠行为层不应依赖具体平台 SDK。

```json
{
  "eventId": "provider-event-id",
  "source": "authorized-provider",
  "type": "gift",
  "user": { "id": "opaque-id", "displayName": "观众昵称" },
  "gift": { "id": "gift-id", "name": "礼物名", "count": 1, "value": 1 },
  "occurredAt": "2026-10-26T00:00:00Z",
  "isReplay": false
}
```

必需规则：
1. 事件 ID 去重，避免重连后重复感谢。
2. 点赞和重复礼物聚合；评论限流，避免刷屏。
3. 动作队列设优先级：高价值礼物 > 普通礼物/关注 > 点赞。
4. 同一用户连续送礼在短窗口合并播报，但保留累计数量与来源事件。
5. 队列长度设上限；高优先级事件可以覆盖低优先级动作，不能无限堆积。
6. 原始昵称和弹幕作为不可信文本显示，做长度限制与安全渲染，禁止将其当作 HTML 执行。
7. 断线、授权过期、无数据时显示明确状态，绝不伪造真实事件。

## Android 接入边界

现有 `PetService` 管理悬浮窗、动画与台词气泡。建议后续新增独立的 `live/` 包：
- `LiveEvent.kt`：平台无关的数据模型
- `LiveEventSource.kt`：事件来源接口
- `LiveEventQueue.kt`：去重、合并、优先级和限流
- `PetReactionMapper.kt`：事件映射到已存在的动作 slug 与台词分类
- `LiveControlActivity.kt`：主播开关、连接状态、音量与规则配置

已新增 `PetService.submitLiveEvent(context, event)` 窄入口，以及 `LiveEventQueue` / `PetReactionMapper`。目前入口只接受应用内部提交的事件，尚未连接任何平台网络数据源。不要把平台网络连接和解析代码塞进帧循环。互动服务断开时，现有桌宠应完全照常运行。

## 真实抖音接入的前置条件

先确认账号类型、直播权限和可用的官方/授权事件来源。普通 Android 应用并不天然拥有直播礼物事件流。未经授权的页面抓取、协议逆向、账号 Cookie 采集不作为实现路径。接入层必须基于实际获准的 API / SDK 文档实现，并把连接状态与权限失败反馈给用户。

## 验收标准

- 原 `main` 分支无改动。
- 原有桌宠功能与互动模块隔离。
- 六类模拟事件能触发对应反馈并进入日志。
- 连续礼物可以合并，静音模式只显示字幕，清空日志可重置计数。
- 页面手机可用、无第三方 CDN 或依赖。
- 文档不声称真实抖音事件已接通。


## 官方接入路径核查（2026-10-26）

核对了抖音开放平台的公开文档后，当前不能把普通 Android APK 直接当成官方直播事件订阅端：

- [互动工具开发者指南（抖音 App + 直播伴侣）](https://developer.open-douyin.com/docs/resource/zh-CN/live-interactive-tools/development/tutorial/app-interaction-guide) 描述的是抖音直播互动插件：在平台环境内申请能力，插件卡片通过 `tt.subscribeLiveInteractPluginMessage` 订阅 `live_like`、`live_comment`、`live_gift`、`live_follow` 等开放消息。
- [互动工具接入指南](https://developer.open-douyin.com/docs/resource/zh-CN/live-interactive-tools/introduction/interactive-tools-guide) 当前页面明确提示“插件业务更新中，暂不准入新插件”。这意味着不能把新建插件作为已经可走通的即时方案。
- [直播间礼物互动能力](https://developer.open-douyin.com/docs/resource/zh-CN/interaction/jierushuoming/hudongshuju/liwushuju) 说明礼物数据推送需要申请对应能力，并通过启动任务等接口启用推送。

因此实际工程分成两条路线：
1. **先做可验证的 Android 桌宠本体**：事件桥接、动作映射、优先级、构建与 APK 产物，独立于平台授权。
2. **真实直播接入**：只有在账号/应用获得相应开放能力后，才能实现官方插件或服务端数据推送适配。若平台不批准新插件，必须改走其他获准的数据来源，不能靠页面抓取、Cookie 或协议逆向替代。

公开文档核查不是权限审批结果；本仓库没有抖音应用凭证，也没有声称已取得平台授权。官方参考链接：
- https://developer.open-douyin.com/docs/resource/zh-CN/live-interactive-tools/development/tutorial/app-interaction-guide
- https://developer.open-douyin.com/docs/resource/zh-CN/live-interactive-tools/introduction/interactive-tools-guide
- https://developer.open-douyin.com/docs/resource/zh-CN/interaction/jierushuoming/hudongshuju/liwushuju
