const MESSAGE_TYPES = ['live_like', 'live_comment', 'live_gift'];

function reactionFor(payload) {
  if (payload.content) return { kind: 'COMMENT', count: 1, text: String(payload.content).slice(0, 80) };
  if (typeof payload.gift_num === 'number') return { kind: 'GIFT', count: Math.max(1, payload.gift_num), text: '送出了礼物' };
  if (typeof payload.like_num === 'number') return { kind: 'LIKE', count: Math.max(1, payload.like_num), text: '点了赞' };
  return null;
}

Page({
  data: {
    statusText: '未启动。点击“开启互动”后申请订阅真实事件。',
    lastEvent: '尚未收到直播事件'
  },

  onUnload() {
    this.stop();
  },

  start() {
    if (this._listening) return;
    this.setData({ statusText: '正在向抖音请求订阅权限…' });
    tt.subscribeLiveInteractPluginMessage({
      messageType: MESSAGE_TYPES,
      success: () => {
        this._listening = true;
        this.setData({ statusText: '已订阅。等待真实直播间点赞、评论或礼物事件…' });
        tt.onReceiveLiveInteractPluginMessage((payloads) => {
          if (!Array.isArray(payloads)) return;
          payloads.forEach((payload) => {
            if (!payload || !payload.msg_id) return;
            const app = getApp();
            if (app.globalData.lastEventAt === payload.msg_id) return;
            app.globalData.lastEventAt = payload.msg_id;
            const reaction = reactionFor(payload);
            if (!reaction) return;
            const event = {
              eventId: String(payload.msg_id),
              type: reaction.kind,
              nickname: String(payload.nickname || '观众').slice(0, 24),
              count: reaction.count,
              text: reaction.text,
              timestamp: Number(payload.timestamp || Date.now())
            };
            this.setData({
              statusText: '真实事件已收到',
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
        this.setData({
          statusText: '订阅失败：' + String((error && (error.errMsg || error.message)) || '请检查开放平台权限和直播环境')
        });
      }
    });
  },

  stop() {
    if (!this._listening) {
      this.setData({ statusText: '互动已关闭' });
      return;
    }
    tt.unsubscribeLiveInteractPluginMessage({
      messageType: MESSAGE_TYPES,
      success: () => {
        this._listening = false;
        this.setData({ statusText: '已取消订阅' });
      },
      fail: (error) => {
        this.setData({ statusText: '取消订阅失败：' + String((error && error.errMsg) || '未知错误') });
      }
    });
  }
});
