# 个人测试安装包

[下载 OpenHOYI 77 扩展 V7 APK](https://github.com/wwh0188/future68/raw/refs/heads/feature/control-modes-curve-notes/downloads/OpenHOYI-77-Personal-v7.apk)

私有仓库需登录有权限的 GitHub 账号；如打开文件详情，选择下载原始文件。

- 版本：`0.2.0-personal.7`，versionCode 105；Android 8.0 及以上。
- 应用名：OpenHOYI 77 扩展；包名：`io.openhoyi.mobile.personal`；沿用原个人测试证书，可覆盖升级 V1～V6，不要卸载或清数据。
- 功能对齐 dev104：拨杆配方最近发送记录、自定义快捷槽位、默认关闭的一键萃取、实时重量/本杯液重、历史机器计时与采样覆盖区分、曲线删除撤销、统一开关和扫描/连接/残帧修复。dev105 仅改变上游发布列表提示，App 功能相同。
- 保留 ART 联动、四种拨杆模式、曲线说明、补偿/隐藏段与个人 schema1～4。删除归档使用新的库容器，旧 APK 不保证能读取，不通过降级恢复新数据。
- 拨杆配方单次发送须确认；最近发送不代表完整机内回读或持久保存确认，目标液重不保存到机器。现有低流量预浸仍不保证 0bar。
- 个人设置页通过浏览器打开 GitHub 下载页；不使用原版官方更新索引/下载/安装器，APK 已移除 REQUEST_INSTALL_PACKAGES 权限。
- APK 大小：18,766,818 字节；SHA-256：`45f07347467159b4d4da85c23d26c73891f8e76f42f07becdfb247ea92e887a9`。
- 构建源码：`315151e7888f8e429ac4dac6efb810b788b878ff`；后续交付提交仅增加 APK、下载说明、构建记录和校验文件，不宣称二进制源码重现认证。
- [build-record-v7.json](build-record-v7.json)：协议 39,862 项检查/32,856 条通知回放、session 803 项、mobile 675 项（1 项原有跳过）、BLE 9 项，无失败；构建、lint、Lab Kotlin 和 Mock AndroidTest 编译通过。实际 APK 的 3,232 项编译字符串比对通过。
- 未进行实际安装、屏幕、机器停水或拨杆配方持久性验收；静态同包/同签名/递增版本不等于实际数据保留验证。

[安装与使用说明](../PERSONAL_BUILD.md) · [V7 跟进记录](../docs/evidence/personal-dev104-audit.md) · [完整源码](../) · [许可证](../LICENSE)

历史归档：[V1 APK](OpenHOYI-77-Personal-v1.apk) · [V2 APK](OpenHOYI-77-Personal-v2.apk) · [V3 APK](OpenHOYI-77-Personal-v3.apk) · [V4 APK](OpenHOYI-77-Personal-v4.apk) · [V5 APK](OpenHOYI-77-Personal-v5.apk) · [V6 APK](OpenHOYI-77-Personal-v6.apk)。V2 有已知启动问题；不通过低版本降级或卸载恢复新格式数据。本次只通过 GitHub 交付。
