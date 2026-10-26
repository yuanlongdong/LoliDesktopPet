# 甜心桌宠 · LoliDesktopPet

独立的 Android 桌面宠物原型。与 `AlphaTradingEngine` 完全分离。

## 当前功能
- 悬浮窗权限引导与桌宠启动/收起
- 桌宠可拖动
- 点击切换互动短句，连续点击可隐藏/显示气泡
- 前台通知维持服务运行

## 当前限制
角色目前是 Canvas 绘制的简化矢量占位图，不是 Live2D 或逐帧动画；暂未加入自然语音。

## 构建 APK
打开 GitHub Actions，运行 **Build LoliDesktopPet APK** 工作流，完成后从 workflow run 的 Artifacts 下载 `loli-desktop-pet-debug-apk`。

也可在本地安装 JDK 17、Android SDK 35 和 Gradle 8.9 后，在项目根目录执行：

```bash
gradle --no-daemon assembleDebug
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`。
