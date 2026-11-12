# 个人测试安装包

[下载 OpenHOYI 77 扩展 V6 APK](https://github.com/wwh0188/future68/raw/refs/heads/feature/control-modes-curve-notes/downloads/OpenHOYI-77-Personal-v6.apk)

如果打开的是文件详情，选择下载原始文件。私有仓库需要登录有权限的 GitHub 账号。

- 版本：`0.2.0-personal.6`，versionCode 87；Android 8.0 及以上。
- 应用名：OpenHOYI 77 扩展；包名：`io.openhoyi.mobile.personal`。
- 可覆盖升级个人 V1～V5，保持原包名和原测试签名；不要卸载或清数据。
- 对齐 dev85（#60）：修复首页/实时萃取暗色背景、温度仪表盘、快捷卡片、操作区和末端读数配色；保留浅色原值及曲线交互。
- 对齐 dev86（#61）：支持 Acaia 已知重量/计时组合记录，修复部分 Lunar 的同步超时；完整校验后更新重量，不支持的编码和未知/截断记录继续拒绝，不扩大 Lunar/AP 控制权限。
- 保留 ART、拨杆、曲线说明、补偿/隐藏段保真和个人 schema1～4，以及 V5 的锅炉回读、首次连接、直接启动/休眠、低温预热与运行重量进度。固件支持范围不变，新 FLOW 组合须机器确认 1.1.4 或 1.1.5。
- FLOW 首段可用 0mL＋正秒数计时，流量最低 1mL/s；不保证 0bar。
- APK 大小：18,374,420 字节；SHA-256：`82f186fddd01f09c6de4dca25478b0e3dbedbc5299590fa979a93bbeb3307844`。
- 构建源码：`5b1751811a8f69cc25418a7402f50a4b732933c7`；后续交付提交仅增加 APK、下载记录和校验文件，不宣称可重现二进制认证。
- 测试与身份核验见 [build-record-v6.json](build-record-v6.json)：相关单测无失败，构建、lint、Lab 和 Mock AndroidTest 编译通过，实际 APK 的 3,040 项编译字符串比对通过。未进行真实安装、界面操作或机器验收。

[安装与使用说明](../PERSONAL_BUILD.md) · [V6 跟进记录](../docs/evidence/personal-dev86-audit.md) · [V5 跟进记录](../docs/evidence/personal-dev84-audit.md) · [完整源码](../) · [许可证](../LICENSE)

历史归档：[V1 APK](OpenHOYI-77-Personal-v1.apk) · [V2 APK](OpenHOYI-77-Personal-v2.apk) · [V3 APK](OpenHOYI-77-Personal-v3.apk) · [V4 APK](OpenHOYI-77-Personal-v4.apk) · [V5 APK](OpenHOYI-77-Personal-v5.apk)。V2 有已知启动问题；不通过低版本降级或卸载恢复新格式数据。本次只通过 GitHub 交付。
