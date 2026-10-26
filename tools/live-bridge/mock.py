#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""模拟直播间事件：每 1~3 秒往 stdout 吐一条 JSON，不接真数据也能测联动。

用法：python3 mock.py | python3 bridge.py --port 17632

注意：这只是演示用的假事件，接真实抖音直播数据请参考 protocol.md。
"""
import json
import random
import sys
import time

GIFTS = ["小心心", "粉丝灯牌", "玫瑰", "棒棒糖", "火箭"]
USERS = ["小可爱", "皮皮虾", "奶茶三分糖", "阿伟", "小鹿", "只想白嫖"]
COMMENTS = ["主播真好看", "求关注", "唱首歌吧", "666", "笑死我了", "爱了爱了", "感谢主播"]


def event():
    t = random.choice(["gift", "gift_big", "follow", "like", "comment", "enter"])
    u = random.choice(USERS)
    if t == "gift":
        return {"type": "gift", "user": u, "item": random.choice(GIFTS), "count": random.randint(1, 8)}
    if t == "gift_big":
        return {"type": "gift_big", "user": u, "item": "火箭", "count": 1, "amount": 1000}
    if t == "follow":
        return {"type": "follow", "user": u}
    if t == "like":
        return {"type": "like", "user": u, "count": random.randint(10, 99)}
    if t == "comment":
        return {"type": "comment", "user": u, "text": random.choice(COMMENTS)}
    return {"type": "enter", "user": u}


def main():
    try:
        while True:
            sys.stdout.write(json.dumps(event(), ensure_ascii=False) + "\n")
            sys.stdout.flush()
            time.sleep(random.uniform(1.0, 3.0))
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
