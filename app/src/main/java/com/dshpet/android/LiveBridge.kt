package com.dshpet.android

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 直播联动网络桥：作为 TCP 客户端，连到用户在自己电脑/服务器上跑的桥
 * （见 tools/live-bridge/protocol.md），按行读取 JSON 事件，转发给反应引擎。
 *
 * 特性：
 *  - 断线自动重连，指数退避（2s → 30s），适合直播中网络抖一下自动恢复
 *  - 收行/解析在网络线程，投递到主线程后再触发反应
 *  - 纯 java.net，零第三方依赖
 */
class LiveBridge(
    private val host: String,
    private val port: Int,
    private val onLine: (String) -> Unit,   // 收到一行（未解析，交给调用方 parseLiveEvent）
    private val onStatus: (String) -> Unit,  // 连接状态（用于设置页显示 / 写入诊断）
) {
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val running = AtomicBoolean(false)
    private var socket: Socket? = null

    fun start() {
        if (running.getAndSet(true)) return
        worker.execute { runLoop() }
    }

    fun stop() {
        if (!running.getAndSet(false)) return
        try {
            socket?.close()
        } catch (_: Throwable) {
        }
        worker.shutdownNow()
    }

    private fun runLoop() {
        var delayMs = 2000L
        while (running.get()) {
            val s = Socket()
            try {
                s.connect(InetSocketAddress(host, port), 6000)
                socket = s
                delayMs = 2000L
                status("已连接 $host:$port")
                val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                while (running.get()) {
                    val line = reader.readLine() ?: break   // 对端断开返回 null
                    if (line.isBlank()) continue
                    val payload = line
                    main.post { onLine(payload) }
                }
            } catch (t: Throwable) {
                Log.w("LiveBridge", "连接失败: ${t.message}")
            } finally {
                try {
                    s.close()
                } catch (_: Throwable) {
                }
                socket = null
            }
            if (!running.get()) break
            status("未连接 $host:$port，${delayMs / 1000}s 后重试")
            val wait = delayMs
            delayMs = (delayMs * 2).coerceAtMost(30_000L)
            sleep(wait)
        }
        status("直播联动已关闭")
    }

    private fun status(msg: String) = main.post { onStatus(msg) }

    /** 可中断的睡眠：stop() 后能立刻退出重连等待 */
    private fun sleep(ms: Long) {
        val deadline = System.currentTimeMillis() + ms
        while (running.get() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(50)
            } catch (_: InterruptedException) {
                break
            }
        }
    }
}
