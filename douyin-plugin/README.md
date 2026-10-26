# 桌宠抖音直播互动插件（开发中）

这是独立的抖音「直播小玩法 → 互动插件」小程序工程，使用抖音官方直播互动 API 接收真实直播间事件。它不是 Android APK 内部直接读取抖音数据的功能。

## 已实现的代码

- 玩法面板启动直播卡片：`tt.getLiveRoomCardInfo`、`tt.createLiveCard`
- 请求订阅 `live_like`、`live_comment`、`live_gift`
- 监听 `tt.onReceiveLiveInteractPluginMessage`
- 使用 `msg_id` 去重，读取昵称、评论内容、点赞数、礼物数量
- 把真实事件传给玩法卡片并显示
- 订阅失败时显示真实错误，不用模拟事件伪装成功
- 附带抖音开发者工具项目配置；导入时需在开放平台创建项目并填写实际 AppID

## 重要限制

1. 真实事件只会在抖音直播间运行环境中出现，并且需要开放平台申请对应的数据权限。
2. 官方互动工具接入指南目前注明「插件业务更新中，暂不准入新插件」。因此即使本地代码完成，也不代表当前一定能申请入驻、审核或发布。
3. 本工程当前把事件显示在抖音玩法卡片中，**尚未把事件接入 Android 悬浮桌宠 APK 的动画系统**。
4. 官方互动工具规范还明确要求此类能力不连接网络、不经过开发者服务器。因此不能简单把此前设想的云端 WSS 转发当成可行方案。要把事件送进独立 Android 悬浮窗，必须先确认抖音允许的运行方式和系统侧可用的通信边界，不能绕过平台规则。
5. 本仓库尚未在抖音开发者工具或真实直播间完成编译、授权和端到端测试。

## 真实测试流程

1. 在抖音开放平台查看「直播小玩法 → 互动插件」当前是否允许创建新项目。
2. 如开放申请，创建小程序类型互动插件，申请点赞、评论、礼物数据能力，并填写实际 AppID。
3. 用抖音开发者工具导入本目录，检查 API 兼容性并修复 IDE 报错。
4. 按官方调试流程绑定调试成员，在真实直播间开启插件，分别验证真实评论、点赞、礼物。
5. 只有以上步骤成功后，才能认定插件端真实事件链路通过；Android 桌宠联动仍需单独验收。

## 官方资料

- 互动工具开发者指南：https://developer.open-douyin.com/docs/resource/zh-CN/live-interactive-tools/development/tutorial/app-interaction-guide
- 互动工具能力概述：https://developer.open-douyin.com/docs/resource/zh-CN/live-interactive-tools/capability/capability-overview
- 互动工具接入指南（含当前准入状态与网络限制）：https://developer.open-douyin.com/docs/resource/zh-CN/live-interactive-tools/introduction/interactive-tools-guide
