# 个人测试安装包

[下载 OpenHOYI 77 扩展 V5 APK](https://github.com/wwh0188/future68/raw/refs/heads/feature/control-modes-curve-notes/downloads/OpenHOYI-77-Personal-v5.apk)

如果打开的是文件详情，选择下载原始文件。私有仓库需要登录有权限的 GitHub 账号。

- 版本：`0.2.0-personal.5`，versionCode 85；Android 8.0 及以上。
- 应用名：OpenHOYI 77 扩展；包名：`io.openhoyi.mobile.personal`。
- 可覆盖升级个人 V1～V4，保持原包名和原测试签名；不要卸载或清数据。
- 采纳 dev84（#58／#59）：修复锅炉回读卡住、首次连接偶发等待；明确点击后直接开始萃取或休眠，低温仍提示预热，防重复和去皮/秤重保护继续保留。
- 停止方式说明放在开始按钮附近，运行中保留目标重量进度；预热显示更准确。显式支持机器固件 1.1.5，不更新机器固件；新 FLOW 组合须机器确认 1.1.4 或 1.1.5。
- 保留 ART、拨杆、曲线说明和 V4 的通用秤记录、补偿/隐藏段保真及个人 schema1～4。上游 configuration schema3 仍明确拒绝。
- FLOW 首段可用 0mL＋正秒数计时，流量最低 1mL/s；不保证 0bar。
- APK 大小：18,373,404 字节；SHA-256：`90b65e61462f8d2c5eba70a03f5c9666e3a0ea843d0b1606b61e0e0f98a8398f`。
- 构建源码：`9dfa4d415c0a564d4fdbaa1f40ea1fda11953e63`；后续交付提交仅增加 APK、下载记录和校验文件，不宣称可重现二进制认证。
- 测试与身份核验见 [build-record-v5.json](build-record-v5.json)：相关单测无失败，构建、lint、Lab 和 Mock AndroidTest 编译通过，实际 APK 的 3,040 项编译字符串比对通过。未进行真实安装、界面操作或机器验收。

[安装与使用说明](../PERSONAL_BUILD.md) · [V5 跟进记录](../docs/evidence/personal-dev84-audit.md) · [V4 修复与预浸说明](../docs/evidence/personal-dev82-audit.md) · [完整源码](../) · [许可证](../LICENSE)

历史归档：[V1 APK](OpenHOYI-77-Personal-v1.apk) · [V2 APK](OpenHOYI-77-Personal-v2.apk) · [V3 APK](OpenHOYI-77-Personal-v3.apk) · [V4 APK](OpenHOYI-77-Personal-v4.apk)。V2 有已知启动问题；不通过低版本降级或卸载恢复新格式数据。本次只通过 GitHub 交付。
