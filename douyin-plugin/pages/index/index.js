const MESSAGE_TYPES = ['live_like', 'live_comment', 'live_gift'];

function reactionFor(payload) {
  if (typeof payload.content === 'string' && payload.content.length) {
    return { kind: 'COMMENT', count: 1, text: payload.content.slice(0, 80) };
  }
  if (typeof payload.gift_num === 'number') {
    return { kind: 'GIFT', count: Math.max(1, payload.gift_num), text: '送出了礼物' };
  }
  if (typeof payload.like_num === 'number') {
    return { kind: 'LIKE', count: Math.max(1, payload.like_num), text: '点了赞' };
  }
  return null;
}

Page({
  data: {
    statusText: '未启动。开启后会申请订阅真实直播事件。',
    lastEvent: '尚未收到直播事件'
  },

  onUnload() {
    this.stop();
  },

  start() {
    if (this._starting || this._listening) return;
    this._starting = true;
    this.setData({ statusText: '正在启动直播卡片…' });

    tt.getLiveRoomCardInfo({
      success: (info) => {
        tt.createLiveCard({
          url: '/live-card/card',
          width: info.liveCardMaxWidth,
          height: info.liveCardMaxHeight,
          success: (res) => {
            const app = getApp();
            app.globalData.liveCardContext = res.cardContext;
            this.subscribeRealEvents();
          },
          fail: (error) => {
            this._starting = false;
            this.setData({
              statusText: '直播卡片启动失败：' + String((error && error.errMsg) || '请确认在真实直播间环境运行')
            });
          }
        });
      },
      fail: (error) => {
        this._starting = false;
        this.setData({
          statusText: '无法读取直播卡片尺寸：' + String((error && error.errMsg) || '请在抖音直播间内打开插件')
        });
      }
    });
  },

  subscribeRealEvents() {
    tt.subscribeLiveInteractPluginMessage({
      messageType: MESSAGE_TYPES,
      success: () => {
        this._starting = false;
        this._listening = true;
        this.setData({ statusText: '已订阅，等待真实点赞、评论或礼物事件…' });
        tt.onReceiveLiveInteractPluginMessage((payloads) => {
          if (!Array.isArray(payloads)) return;
          payloads.forEach((payload) => {
            if (!payload || !payload.msg_id) return;
            const app = getApp();
            if (!app.globalData.seenEventIds) app.globalData.seenEventIds = {};
            const id = String(payload.msg_id);
            if (app.globalData.seenEventIds[id]) return;
            app.globalData.seenEventIds[id] = true;
            const reaction = reactionFor(payload);
            if (!reaction) return;
            const event = {
              eventId: id,
              type: reaction.kind,
              nickname: String(payload.nickname || '观众').slice(0, 24),
              count: reaction.count,
              text: reaction.text,
              timestamp: Number(payload.timestamp || Date.now())
            };
            this.setData({
              statusText: '已收到抖音开放平台真实事件',
              lastEvent: event.nickname + ' · ' + reaction.text
            });
            const card = app.globalData.liveCardContext;
            if (card && typeof card.onPetLiveEvent === 'function') {
              card.onPetLiveEvent(event);
            }
          });
        });
      },
      fail: (error) => {
        this._starting = false;
        this.setData({
          statusText: '订阅失败：' + String((error && (error.errMsg || error.message)) || '请检查开放平台数据权限')
        });
      }
    });
  },

  stop() {
    if (!this._listening) {
      this._starting = false;
      this.setData({ statusText: '互动已关闭' });
      return;
    }
    tt.unsubscribeLiveInteractPluginMessage({
      messageType: MESSAGE_TYPES,
      success: () => {
        this._listening = false;
        const app = getApp();
        app.globalData.liveCardContext = null;
        this.setData({ statusText: '已取消真实事件订阅' });
      },
      fail: (error) => {
        this.setData({
          statusText: '取消订阅失败：' + String((error && error.errMsg) || '未知错误')
        });
      }
    });
  }
});
