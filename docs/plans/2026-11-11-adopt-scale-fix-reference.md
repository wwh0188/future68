# 采纳用户实测通过的 dev-79 Felicita 修复

- 目标：用户反馈个人 V2 仍不能启动目标重量曲线，上传已实测解决的 `OpenHOYI-0.2.0-dev.79-scale-fix-24640347.apk`，要求直接采纳相关实现。保留原个人包的拨杆模式、曲线模式及多行说明功能。
- 来源/分支：客户私有仓库 `wwh0188/future68` 的工作分支 `feature/control-modes-curve-notes` / `fbd4a092`；新建 `fix/felicita-scale-reference`。不直接推送 main/develop、不强推、不创建发布标签或未经检查的 PR。
- 参考：用户 APK SHA-256 `d0bcf7dfa2d75318ba57a00d5c0dafc3b181ac4c7947d04965eb72f6a6fbe014`；GitHub 上游确切提交 `24640347a9e66c79f0a4d57fdbd0393bd268e849` 及前置 `31890d58`。已核对上传 APK 的 ShotGate DEX 包含通用读数时间和目标资格路径；不宣称 APK 可反向认证完整源码提交。
- 根因：V2 普通 Felicita 仍被能力门禁限制，且启动检查继续依赖 BOOKOO 专属 `weightAt`；Felicita 的通用 `scaleObservation.receivedAtMs` 新鲜也无效。上一轮协议/控制器测试未串通实际 mobile 门禁。
- 设计/分工：Astra 只读设计和最终独立审查，Sol 实现 protocol/session/BLE，主线程负责 mobile、回归、构建和交付。协议与 mobile 文件互不重叠。
- 最小改动：采纳上游 protocolSupportedWeightControl / supportsTargetWeight / eligibleForTargetWeight，使普通可写 Felicita 保持 documented、非 hardware-validated、无自动去皮也支持目标；ShotGate 真实模式只用合法通用 observation 时间，Mock 保留旧时间；任意非 READY 撤销控制输入；保留对未知帧失效的加固。撤除 V2 ART 启用卡片，旧 ART id 作为默认协议兼容别名，不迁移/清除偏好。只读绑定继续只读。
- 范围边界：不引入无关 DiFluid 的单位/动态 GATT 增量，不重写曲线/拨杆页面，不修改 1.5 秒新鲜度、7 秒重量停止门槛、去皮后新零值或未知结果不重发的政策。更正电子秤能力提示。
- 验收：先用真实 Felicita/ART 解码且 `weightAt=null` 的 mobile 用例复现失败；修复后同用例通过。集中覆盖旧时间不能掩盖过期/未来/非法读数、只读/离线/无去皮不获资格、目标 0 与 Mock、默认 Felicita 从前置去皮到按重停止请求、非 READY 和断连/过期保护、旧 ART id 兼容。复用既有拨杆/曲线回归，执行受影响模块检查、个人包构建和相关 lint，不增加无关矩阵或 AVD。
- 签名/升级：包名仍为 `io.openhoyi.mobile.personal`、原个人测试证书不变，versionCode 80 / versionName `0.1.77-personal.3`。核验实际 APK 与 v1/v2 的升级身份；不以用户参考 APK 的原版包名/签名替换个人身份，不卸载或清数据。
- 推送/交付：用户持续授权个人测试 APK 与 GitHub 交付；验证和独审通过后推送 fix 分支，只快进更新客户默认工作分支，归档 v3 APK/准确源码记录，更新原私有下载页，保留旧包但不推荐未解决的 V2。
- 实机边界/回退：用户报告参考版已能用，合并后的个人包仍须实机验收。不开机、不发起萃取、不远程安装，不删除未知记录；问题通过新版本修复，不以低版本覆盖或卸载恢复。
- 状态：红阶段 8 项中 5 项失败；实现后首轮 mobile 单测/构建/lint 全部成功，关键 8 项全绿。protocol/session/BLE 检查通过（会话 JUnit 736、Session103、BLE4；协议 39844 项及 32856 条通知回放）。Astra 已串通真实 adapter 到 UI/服务/Controller 链路并完成独立复核；指出的 3 处扫描旧提示已修正，当前只复跑受影响的 mobile 检查。冻结源码后核验实际 APK 身份并交付；不将参考版的用户实机结果当作个人 V3 已验收。

遵循 [Git 工作流](../git-workflow.md)。
