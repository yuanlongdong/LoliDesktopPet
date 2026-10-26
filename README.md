# 甜心桌宠 · LoliDesktopPet

独立的 Android 桌面宠物原型，与 `AlphaTradingEngine` 完全分离。

## 当前功能
- 悬浮窗权限引导与桌宠启动/收起
- 桌宠可拖动
- 点击切换互动短句，连续点击可隐藏/显示气泡
- 前台通知维持服务运行

## 当前限制
角色目前是 Canvas 绘制的简化矢量占位图，不是 Live2D 或逐帧动画；暂未加入自然语音。

## 获取 APK
1. 打开本仓库的 [Actions](https://github.com/yuanlongdong/LoliDesktopPet/actions/workflows/build-apk.yml)。
2. 进入最新的 **Build LoliDesktopPet APK** 任务。
3. 等待构建成功，在页面底部的 **Artifacts** 下载 `loli-desktop-pet-debug-apk`。
4. 解压 ZIP，得到 `app-debug.apk`，传到安卓手机安装。首次使用需要在系统设置中允许“显示在其他应用上层”。

工作流会在推送到 `main` 或手动触发时构建 APK，产物保留 14 天。

## 本地构建
安装 JDK 17、Android SDK 35 和 Gradle 8.9 后，在项目根目录执行：

```bash
gradle --no-daemon assembleDebug
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`。
