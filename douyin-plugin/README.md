# 桌宠抖音直播互动插件（开发中）

这是独立的抖音「直播互动插件」小程序工程，使用官方开放能力订阅真实直播间事件。它不是 Android APK 内部直接读取抖音数据的功能。

## 已实现

- 订阅 `live_like`、`live_comment`、`live_gift`
- 读取官方事件中的 `msg_id`、`nickname`、`like_num`、`content`、`gift_num`
- 按事件 ID 去重
- 在抖音直播玩法卡片中显示真实收到的事件
- 明确显示订阅失败，不用模拟事件伪装成功

## 尚未完成的端到端链路

当前代码把事件从抖音插件传到插件自己的直播卡片。它**还没有把事件传给 Android 悬浮桌宠 APK**。要完成那一段，需要在获批的插件运行环境和 Android 客户端之间建立经过授权的安全转发链路，并配置可用的 WSS 服务端；Android 端再调用现有 `PetService.submitLiveEvent(...)`。

## 在哪里运行

在抖音开放平台创建「直播小玩法 → 互动插件」，选择可在抖音 App 和直播伴侣运行的小程序产物类型，再把本目录导入抖音开发者工具。需申请点赞、评论、礼物数据权限；真实事件必须在真实直播间调试，IDE 模拟事件不能作为真实直播验收。

官方文档：
- https://developer.open-douyin.com/docs/resource/zh-CN/live-interactive-tools/development/tutorial/app-interaction-guide
- https://developer.open-douyin.com/docs/resource/zh-CN/live-interactive-tools/introduction/interactive-tools-guide
