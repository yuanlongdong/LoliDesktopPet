# 桌宠抖音直播小玩法客户端（独立开发路径）

这个目录是把桌宠改造成**抖音官方直播小玩法**的独立客户端路径，不会改动现有 Android 悬浮桌宠 APK。它围绕官方「直播玩法 + 抖音云 + Unity SDK」的互动链路开发，目标是在抖音手机开播时，由真实的点赞、评论指令和礼物事件驱动角色动画。

## 先讲清楚产品边界

这不是把普通 Android App 接到直播间数据上。抖音官方说明，直播玩法必须本身具备直播互动玩法内容；普通应用/编辑器不能借直播玩法获取数据后自行应用。手机开播要求通过官方云启动能力运行经审核的玩法。

官方文档：
- [直播玩法简介与产品形态](https://developer.open-douyin.com/docs/resource/zh-CN/interaction/introduction/live-play/1)
- [直播玩法创建与开放能力申请](https://developer.open-douyin.com/docs/resource/zh-CN/interaction/introduction/zn/toybox-start)
- [抖音云互动事件接入](https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/douyincloud/guide)
- [Unity SDK 接入](https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/unity-sdk/unity-sdk-access)
- [手机端适配要求](https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/unity-sdk/cloud-sync/mobile-live-integration)

## 本次新增的代码

- `Assets/Scripts/PetLiveReactionController.cs`：标准化事件队列、去重、频率控制、角色 Animator 触发、台词气泡、安全的评论指令白名单。
- `Assets/Scripts/DouyinCloudMessageAdapter.cs`：接收 SDK 示例 `SampleDyCloudManager.OnDyCloudMessage`，解析官方云消息数组，处理真实事件并上报消息 ACK。
- `cloud/start_game.ts`：按当前直播上下文开启点赞、评论、礼物三个推送任务。
- `cloud/finish_game.ts`：一局结束时停止这三个任务，避免游戏结束后继续消费事件。
- `cloud/live_data_callback.ts`：接收抖音云内网回调，并将原始事件批次推送到官方 WebSocket 网关。
- `cloud/websocket_callback.ts`：记录客户端连接状态，不记录观众评论内容。

这些是已写入仓库的源码，**不是已部署的云服务，也不是已通过抖音审核的产品**。我没有你的抖音开放平台 AppID、抖音云 EnvID/ServiceID 或已审批的互动数据能力，所以目前不能宣称收到真实直播事件或已完成手机端开播联调。

## 运行前置条件

1. 在抖音开放平台创建「直播小玩法」，选择「弹幕玩法」类型并提交玩法提案。平台通过提案后才能申请对应开放能力。个人开发者也可按官方规则申请。
2. 申请并配置评论、点赞、礼物互动数据能力，以及用于手机开播的云启动能力。
3. 按官方文档建立抖音云服务，把 `cloud/start_game.ts`、`cloud/live_data_callback.ts`、`cloud/websocket_callback.ts` 分别配置为对应路径。
4. 在 Unity 2022.3 LTS 项目中安装抖音官方 BGDT 与 LiveOpenSDK。官方 SDK 示例目录包含 `SampleDyCloudManager.cs`；将 SDK 的 Samples 复制到项目的 `Assets` 下，避免直接修改 Packages 内的文件。
5. 在玩法启动代码中初始化 LiveOpenSDK，设置真实 `AppId`，初始化 `SampleDyCloudManager` 的 `EnvId` / `ServiceId` 后连接抖音云，并调用本目录 `DouyinCloudMessageAdapter.Bind(manager)`。
6. 在 Unity 场景中挂载 `PetLiveReactionController`，绑定角色 Animator 与可选的气泡 UI。Animator 需要配置以下 Trigger 名称：`Like`、`Comment`、`Gift`、`BigGift`、`Follow`、`Dance`、`Heart`、`Wave`、`Hug`。按实际角色动画命名可修改映射。
7. 在抖音云 dev 环境完成联调，再按平台要求发布到正式环境和提审。不要把 App Secret、访问令牌或云端私钥提交到 Git。

## 事件表现

| 真实事件 | 角色反馈 |
|---|---|
| 点赞 | 触发 `Like`，显示致谢气泡 |
| 评论 | 默认触发 `Comment`；命中“跳舞 / 比心 / 挥手 / 抱抱”白名单指令时触发相应动作。不会把任意弹幕原文直接显示到画面上 |
| 礼物 | 触发 `Gift`，数量达到 10 或以上时触发 `BigGift` |
| 关注 | 如后台开通并推送 `live_follow`，触发 `Follow` |

评论指令只决定已配置动作，不实现抽奖或随机付费效果。礼物触发规则需在直播玩法提案和平台运营配置中保持一致。

## 角色素材说明

当前 Android 桌宠的动画 WebP 是运行时下载并构建的上游素材，仓库没有提交这些二进制动画；上游素材的授权注明禁止商用。这个 Unity 子项目没有擅自复制它们，也没有用临时几何图冒充你的角色。接入抖音玩法前，需要把你有权使用的角色动画制作/导入为 Unity Animator 动画片段，再把片段挂到上述 Trigger 上。

## 当前验收状态

- [x] 写入客户端事件消费与反应映射代码
- [x] 写入抖音云三个函数源码
- [ ] 安装官方 SDK 与真实 AppID
- [ ] 配置并部署抖音云 dev 环境
- [ ] 提案通过并开通直播互动数据权限
- [ ] 在真实手机直播间验证点赞、评论、礼物触发
- [ ] 完成角色美术/动画资产接入

在最后四项完成前，不得把当前版本称为端到端已接通。
