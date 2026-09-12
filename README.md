# 调音节拍 · TuneBeat

轻量独立 Android 调音／节拍工具，当前 **0.3.0-v02**。

- 中文深色节拍器：30–240 BPM、加减／圆盘／输入，2/4、3/4、4/4、6/8；主拍细分与小节边界切换，锁屏播放、通知停止。
- 六弦调音器：Strat 木色琴头，默认自动选弦／手动锁弦，实测音与目标音分开显示，标准 E2 A2 D3 G3 B3 E4，A4=440Hz。
- 页面和速度记忆，启动保持暂停。只在使用调音时申请麦克风权限。
- 无广告、无联网服务；包名 `io.github.a3322505a.tunebeat`，与 Diatronome 并存。

基于 Diatronome 1.0.15（GPL-3.0），保留 LICENSE 与源码作者署名。原生 UI；当前节拍使用连续 PCM 采样时钟，调音使用 TarsosDSP MPM（GPL-3.0）。详情见 [P0](docs/P0-验证记录.md)、[P1](docs/P1-验证记录.md)、[P2](docs/P2-验证记录.md)。

构建：JDK17、Gradle8.12、Android SDK36、Build Tools36.1.0。

```sh
gradle :app:assembleRelease
```

Actions `TuneBeat-build` 提供未签名 APK，维护者使用单独保存的固定私钥签名后交付；私钥不进仓库或 CI。`Tool-verification` 保存测试与截图。P0、P1、P2 使用同一私有签名覆盖安装。

V0.2 改进已进入实现与验证，来源和算法对照见 [V02-sources](docs/V02-sources.md)。手机实效单列待测；不包含旧方案 P3 跟拍评分。
