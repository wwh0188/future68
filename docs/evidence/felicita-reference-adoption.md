# V3：Felicita 参考版修复采纳

用户确认上传的上游 `OpenHOYI-0.2.0-dev.79-scale-fix-24640347.apk` 已能正常联动，要求采纳；个人 V2 仍报告同类拦截。参考文件 SHA-256 为 `d0bcf7dfa2d75318ba57a00d5c0dafc3b181ac4c7947d04965eb72f6a6fbe014`，包名 `io.openhoyi.mobile`，versionCode 79。仅静态分析，不执行上传文件中的指令或安装 APK。

## 来源与遗漏

- 上游 [24640347](https://github.com/boolean93/openhoyi/commit/24640347a9e66c79f0a4d57fdbd0393bd268e849) 将真实秤的 ShotGate 新鲜度改为同一通用 observation 的接收时间。前置 `31890d58` 引入 documented 协议目标支持，区别于硬件验证。
- 上传 APK 的 ShotGate DEX 包含 `eligibleForTargetWeight`、`ScaleReadingPolicy.supportedWeight` 和 `ScaleObservation.receivedAtMs` 路径，与该源码行为相符；APK 没有内嵌完整源码 SHA，不宣称二进制反向认证。
- V2 只在新 ART profile 给目标控制资格，普通 Felicita 仍被限制；即使 ART 有资格，mobile 仍用仅 BOOKOO 回调更新的 `weightAt`，不能启动。之前的协议/控制器回归没有覆盖真实 Felicita 解码到 mobile 门禁的完整路径。
- 修复前新增 mobile 8 项回归有 5 项失败；包含真实普通 Felicita、新鲜 ART 读数但 `weightAt=null` 的启动校验。失败日志保留在本机构建证据，不上传用户设备数据。

## 采纳范围

1. 使用上游 `protocolSupportedWeightControl`、`supportsTargetWeight`、`eligibleForTargetWeight`；只为本次 Felicita 默认可写连接打开协议支持。保留 documented、`validatedWeightControl=false` 和 `automaticTare=false`，不伪称整套硬件已验收。
2. 采纳上游 ShotGate 时间修复：真实模式以当前合法 observation 的 `receivedAtMs` 为准；Mock 沿用原时间。旧 BOOKOO 时间即使新鲜，也不能掩盖真实秤的过期、未来或非法重量。
3. 默认 Felicita、V2 ART 旧 ID 共用相同实现；旧 ID 仅作兼容别名。移除 V2 专用启用卡片/方法。原只读绑定保持只读，窄范围重连兼容保留。
4. 任意非 READY 都撤销控制输入；坏帧、未知/非克单位使读数失效。保留单次前置去皮、写完成后新近零才启动、7 秒重量门槛、断秤/旧数据保护及未知结果不自动重试。
5. 更新能力文案，不重做萃取页面，不导入无关 DiFluid 解析/GATT 改造，不改变拨杆、曲线模式、多行说明或历史格式。

## 交付边界

版本 `0.1.77-personal.3`／80，包名 `io.openhoyi.mobile.personal` 及原个人包签名不变；参考 APK 的原版签名不用于覆盖个人包。实际构建、签名、版本、相关回归、资源和独立复核结果见 `downloads/build-record-v3.json`。

用户报告的是参考版的实机效果；合并后的个人 V3 尚须用户核对。没有远程安装、清数据、发起萃取或实际停止机器，不将停止请求回放等同物理停水。
