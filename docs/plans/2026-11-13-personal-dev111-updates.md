# 个人 V8：对齐 dev111

- 需求与范围：在个人 V7 上采纳 dev104 到 dev111 的 App 更新，保留 ART、拨杆四模式、多行说明、私人曲线/归档格式和 GitHub 个人更新渠道；构建新的同身份个人 APK。
- 来源分支/基线：私有 `wwh0188/future68` 的 `feature/control-modes-curve-notes`，基线 `706153f1d0766310c47f0396f214262a092e8644`；起始干净，远端一致。
- 工作分支：`fix/dev111-personal-updates`，从个人交付基线建立；不覆盖整份上游私有不兼容模型。
- 固定上游：dev111 `0d64b9af1008554efc1c0faffa9ea6d220ca6c5c` 相对 dev104 `5a765743a502ad03036a56c517fd211d2878edc5`，包括 #82～#87；发布 API 声明 APK 为 17,521,619 字节、SHA-256 `316952d4fad1a860b0db28ab2ca2c06aded4183de96be2f0977c6690b0554ccb`。本轮不下载上游 APK、不宣称二进制源码重现认证。
- 分工：实际 Sol `dev111_runtime` 负责 session/Service/净重与样本模型及对应测试；Sol `dev111_ui` 负责字号、布局、预热展示与曲线多选、MobileApplication/MainActivity 和 AndroidTest；实际 Astra `dev111_review` 初审/最终只读独审；根线程资源/语言、文档、集中检查与 GitHub 交付。不并行编辑同一文件，不由子任务运行 Gradle 或提交。
- 改动步骤：合入工作室选曲自动准备温度/进度及安全离页取消、精确 FW113 拨杆发送、近方窗比例、独立小中大字号与表单重建、多选实时指标及净出液重显示；集中核对所有私人门禁和状态，相关验证后冻结源码、实际 APK 验签及 GitHub 回下载。
- 自动预热边界：只由工作室明确使用曲线的意图触发；温度就绪不等于自动开始，受阻快捷不保留延迟出水意图。离页仅在同连接/代次和安全机器状态允许时尝试单次取消，不在重连后补发。旋转/字号等配置重建不重启 BLE、不重发或取消既有预热，保存未提交输入、焦点、选区及可恢复滚动。
- 协议与数据边界：FW113 只加入 stored-lever 的精确许可集合，依据官方客户端既有格式，不冒充有 113 固件二进制/持久性证明；FLOW 精确谓词仍仅 114/115，原工厂/压力路径保留。净出液重估计仅显示/记录，确认与估计来源区分，连接/坐标变化断线分段，不用于去皮、停泵或控制许可。旧样本缺字段不伪造。
- 私人保留：不替换 schema1～4、description、compensation、inactiveStages、b1/b0 校验、fingerprint、归档 wrapper1/2、总量 max(base,ceil4×target) 或固件政策。保留 ART 默认/旧别名、其他秤的控制边界、新零/1.5 秒/7 秒、sticky close、V5/V6/V7 修复与 private update 全链禁官方渠道/个人 Manifest 权限移除。私人原无无目标延迟自动去皮，本轮不因展示算法注释新增动作。
- 验证：protocol/session/BLE/mobile 相关检查、个人 APK、lint、Lab Kotlin 和 Mock AndroidTest 编译、语言完整性/生成漂移及脚本测试；实际 APK 同包同签/递增版本/非 Mock/编译中文英文/无 REQUEST_INSTALL_PACKAGES 权限；GitHub 文件回读哈希。静态守卫、JVM、编译与实际屏幕/设备证据分别记录，不机械增加无关矩阵。
- 推送/PR目标：正常推送 fix 分支，用户工作分支只快进；不向 main/develop 直推、不强推/移动标签，不创建未经验证上游 PR。
- 发布与签名：个人 V8，拟 code112、name `0.2.0-personal.8`；沿用 `io.openhoyi.mobile.personal` 和原个人测试证书。冻结源码后只添加 APK/构建记录/下载校验文件，仅 GitHub，不发正式标签/Release、不安装、不清数据、不操作机器或启动 AVD。
- 回退：保留 V1～V7，追加更高版本修复，不以卸载/降级/清库或丢弃未知记录替代恢复。
- 状态：实现及 Astra 最终独审完成，无剩余阻断。协议 39,862 项检查/32,856 条既有通知回放、session 807 项、mobile 724 项（1 项原有跳过）、BLE 9 项通过，无失败；个人 APK、lint、Lab Kotlin 与 Mock AndroidTest 编译通过。语言目录 1,043 项完整、生成资源无漂移，25 项语言脚本测试通过；新增显示估计器物理归零断段回归。接下来冻结源码、核验实际 APK 与 GitHub 文件；实际设备/屏幕/安装验收未执行。

遵循 [Git 工作流](../git-workflow.md)；个人扩展持续授权沿用对话范围。
