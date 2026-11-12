# 个人 V6：对齐 dev85／dev86

- 需求与范围：采纳 dev85（#60）首页/实时萃取暗色配色修复，以及 dev86（#61）Acaia 已知重量/计时组合记录解析；保留个人 ART、拨杆、说明编辑、schema4 与 V5 的锅炉/预热/直接启动行为。
- 来源分支/基线：用户私有 `wwh0188/future68` 的 `feature/control-modes-curve-notes`，已核对与本地 V5 交付提交 `4229435800a99ae556e123449f1965780ca04041` 一致，起始工作树干净。
- 工作分支：`fix/dev86-personal-updates`，从上述个人交付基线建立；不替换成上游 develop 的整套个人不兼容模型。
- 固定上游：dev85 解引用提交 `b8cf9869051e23892990acf631227b5ef9376ff2`；dev86 解引用提交 `00330680d68d0d9f65315d9f6371a63c3df7b411`。只采纳相对 dev84 `7fbfcbea69da126b2361c48fcf88e1d6acf80b07` 的相关源码/测试补丁；发布 API 元数据不代表已下载或重现上游二进制。
- 分工：实际 Sol `dev86_impl` 独占生产源码及测试，实际 Astra `dev86_review` 只读初审和最终独立复核；根线程负责规划/脱敏证据/说明、集中构建、身份核验与 GitHub 交付。不并行编辑相同文件或运行 Gradle。
- 改动文件与步骤：配色涉及 HomeFragment、BoilerTemperatureDial、ExtractionActivity/Fragment、相关 visual style、Overlay 与末端标签；协议仅 AcaiaCodec 的 `0x0C` 事件链，补齐协议、会话、BLE 路径和暗色 AndroidTest 回归。保留个人 MobileService/曲线模型/存储/适配器/指纹，不更改签名或依赖。
- 协议边界：仅依据独立公开布局处理 tags 5/6/7/8/11，完整记录链成功后才发布最后重量；未知边界不猜，已知截断、无效小数位及不支持的非零高字节拒绝，不放宽单位白名单。电池/计时/按键/ACK 只跳过，不推断去皮、停止或控制成功；Lunar/AP 控制权限不因解码扩展而增加。回归使用公开布局自主构造数据，不复制用户原始诊断。
- 体验/数据边界：浅色值保持原样，暗色统一主题色；不改变曲线选择、按钮门禁、运行中目标重量进度或四种拨杆模式。保留个人 schema1～4、说明、补偿/隐藏段、ART 旧 ID、只读绑定及未知结果记录；不增加手动历史转配方或固定泵输出功能。
- 验证证据：集中执行 protocol/session checks、BLE/mobile JVM 回归、个人 APK 构建、lint、Lab Kotlin 和 Mock AndroidTest 编译；再对实际 APK 验签、核对包名/递增版本/非 Mock、编译中文/英文资源，并从 GitHub 重新读取文件校验哈希。相关检查通过后停止扩大测试，不把 AndroidTest 编译当执行。
- 推送/PR目标：持续授权仅针对个人仓库工作分支，普通推送 fix 分支，用户原工作分支仅快进更新；不直推 main/develop，不强推、不重写共享历史，不创建未经验证的上游 PR。
- 发布：个人 V6，拟 versionCode 87、versionName `0.2.0-personal.6`，包名 `io.openhoyi.mobile.personal` 与原测试签名保持一致；先冻结源码，再添加 APK/构建记录/校验文件。只通过 GitHub 交付，不打标签或创建正式 GitHub Release，不更新其他下载渠道；不安装、不清数据、不操作机器。
- 回退：保留 V1～V5 安装包与记录，遇到问题追加递增修复版本；不以卸载/降级/清库或删除未知结果替代恢复。
- 状态：实现与 Astra 最终独立复核完成，集中 Gradle 验证通过。协议 39,848 项检查/32,856 条既有通知回放；session 773 项、mobile 551 项（1 项原有跳过）、BLE 9 项，无失败；个人 APK、lint（0 error，457 warning，与 V5 相同）、Lab Kotlin 和 Mock AndroidTest 编译通过。语言目录完整、生成文件无漂移。接下来冻结源码并对实际 APK 和 GitHub 文件做核验；实际安装、屏幕和设备验收未进行。

遵循 [Git 工作流](../git-workflow.md)；本次个人扩展授权沿用对话中的持续范围。
