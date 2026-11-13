# 个人测试安装包

[下载 OpenHOYI 77 扩展 V8 APK](https://github.com/wwh0188/future68/raw/refs/heads/feature/control-modes-curve-notes/downloads/OpenHOYI-77-Personal-v8.apk)

私有仓库需登录有权限的 GitHub 账号；如打开文件详情，选择下载原始文件。

- 版本：`0.2.0-personal.8`，versionCode 112；Android 8.0 及以上。
- 应用名：OpenHOYI 77 扩展；包名：`io.openhoyi.mobile.personal`；沿用原个人测试证书，可覆盖升级 V1～V7，不要卸载或清数据。
- 功能对齐 dev111：工作室选曲准备温度及进度、单次安全离页取消；独立小中大字号与表单重建；近方窗布局；实时曲线多选压力/累计注水/流速/温度/净出液重。
- 净重区分确认与 ≈ 估计，仅显示/记录，不用于定量停止。补修物理秤归零的跨坐标连线，旧 raw 样本保留，净重旁车可选且哈希绑定。
- 固件1.1.3可在原门禁下发送受支持拨杆配方，传输不等于机内持久保存确认；未审查 FLOW 组合仍只允许1.1.4/1.1.5。
- 保留 ART 联动、四种拨杆模式、曲线说明、补偿/隐藏段与个人 schema1～4、删除撤销及全部此前修复。个人更新继续通过 GitHub 浏览器入口，APK 无 REQUEST_INSTALL_PACKAGES 权限。
- APK 大小：18,822,022 字节；SHA-256：`818e7db15b18cb159448caa7a1a9b649e559f053cb42c023473794ebee4ffb6b`。
- 构建源码：`f4894e65a0ea7661bcdfc4ecbacba6dc334f9d82`；后续交付提交只增加 APK、下载说明、构建记录和校验文件，不宣称可重现二进制源码认证。
- [build-record-v8.json](build-record-v8.json)：协议39,862项检查/32,856条通知回放、session807项、mobile724项（1项原有跳过）、BLE9项，无失败；构建、lint、Lab Kotlin和Mock AndroidTest编译通过。实际APK的3,310项编译字符串比对通过。
- 未实际安装或执行窗口/机器验收；字号实际显示、预热/停水、FW113持久化及用户数据保留仍未进行真机验证。

[安装与使用说明](../PERSONAL_BUILD.md) · [V8 跟进记录](../docs/evidence/personal-dev111-audit.md) · [完整源码](../) · [许可证](../LICENSE)

历史归档：[V1 APK](OpenHOYI-77-Personal-v1.apk) · [V2 APK](OpenHOYI-77-Personal-v2.apk) · [V3 APK](OpenHOYI-77-Personal-v3.apk) · [V4 APK](OpenHOYI-77-Personal-v4.apk) · [V5 APK](OpenHOYI-77-Personal-v5.apk) · [V6 APK](OpenHOYI-77-Personal-v6.apk) · [V7 APK](OpenHOYI-77-Personal-v7.apk)。V2 有已知启动问题；不通过低版本降级或卸载恢复新格式数据。本次只通过 GitHub 交付。
