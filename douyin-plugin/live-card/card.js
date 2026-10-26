Card({
  created() {
    this.canvas = this.getCanvas();
    this.ctx = this.canvas.getContext('2d');
    this.canvas.width = 600;
    this.canvas.height = 240;
    this.events = [];
    this.draw();
  },

  onPetLiveEvent(event) {
    if (!event || !event.eventId) return;
    if (this.events.some((item) => item.eventId === event.eventId)) return;
    this.events.unshift(event);
    this.events = this.events.slice(0, 3);
    this.draw();
  },

  draw() {
    const ctx = this.ctx;
    const canvas = this.canvas;
    if (!ctx || !canvas) return;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = 'rgba(18, 22, 32, 0.88)';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = '#ff7722';
    ctx.font = 'bold 28px sans-serif';
    ctx.fillText('桌宠直播互动', 24, 42);
    ctx.fillStyle = '#ffffff';
    ctx.font = '22px sans-serif';
    if (!this.events.length) {
      ctx.fillText('等待真实直播事件…', 24, 92);
      ctx.fillStyle = '#9aa4b2';
      ctx.font = '17px sans-serif';
      ctx.fillText('收到点赞、评论、礼物后将在这里显示', 24, 128);
      return;
    }
    this.events.forEach((event, index) => {
      const label = event.type === 'LIKE' ? '点赞' : event.type === 'GIFT' ? '礼物' : '评论';
      const text = (event.nickname || '观众') + ' · ' + label + (event.text ? ' · ' + event.text : '');
      ctx.fillStyle = index === 0 ? '#ffffff' : '#b7c0cc';
      ctx.font = index === 0 ? 'bold 21px sans-serif' : '18px sans-serif';
      ctx.fillText(text.slice(0, 42), 24, 86 + index * 46);
    });
  }
});
