# 调音节拍 · TuneBeat

轻量独立 Android 调音／节拍工具，当前 **0.2.2-p2**。

- 中文深色节拍器：30–240 BPM、加减／圆盘／输入，4/4 第一拍重音，锁屏播放、通知停止。
- 六弦调音器：木色琴头，手动选弦，偏差提示，标准 E2 A2 D3 G3 B3 E4，A4=440Hz。
- 页面和速度记忆，启动保持暂停。只在使用调音时申请麦克风权限。
- 无广告、无联网服务；包名 `io.github.a3322505a.tunebeat`，与 Diatronome 并存。

基于 Diatronome 1.0.15（GPL-3.0），保留 LICENSE 与源码作者署名。复用音频与音高算法，原生 UI 重新实现。详情见 [P0](docs/P0-验证记录.md)、[P1](docs/P1-验证记录.md)、[P2](docs/P2-验证记录.md)。

构建：JDK17、Gradle8.12、Android SDK36、Build Tools36.1.0。

```sh
gradle :app:assembleRelease
```

Actions `TuneBeat-build` 提供未签名 APK，维护者使用单独保存的固定私钥签名后交付；私钥不进仓库或 CI。`Tool-verification` 保存测试与截图。P0、P1、P2 使用同一私有签名覆盖安装。

当前停止于 P2。P3 跟拍评分未实现；真机音频表现待试装。
