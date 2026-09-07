# 调音节拍 · TuneBeat

独立 Android 调音／节拍 App，当前为 **P0 复用验证基线**。

- 来源：Diatronome 1.0.15，GPL-3.0，保留原作者及算法署名。
- 本阶段保留上游界面与功能，用于对照试装；中文新 UI 属于 P1/P2。
- 安装名：调音节拍 P0；包名：`io.github.a3322505a.tunebeat`。
- 版本：`0.0.1-p0` / versionCode `1`。与原 Diatronome 并存。
- 使用固定私有签名，后续原型保持签名与包名，递增版本覆盖安装。
- 无新增联网权限、广告或后台统计。

构建：JDK 17、Gradle 8.12、Android SDK 36、Build Tools 36.1.0。

```sh
gradle :app:assembleRelease
```

上游未提交可执行 Gradle wrapper，本项目目前用 CI 固定 Gradle 版本。
CI 未签名 APK 位于 Actions 的 `TuneBeat-P0-build` artifact，附源码 SHA 与校验和。由维护者本地使用单独保存的固定私钥签名后提供可安装 APK；私钥不进入仓库或 CI。

详见 [P0 复用结论与验证记录](docs/P0-验证记录.md) 和 [上游说明](docs/UPSTREAM_README.md)。
