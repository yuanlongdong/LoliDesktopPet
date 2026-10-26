#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Convert jwwsjlm/douyinLive WebSocket messages to LoliDesktopPet JSON-lines.

Install: python -m pip install websockets
Run:
  python douyinlive_ws_adapter.py --room 516466932480 \
    | python bridge.py --port 17632

The upstream collector must already be running on the same computer (default port 1088).
This script only adapts events; it does not bypass login, captcha, or platform controls.
"""
import argparse
import asyncio
import json
import sys
import time

try:
    import websockets
except ImportError:
    print("[adapter] 缺少 websockets 依赖：python -m pip install websockets", file=sys.stderr, flush=True)
    raise


def obj(value):
    return value if isinstance(value, dict) else {}


def first(*values, default=""):
    for value in values:
        if value is not None and value != "":
            return value
    return default


def to_pet_event(msg):
    if not isinstance(msg, dict) or msg.get("type") == "system":
        return None

    method = str(msg.get("method", ""))
    user_obj = obj(msg.get("user"))
    user = str(first(user_obj.get("nickname"), user_obj.get("display_id"), msg.get("nickname"), default="观众"))
    gift = obj(msg.get("gift"))

    if method == "WebcastChatMessage":
        content = first(msg.get("content"), msg.get("text"))
        if not content:
            return None
        return {"type": "comment", "user": user, "text": str(content)}

    if method == "WebcastGiftMessage":
        count = first(msg.get("repeatCount"), msg.get("repeat_count"), msg.get("comboCount"), msg.get("groupCount"), default=1)
        diamonds = first(gift.get("diamondCount"), gift.get("diamond_count"), msg.get("diamondCount"), default=0)
        try:
            amount = max(0, int(diamonds)) * max(1, int(count))
        except (TypeError, ValueError):
            amount = 0
        return {
            "type": "gift",
            "user": user,
            "item": str(first(gift.get("name"), gift.get("describe"), msg.get("giftName"), default="礼物")),
            "count": max(1, int(count)) if str(count).isdigit() else 1,
            "amount": amount,
        }

    if method == "WebcastLikeMessage":
        count = first(msg.get("count"), msg.get("total"), default=1)
        return {"type": "like", "user": user, "count": max(1, int(count)) if str(count).isdigit() else 1}

    if method == "WebcastMemberMessage":
        return {"type": "enter", "user": user}

    if method in ("WebcastSocialMessage", "WebcastFollowMessage"):
        action = str(first(msg.get("action"), obj(msg.get("data")).get("action"), default=""))
        # WebcastSocialMessage action=1 is commonly used for following.
        if action in ("", "1", "follow", "关注", "followed"):
            return {"type": "follow", "user": user}

    return None


async def consume(url):
    delay = 2
    while True:
        try:
            print(f"[adapter] 连接采集器：{url}", file=sys.stderr, flush=True)
            async with websockets.connect(url, ping_interval=20, ping_timeout=20, open_timeout=15, max_size=8 * 1024 * 1024) as ws:
                delay = 2
                print("[adapter] 已连接，等待直播事件", file=sys.stderr, flush=True)
                async for raw in ws:
                    try:
                        message = json.loads(raw)
                    except (json.JSONDecodeError, TypeError):
                        if raw == "ping":
                            await ws.send("pong")
                        continue
                    event = to_pet_event(message)
                    if event:
                        print(json.dumps(event, ensure_ascii=False, separators=(",", ":")), flush=True)
        except KeyboardInterrupt:
            return
        except Exception as exc:
            print(f"[adapter] 连接中断：{exc}；{delay}s 后重试", file=sys.stderr, flush=True)
            await asyncio.sleep(delay)
            delay = min(delay * 2, 30)


def main():
    parser = argparse.ArgumentParser(description="douyinLive WebSocket → 桌宠直播桥")
    parser.add_argument("--room", required=True, help="直播间标识或房间号")
    parser.add_argument("--ws-base", default="ws://127.0.0.1:1088", help="douyinLive 本地服务地址")
    args = parser.parse_args()
    room = args.room.strip().rstrip("/").split("/")[-1]
    if not room:
        parser.error("--room 不能为空")
    url = args.ws_base.rstrip("/") + "/ws/" + room
    try:
        asyncio.run(consume(url))
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
