# 个人 V7：对齐 dev104

- 需求与范围：用户要求从个人 V6（上游 dev86）跟进到 dev104，保留 ART、拨杆模式、曲线说明、个人数据格式和此前修复；构建新的个人 APK，仅经 GitHub 交付。
- 来源分支/基线：用户私有 `wwh0188/future68` 的 `feature/control-modes-curve-notes`，基线 `8e3b3ac449d92193be5887a63b71abe21b2599f8`，起始工作树干净、远端一致。
- 工作分支：`fix/dev104-personal-updates`，从个人交付基线建立；不覆盖为上游整份模型或改写共享历史。
- 固定上游：dev86 `00330680d68d0d9f65315d9f6371a63c3df7b411` 到 dev104 `5a765743a502ad03036a56c517fd211d2878edc5`。已核对 dev105 `e6440ed6ac7f4979295dc09665d8807b11d97f64` 仅更改发布列表提示/脚本，与 dev104 没有 App 功能差异；本次固定功能基线为 dev104。
- 分工：实际 Sol `dev104_transport` 独占 protocol/session/BLE；Sol `dev104_data` 负责个人 store/archive、journal/history、preset/shortcut policy；Sol `dev104_runtime` 负责 Service/Application 与 updater/manifest/build；Sol `dev104_ui` 负责其余 UI 和 AndroidTest；实际 Astra `dev104_review` 初审/最终只读独审；根线程负责资源/语言、说明、集中验证及交付。各组不并行编辑同一文件、不自行运行 Gradle 或提交。
- 改动步骤：分区逐块采纳拨杆配方/自定义快捷、默认关闭的一键萃取、去皮/残帧/较晚开机连接恢复、实时秤/液重与结束冻结、机器计时/采样覆盖、曲线删除撤销、统一开关及扫描提示。保留个人差异，整合后集中编译/回归，修复真实失败后复跑相应检查。
- 协议风险与许可：0x08 仅严格已核对格式、已允许固件、同机新鲜空闲状态和明确确认快照单次发送；GATT 成功只记“最近发送”，不冒称完整回读或机内持久保存。保留未知结果/失败关闭，不自动重发控制。保留 ART 默认/旧别名、其他秤的个人控制边界、去皮后新零/1.5 秒重量新鲜度/至少 7 秒按重规则、精确固件及出队检查；不因导入上游测试放宽 DiFluid/其他秤权限。
- 数据与体验：个人 schema1～4 与上游 configuration schema3 不同；保留多行 description、compensation、inactiveStages、b1/b0 库校验、默认指纹和旧不可执行 FLOW 草稿的可读边界。删除/撤销只归档个人草稿，不删除既有萃取历史，旧库可读取升级且损坏校验不静默写回。保留总量 `max(base, ceil(4×target))`、V5 启动/低温/锅炉和 V6 暗色行为。快捷启动受阻只导航，不保留稍后自动出水意图。
- 个人更新渠道：官方更新索引/原版签名不能作为私有个人包更新。原版更新功能可合入用于原包；个人运行时禁止官方 index 检查、提醒、下载和 installer，仅浏览器打开用户私有 GitHub 的 downloads 页面，不嵌入凭据。个人 Manifest 移除 REQUEST_INSTALL_PACKAGES；不发布或维护其他下载站。
- 验证证据：与改动相称的 protocol/session/BLE/mobile 检查、个人 APK、lint、Lab Kotlin、Mock AndroidTest 编译与语言资源检查；对实际 APK 核验包名/原签名/递增版本/非 Mock、编译字符串，再从 GitHub 重新读取 APK 比对哈希。保留源码声明 `sourceCommitVerified=false`，不将编译或模拟操作当安装/屏幕/实际停水验收。
- 推送/PR目标：沿用个人扩展持续授权，正常推送 fix 分支后仅快进用户工作分支；不向 main/develop 直推，不强推、不移动标签，不创建未验证上游 PR。
- 发布与签名：个人 V7，拟 code105、name `0.2.0-personal.7`，沿用 `io.openhoyi.mobile.personal` 与既有个人测试证书；冻结源码后添加 APK/构建记录/校验文件，不打正式发布标签或创建 GitHub Release。无安装、清数据、AVD或机器操作。
- 回退：保留 V1～V6 的包和记录，问题通过新的递增修复版处理，不以卸载、降级、清库或删除未知记录代替恢复。
- 状态：实现与 Astra 最终独立复核完成。协议 39,862 项检查/32,856 条既有通知回放通过；session 803 项、mobile 675 项（1 项原有跳过）、BLE 9 项，无失败；个人 APK、lint、Lab Kotlin 与 Mock AndroidTest 编译通过。语言目录 1,023 项、生成资源无漂移，25 项语言脚本测试通过。已修正旧固件测试夹具，并补修历史初始化失败时详情点击崩溃。接下来冻结源码并核验实际 APK 与 GitHub 文件；实际设备/屏幕/安装验收未执行。

遵循 [Git 工作流](../git-workflow.md)；个人分支、APK 及 GitHub 交付范围沿用对话中的持续授权。
