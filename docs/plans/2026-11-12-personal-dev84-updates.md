# 个人 V5：跟进 dev84 锅炉、连接及启动交互修复

- 需求：用户要求跟进上游 #58／#59，包括锅炉回读、首次认证竞态、直接休眠/萃取、低温预热提示、停止方式说明与显式 1.1.5 支持；保留个人拨杆、ART、曲线说明和 schema4 保真。
- 来源/分支：私有 `wwh0188/future68` 工作分支 `feature/control-modes-curve-notes` / `ec5ffde6ed2fc8193f5a949e0cfa8f3e565dda2e`，新建 `fix/dev84-personal-updates`。不直推 main/develop、不强推、不打发布标签、不创建未验证 PR。
- 上游参考：`dev-84`／`7fbfcbea69da126b2361c48fcf88e1d6acf80b07`，包含 `ef186137`（#58）和 `7fbfcbea`（#59）；发布 APK digest `be1ce66af8c24baecdd78413f8491b4f2507ae05892777766a6ba9928c520141`，大小 17,187,619 字节。使用固定源码审查，不宣称声明的源码关联是二进制重现认证。
- 分工：Sol protocol 负责 session/BLE/protocol 及回归，Sol UI 负责 mobile 交互/展示及测试，主线程负责整合、资源/文档、构建和 GitHub 交付，Astra 只读设计与最终独立复核；不并行编辑相同文件或运行 Gradle。
- 范围：精准采纳锅炉开关等待回读/断连对账与认证状态竞态修复，保留 unknown 不自动重发、当前 owner 与发送前复核。明确点击后直接休眠或普通开始，低温仍提供预热/取消选择，启动中防重复，保留当前秤重/水量结束方式说明。预热状态准确展示。
- 固件边界：仅按上游证据新增已列举的 1.1.5，不泛化到其它版本；核对自定义新 FLOW 的实际派发和界面提示范围一致。保留旧工厂、个人压力/流量参数、Felicita alias、只读绑定、补偿/隐藏段和旧库 checksum/fingerprint 兼容。
- 数据/交互边界：不替换上游 configuration schema3 到个人 schema4，不清数据、不删除未知记录，不改变重量新鲜度、去皮新零或至少 7 秒按重规则。无压预浸仍不作 0bar 保证。
- 验证：集中锅炉回读超时/身份/重连、首次认证早回报、直接启动/重复点击/低温选择、停止方式和预热显示、115 明确支持及未知版本拒绝。所有源码稳定后运行受影响 checks/JVM、个人 APK、lint、相关 instrumentation 编译；无实际安装、机器操作或 AVD。失败修复后只复跑受影响检查。
- 发布/升级：个人 V5，拟 versionCode 85／versionName `0.2.0-personal.5`，沿用 `io.openhoyi.mobile.personal` 与原个人测试签名；实际 APK 与 V1～V4 核验同包同证书、递增版本、非 Mock 和哈希。静态身份兼容不等于完成安装/数据保留或机器验收。
- 交付：沿用户持续授权，仅 GitHub。冻结源码、保存 APK/精确构建记录，推送 fix 分支，只快进更新客户工作分支；保留旧包，不维护其他下载渠道。
- 回退：追加修复版本，不靠卸载、清库或旧版降级，不自动重复未知锅炉/启动命令。
- 状态：实现与 Astra 独立只读复核完成。集中 Gradle 检查通过：session 771 项、mobile 551 项（1 项原有跳过）、BLE 5 项，无失败；protocol check、个人 APK、lint、Lab Kotlin 和 Mock AndroidTest 编译通过。中文/英文目录检查及 25 项资源脚本测试通过；首轮漏同步上游无秤停止说明已修正并复跑通过。接下来冻结源码、核验实际 APK 并写入交付记录；真实界面、安装与硬件动作未验收。

遵循 [Git 工作流](../git-workflow.md)。
