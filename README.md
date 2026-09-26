# 节拍器 · TuneBeat

轻量独立 Android 节拍工具，当前 **0.5.0**。

- 中文深色界面：30–240 BPM、加减／圆盘／输入；2/4、3/4、4/4、6/8。
- 主拍细分、小节边界切换、锁屏播放、通知停止。
- 节奏练习：随机生成 12 小节 4/4 节奏谱，循环跟拍；四分、八分、二分、休止、附点和三连音，按音频播放位置高亮。可换一段，速度沿用节拍器设置。
- 保存速度、拍号和细分；冷启动保持暂停。
- 无广告、无联网服务、无需麦克风权限。
- 包名 `io.github.a3322505a.tunebeat` 不变，沿用原签名覆盖安装。

用户实测调音不准，2026-09-19 决定改用实体调音器；调音功能、琴头界面、拾音算法及相关开发／验收任务已取消，不再返工。历史记录仅作为历史证据，不代表当前功能或待办。

基于 Diatronome 1.0.15（GPL-3.0），保留 LICENSE 与源码作者署名。节拍使用连续 PCM 采样时钟。

构建：JDK17、Gradle8.12、Android SDK36、Build Tools36.1.0。

```sh
bash scripts/run-v02-probe.sh
gradle :app:assembleRelease :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Actions `TuneBeat-build` 提供未签名 APK，使用单独保存的固定私钥签名交付；私钥不进仓库或 CI。`Tool-verification` 保存测试与截图。手机节拍实效与自动验证分别记录。
