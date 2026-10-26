#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Adapt a DyHub SSE event stream to the LoliDesktopPet TCP JSON-lines protocol.

Example:
  python3 dyhub_sse_adapter.py \
    --url 'http://127.0.0.1:8757/api/events?types=chat,gift,member,like,follow' \
    | python3 bridge.py --port 17632

This adapter uses only the Python standard library. It does not collect Douyin
traffic itself; DyHub (or another compatible SSE source) must already be running.
"""
import argparse
import json
import sys
import time
import urllib.error
import urllib.request


def first_int(*values, default=1, minimum=1):
    for value in values:
        try:
            if value is not None:
                return max(minimum, int(value))
        except (TypeError, ValueError):
            continue
    return default


def adapt(event):
    """Map a DyHub-style event into the pet bridge's canonical schema."""
    if not isinstance(event, dict):
        return None

    kind = str(event.get("type", "")).lower()
    user_obj = event.get("user") or {}
    data = event.get("data") or {}
    if not isinstance(user_obj, dict):
        user_obj = {}
    if not isinstance(data, dict):
        data = {}

    user = str(user_obj.get("nickname") or event.get("nickname") or "")
    if kind in ("chat", "comment", "danmaku"):
        return {"type": "comment", "user": user, "text": str(data.get("content") or data.get("text") or "")}
    if kind == "gift":
        count = first_int(data.get("repeatCount"), data.get("comboCount"), data.get("groupCount"), default=1)
        unit = first_int(data.get("diamondCount"), data.get("diamond_count"), default=0, minimum=0)
        return {
            "type": "gift",
            "user": user,
            "item": str(data.get("giftName") or data.get("gift_name") or "礼物"),
            "count": count,
            "amount": unit * count,
        }
    if kind in ("member", "enter", "room_member"):
        return {"type": "enter", "user": user}
    if kind in ("follow", "social"):
        action = str(data.get("action", "")).lower()
        if action and action not in ("1", "follow", "关注", "followed"):
            return None
        return {"type": "follow", "user": user}
    if kind == "like":
        return {"type": "like", "user": user, "count": first_int(data.get("count"), data.get("total"), default=1)}
    return None


def main():
    parser = argparse.ArgumentParser(description="将 DyHub SSE 事件转换为桌宠桥协议")
    parser.add_argument(
        "--url",
        default="http://127.0.0.1:8757/api/events?types=chat,gift,member,like,follow",
        help="SSE events URL",
    )
    parser.add_argument("--timeout", type=int, default=60, help="HTTP read timeout in seconds")
    args = parser.parse_args()

    delay = 2
    while True:
        try:
            request = urllib.request.Request(
                args.url,
                headers={"Accept": "text/event-stream", "Cache-Control": "no-cache", "User-Agent": "LoliDesktopPet-SSE-Adapter/1.0"},
            )
            print(f"[SSE] 连接 {args.url}", file=sys.stderr, flush=True)
            with urllib.request.urlopen(request, timeout=max(5, args.timeout)) as response:
                delay = 2
                for raw in response:
                    line = raw.decode("utf-8", errors="replace").strip()
                    if not line.startswith("data:"):
                        continue
                    payload = line[5:].strip()
                    if not payload or payload == "[DONE]":
                        continue
                    try:
                        event = json.loads(payload)
                        converted = adapt(event)
                    except (json.JSONDecodeError, TypeError, ValueError):
                        continue
                    if converted is not None:
                        print(json.dumps(converted, ensure_ascii=False, separators=(",", ":")), flush=True)
        except KeyboardInterrupt:
            break
        except (urllib.error.URLError, TimeoutError, OSError) as exc:
            print(f"[SSE] 连接中断: {exc}; {delay}s 后重试", file=sys.stderr, flush=True)
            time.sleep(delay)
            delay = min(delay * 2, 30)


if __name__ == "__main__":
    main()
