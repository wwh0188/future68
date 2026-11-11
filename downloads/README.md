# 个人测试安装包

[下载 OpenHOYI 77 扩展 V4 APK](https://github.com/wwh0188/future68/raw/refs/heads/feature/control-modes-curve-notes/downloads/OpenHOYI-77-Personal-v4.apk)

如果打开的是文件详情，选择下载原始文件。私有仓库需要登录有权限的 GitHub 账号。

- 版本：`0.2.0-personal.4`，versionCode 83；Android 8.0 及以上。
- 应用名：OpenHOYI 77 扩展；包名：`io.openhoyi.mobile.personal`。
- 可覆盖升级个人 V1／V2／V3，保持原包名和原测试签名；不要卸载或清数据。
- 采纳 dev82 的通用秤实时/采样/历史终重与关闭不确定保护，保留 ART、拨杆和曲线说明功能。
- 修复复制曲线丢补偿、隐藏段参数及纯时间首段误拦。新非默认补偿/尾段使用个人 schema4，旧个人 schema1/2/3 可继续读取；上游 configuration schema3 仍明确拒绝。
- FLOW 首段可用 0mL＋正秒数计时，流量最低 1mL/s；不保证 0bar。新流量组合须机器确认固件 1.1.4。
- APK SHA-256：`d5ceece547196d2737b697c91c24bb90d9df3c692e35fcf392482119f0bfb2e5`。
- 构建源码：`fadfd744c16688ee08a00a62ffa4997c93394749`；交付提交仅增加 APK、下载记录和校验文件。
- 测试与身份核验见 [build-record-v4.json](build-record-v4.json)；相关单测、构建、lint、Lab/Mock instrumentation 编译通过，未进行真实安装、界面操作或机器验收。

[安装与使用说明](../PERSONAL_BUILD.md) · [V4 修复范围与预浸说明](../docs/evidence/personal-dev82-audit.md) · [完整源码](../) · [许可证](../LICENSE)

历史归档：[V1 APK](OpenHOYI-77-Personal-v1.apk) · [V2 APK](OpenHOYI-77-Personal-v2.apk) · [V3 APK](OpenHOYI-77-Personal-v3.apk)。V2 有已知启动问题；不通过低版本降级或卸载恢复新格式数据。本次只通过 GitHub 交付。
