# 抖音手机开播真实互动接入

## 当前状态

Android 桌宠已具备内部事件入口（PetService 的 ACTION_LIVE_EVENT）和事件到动作的映射，但这**不是**抖音直播连接。此仓库没有抖音开放平台凭证、已开通的直播互动能力，也没有已部署的回调服务，因此不能声称已经接收到真实弹幕、点赞或礼物。

## 手机开播时的官方接入边界

抖音开放平台当前公开的直播实时互动方案是「直播小玩法」能力：开发者创建并申请开通直播小玩法/互动数据能力，在主播直播间挂载玩法；平台把获准的评论、点赞、礼物等事件推送到开发者服务端，服务端再通过 WebSocket 等方式把处理后的事件推给客户端。

这不是一个普通 Android App 仅凭悬浮窗权限就能读取任意抖音直播间数据的通用 API。单独在同一部手机上运行桌宠，不会自动获得抖音直播事件。

官方文档：
- 接入指南：https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/douyincloud/guide
- 直播间评论互动能力与开通流程：https://developer.open-douyin.com/docs/resource/zh-CN/interaction/jierushuoming/hudongshuju/pinglunshuju/

## 真实数据链路

```
抖音直播间（主播挂载已获批的直播小玩法）
  -> 抖音平台推送真实评论 / 点赞 / 礼物事件
  -> 已配置并验证的官方回调服务
  -> 服务端校验来源、去重、限流、过滤敏感文本
  -> 安全的 WSS WebSocket 通道
  -> Android LiveBridgeClient
  -> PetService.submitLiveEvent(...)
  -> PetReactionMapper
  -> 原始动画与台词气泡
```

## 开发顺序

1. 先在抖音开放平台创建直播小玩法应用，并申请开通直播间互动数据能力。
2. 根据平台要求配置服务端回调和事件推送；不要把 App Secret、access token 或服务器密钥写进 APK。
3. 为手机端配置带身份验证的 WSS 地址，Android 客户端只接收服务端已验证的标准化事件。
4. 将评论、点赞、礼物分别映射到现有 LiveEvent.Type，再交给 PetService 的事件队列。
5. 使用抖音平台认可的测试/联调流程验证；只有收到平台真实回调并通过签名/来源校验后，状态才可显示「已连接」。

## 标准化事件格式（服务端到 Android）

服务端应只发送 JSON 文本消息，例如：

```json
{
  "event_id": "平台事件唯一标识",
  "type": "COMMENT_MENTION",
  "nickname": "已按产品隐私策略处理的昵称",
  "count": 1,
  "source": "douyin_open_platform"
}
```

允许的 type 需映射到 App 中已定义的 LiveEvent.Type；未知类型、重复 event_id、超长昵称/评论、非法计数和未认证连接必须丢弃。客户端不应自行把本地测试消息标成真实抖音事件。

## 当前阻塞与验收标准

当前唯一的外部阻塞不是桌宠动画，而是官方接入资格、直播小玩法配置和回调服务。拿到获批应用与已部署的 WSS 回调地址后，才能完成端到端真实连接验证。验收必须同时满足：
- 后台显示平台回调已收到真实事件；
- Android 显示已认证的 WSS 连接状态；
- 真实评论/点赞/礼物各触发预期动作；
- 断线自动重连，重复事件不会重复触发；
- 没有凭空生成的演示事件混入真实直播。
