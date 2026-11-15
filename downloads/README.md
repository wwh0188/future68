# 个人测试安装包

[下载 OpenHOYI 77 扩展 V9 APK](https://github.com/wwh0188/future68/raw/refs/heads/feature/control-modes-curve-notes/downloads/OpenHOYI-77-Personal-v9.apk)

私有仓库需登录有权限的 GitHub 账号；如打开文件详情，选择下载原始文件。

- 版本：`0.2.0-personal.9`，versionCode 128；Android 8.0 及以上。
- 应用名：OpenHOYI 77 扩展；包名：`io.openhoyi.mobile.personal`；沿用原个人测试证书，可覆盖升级 V1～V8，不要卸载或清数据。
- 累计对齐 dev111→117→123→127。前面的117/123中间改动已一起纳入，无需依次安装。
- 新曲线编辑页：实时预览、卡内模式、固定保存按钮；已有曲线另存新副本，原配方保留。多行说明、补偿、隐藏段和分别记忆的压力/流量输入继续保留；隐藏无效输入不会悄悄回退旧值。
- 自定义曲线筛选、蓝绿模式控件、待用配方提示；修复过期设置同步与模式回读，保留 ART 联动、目标进度/停止方式、字号和此前预热/历史修复。
- 设置增加日志管理与个人截图导入教程；固定图轴默认关闭。一键萃取未保存偏好时默认开启，之前明确关闭的保持关闭。
- 精确固件1.1.3/1.1.4/1.1.5的完整模式仍须通过参数、owner、机器状态和出队检查；传输成功不证明机内配方持久保存。首段低流量预浸不保证0bar。
- 个人更新仍只走GitHub浏览器入口，不使用原版索引、下载器或安装器；APK无安装权限。原版加速通用代码默认关闭，Gitee下载传输已移除。
- APK 大小：18,912,766 字节；SHA-256：`da9c46fcef0f25b7d91ea040ad2f76c0a1379ab91cc2c76ad4f3f778732478a9`。
- 构建源码：`3f45e58f5853284cfd7042d7e0bad9a2f3cdef78`；后续交付提交只增加APK、提示词副本、下载说明、构建记录与校验，不宣称可重现二进制源码认证。
- [build-record-v9.json](build-record-v9.json)：protocol 39,862项检查/32,856条通知回放；session 831项、mobile 790项（1项原有跳过）、BLE 9项、Lab 21项，无失败。构建、lint（0错误）、Mock AndroidTest编译通过；实际APK 3,534项编译字符串比对通过。
- [同版本截图导入提示词](./OpenHOYI-77-Personal-v9-curve-import-prompt.zh-CN.txt)：个人schema4平级格式，SHA-256 `bbf667dabb2de0ce5bb6727d77e5a4e20898bb28549de7c5bdbb01a90e117399`；入口和操作见[个人教程](../docs/personal-curve-import-guide.md)。不要混用原版configuration格式。
- 未实际安装或执行窗口/机器验收；字号实显、预热/停水、FW113持久化及升级后用户数据仍待真机验证。网站参考截图来自上游Mock模拟器，不作为个人包证据。

[安装与使用说明](../PERSONAL_BUILD.md) · [累计对齐与复核记录](../docs/evidence/personal-dev127-audit.md) · [完整源码](../) · [许可证](../LICENSE)

历史归档：[V1 APK](OpenHOYI-77-Personal-v1.apk) · [V2 APK](OpenHOYI-77-Personal-v2.apk) · [V3 APK](OpenHOYI-77-Personal-v3.apk) · [V4 APK](OpenHOYI-77-Personal-v4.apk) · [V5 APK](OpenHOYI-77-Personal-v5.apk) · [V6 APK](OpenHOYI-77-Personal-v6.apk) · [V7 APK](OpenHOYI-77-Personal-v7.apk) · [V8 APK](OpenHOYI-77-Personal-v8.apk)。旧文件保持原样；V2有已知启动问题，不通过低版本降级或卸载恢复新格式数据。本次只通过GitHub交付。
