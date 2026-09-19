> 历史记录：2026-09-19 用户取消调音功能，改用实体调音器。本文调音／琴头相关实现与未测项均已退出当前范围，不再作为待办；节拍相关记录保留。

# V0.2 来源与选择记录

- 基线：a3322505a-sys/002，9e107f3203343e1f14e28e7de7fcb9b3e5a1f5ad。当前 TuneBeat 页面接入独立的采样时钟播放核心；原 Diatronome 页面源码保留原许可证与兼容接口。
- TarsosDSP：JorenSix/TarsosDSP，41476b268b8b34664d3e43997fb05c95cec29e3d，GPL-3.0。直接使用 McLeodPitchMethod、PitchDetector、PitchDetectionResult，保留头部署名。唯一算法源码改动是把 80 Hz 下限降至 40 Hz，使明显偏低的 E2 也能被测量，不进行目标音八度折算。YIN 仅在 scripts/reference 对照，不进入 App。完整许可见 scripts/reference/TarsosDSP-LICENSE。
- thetwom/Tuner：0b946a227f62f7cbf72c0e8fdd444bb6e88de2ac，阅读 TargetNoteAutoDetection.kt、OutlierRemovingSmoother.kt；借鉴连续证据、目标滞回、异常数据重置机制，未复制 Kotlin 实现。TuneBeat 的短帧状态机独立编写。
- sevagh/pitch-detection：8055f50bd94ffcc9fec4b63ca9a6a087eec9d892，MIT。E2_44100_acousticguitar.txt 是真实声学吉他录音的时域样本；LFS 内容 SHA256 为 900bc85d2cdff64cdac9d5837d03521b0eb3c0f5e019c9ac9ea368d78a54acd6。CI 按提交下载并校验。该片段只有 4095 样本（92.86ms），检测对照明确在末尾补一个零样本（0.023ms）；没有循环扩充，不能验证连续稳定、起音响应或持续丢音率。首次回放因脚本误以为至少有一整窗而主动失败，已修正测试范围为单窗检测，未伪造完整链路通过。来源的 E2 名称仅代表标称音高，不能把相对 82.4069 Hz 的偏移全部认定为算法误差，更不能替代用户电吉他和手机采集链路。
- 琴头首版参考：[Fender American Vintage II 1961 Stratocaster 正面](https://www.thomann.de/be/fender_av_ii_61_strat_rw_owt.htm)，[对应正面图](https://thumbs.static-thomann.de/thumb/padthumb600x600/pics/bdb/_54/548688/20948491_800.jpg)。采用 pre-CBS 小琴头轮廓、单侧六弦钮、第一/第二弦压弦器、木色与金属质感。Android Path 与点击命中共用坐标；未嵌入商品照片。并非用户 Classic Vibe 60s 实琴的精确扫描；有对应照片后才能做精确匹配验收。

## 同输入对照

见 CI 附件 verification/pitch-comparison.csv。每组 126 帧：六弦×七个偏移（0、±5、±10、±25 音分）×三随机种子。全部先量化为 PCM16，比较旧 Envelop＋原门槛、Tarsos YIN .10、MPM .97。五组为纯音、弱音、弱基频强泛音、起音衰减、加噪音。数字只描述固定输入，不泛化为真实手机准确率。

本地 JVM 首次对照：旧门槛将 126/126 弱音丢弃；起音衰减 15/126 无效、9/126 八度错。MPM 在噪声组的 95% 误差约 4.73 音分，YIN 约 9.91，旧实现约 50.66；MPM 起音衰减约 5.74 音分，仍未达到所有场景 5 音分目标。MPM 桌面平均约 7.5ms/4096 帧，YIN 约 3.5ms，旧实现约 0.05–0.18ms。选择 MPM 的依据是这批信号的稳健性，代价是 CPU 消耗。Android 耗时、功耗和真实输入效果单列待测。

## 阈值边界

周期可信度 ≥.95，音量门槛随无效背景估计变化（最小 RMS .0004、最大 .003），严重削波拒绝；3 帧一致后发布（2048 样本 hop），本批起音帧中真值 -10 音分被测为 -4.25，故音准首次进入暂用连续两次稳定且在 ±3 音分内，已进入后超过 ±5 音分退出。偏差较大不输出拧弦方向；无效帧立即撤销调准状态，400ms 无新帧清除读数。参数是首轮可测假设。

纯单频、基频完全缺失且只有单个倍频时，不能单凭声波知道拨的是哪根物理琴弦。当前检测不强行折算；手动锁弦保留真实大偏差，自动模式依赖可观测周期证据。此不可辨识场景和手机实际八度错误不能用合成通过掩盖。
