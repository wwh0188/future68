# 个人 V4：采纳 dev82 修复与曲线保真

- 需求：用户上传上游 dev82，要求优化当前个人版、查已有问题，并询问流量首段预浸能否无压或类似 Slayer。交付同身份可覆盖的个人测试 APK，保留拨杆、曲线模式和多行说明。
- 来源与分支：客户仓库 `wwh0188/future68` 工作分支 `feature/control-modes-curve-notes` / `e7130d2f86cf67c7c4fa8c87b30d134645d7c281`，新建 `fix/dev82-personal-updates`。不直推 main/develop、不强推、不打发布标签或创建未经本地验证的 PR。
- 参考：`dev-82` / `e7c7ac8aab0cd1accc661c91a6f664dfa0e8a312`，上传 APK SHA-256 `7cee30e402e05fcba62b7e99f5cde9eeba7bf334924f4e7d9bc8461a0acc2c06`，与 GitHub release 对应文件 digest、大小一致。主要采纳 #57；#56 按个人模型缺口局部适配，不整体替换曲线文件。
- 分工：Astra 设计和最终独立只读复核；Sol transport 负责协议/session/BLE 连接与 dispatch 门禁，Sol curve 负责个人曲线数据/UI/测试及局部 curve policy，主线程负责 mobile/app 读数链路、整合、构建及交付。文件归属明确，不并行运行 Gradle。
- 修复范围：统一 ScaleObservation 为实时、采样和历史终重来源；关闭结果不确定时禁止重连/替换；重复 GATT UUID 拒绝歧义；保留 Felicita ART alias、documented 目标支持、只读及窄扫描匹配。保留未知结果不自动重发、前置去皮新零及 7 秒重量门槛。
- 个人问题：复制丢重量补偿和非活动尾段、缩段保存丢隐藏输入、合法 FLOW 首段 0mL+正秒数被误拦。增个人 schema4 保存必要执行参数；旧 schema1/2/个人3、checksum canonical b1/b0、默认参数执行指纹保持兼容；名称/说明不进入执行指纹。新执行参数非默认时参与指纹及信任判定，冻结列表不被外部修改。
- 格式边界：上游 configuration schema3 与个人 description schema3 不同；本轮不导入上游格式，不静默丢字段。保持严格键集并保留原文件。总水量上限仅依据活动段，不因隐藏尾段放宽。
- 预浸边界：当前协议最小首段流量 1mL/s；FLOW 首段切压力最低 0.5bar，没有独立压力上限字段。允许按已证据的 FLOW 范围做 0mL+正计时，不能宣称 0bar、针阀等效或新固件能力。新增流量组合按上游精确规则在 dispatch 要求已知 1.1.4；原压力/工厂路径不误伤。
- 验证：集中回归关闭不确定/回调重入/端点重复、通用读数到终重与过期/断连、旧说明/存储/指纹、补偿/尾段保真、缩扩段、纯计时首段及精确固件限制。所有代码稳定后，一轮受影响模块 checks/JVM tests、个人 APK 构建、lint 与资源核验。新增失败修复后只复跑受影响检查。无设备安装、真实萃取或 AVD。
- 发布与升级：个人测试包 V4，拟 versionCode 83 / versionName `0.2.0-personal.4`，沿用 `io.openhoyi.mobile.personal` 与原个人证书；实际 APK 核验身份、版本、哈希与非 Mock。原作者 dev82 是不同包名，不替换签名、不卸载清数据。参考发布哈希匹配不当作构建源码反向认证或实机验收。
- 交付：按用户后续要求仅通过 GitHub。测试和独审通过后冻结源码、保存 APK/精确构建记录，推送 fix 工作分支，仅快进更新客户默认工作分支；安装包、源码和更新记录集中在该私有仓库，不再更新额外下载站。保留旧包。升级数据保留与实际停水由用户实机核对。
- 回退：追加修复版本，不删除未知记录，不靠降级/清库掩盖问题；schema4 原始文件保留，旧 APK 不承诺能读取新格式。
- 状态：实现和 Astra 独立只读复核完成，包括补偿/尾段、旧库/默认指纹、历史尾段单位、close 未知锁存及发送前固件重验。统一构建验证 `BUILD SUCCESSFUL`：device-session JUnit 744、mobile 543（1 项原有跳过）、BLE 5，0 失败/错误；protocol/session checks、个人包、lintDebug（0 errors／444 warnings）、Lab 与 Mock instrumentation 编译均成功。首轮仅旧测试回调签名编译失败，已修 fixture 后按原范围复跑；未删除保护断言。未运行设备或 AVD，真实安装、停水和低压浸润效果未验收。后续冻结源码、核验实际 APK 并只通过 GitHub 交付。

遵循 [Git 工作流](../git-workflow.md)。
