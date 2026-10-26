#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""直播联动桥（服务端）：把直播间事件转发给桌宠。

桌宠（手机端）作为 TCP 客户端连到本程序监听的端口收事件。事件来源二选一：
  - 标准输入（每行一条 JSON），例如：python3 mock.py | python3 bridge.py
  - 追尾一个文件（--file），文件里每行一条 JSON，新追加的行会被实时转发

用法：
  python3 bridge.py --port 17632                # 从 stdin 读，监听本机所有网卡
  python3 bridge.py --host 192.168.1.5 --port 17632 --file events.txt

把真实抖音直播数据接进来：任何能吐出事件行的脚本都可以作为来源，
只要把它的输出 pipe 给本程序即可（见 protocol.md）。

纯 Python 标准库，无需安装依赖。
"""
import argparse
import socket
import sys
import threading
import time


def main():
    ap = argparse.ArgumentParser(description="直播联动桥：把直播间事件转发给桌宠")
    ap.add_argument("--host", default="0.0.0.0", help="监听地址（默认所有网卡）")
    ap.add_argument("--port", type=int, default=17632, help="监听端口（默认 17632）")
    ap.add_argument("--file", default=None, help="追尾该文件（每行一条 JSON），代替 stdin")
    args = ap.parse_args()

    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    srv.bind((args.host, args.port))
    srv.listen(8)
    print(f"[桥] 监听 {args.host}:{args.port}，等桌宠连接…（Ctrl+C 退出）", flush=True)

    clients = set()
    lock = threading.Lock()

    def accept():
        while True:
            c, addr = srv.accept()
            with lock:
                clients.add(c)
            print(f"[桥] 桌宠已连接 {addr}（当前 {len(clients)} 台）", flush=True)

    threading.Thread(target=accept, daemon=True).start()

    def line_iter():
        if args.file:
            with open(args.file, encoding="utf-8") as f:
                f.seek(0, 2)  # 从文件末尾开始追尾
                while True:
                    line = f.readline()
                    if line:
                        yield line
                    else:
                        time.sleep(0.1)
        else:
            for line in sys.stdin:
                yield line

    def broadcast(line):
        line = line.strip()
        if not line:
            return
        dead = []
        with lock:
            for c in list(clients):
                try:
                    c.sendall((line + "\n").encode("utf-8"))
                except Exception:
                    dead.append(c)
            for c in dead:
                clients.discard(c)
        if dead:
            print(f"[桥] 有连接断开，剩余 {len(clients)} 台", flush=True)

    for line in line_iter():
        broadcast(line)


if __name__ == "__main__":
    main()
