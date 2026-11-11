# 个人测试安装包

[下载 OpenHOYI 77 扩展 v3 APK](https://github.com/wwh0188/future68/raw/refs/heads/feature/control-modes-curve-notes/downloads/OpenHOYI-77-Personal-v3.apk)

如果打开的是文件详情，选择下载原始文件。私有仓库需要登录仓库所有者的 GitHub 账号。

- 版本：`0.1.77-personal.3`，versionCode 80；Android 8.0 及以上。
- 应用名：OpenHOYI 77 扩展；包名：`io.openhoyi.mobile.personal`。
- 可覆盖升级个人 v1/v2，保持原包名和测试签名；不要卸载或清数据。
- 采纳用户实测正常的 dev-79-scale-fix 对应 Felicita 目标重量与读数时间修复。连接普通 Felicita 即可使用目标重量，无需 V2 的 ART 启用按钮；原只读绑定继续只读，可重新扫描选择普通 Felicita。
- 曲线目标重量须大于 0。保留前置去皮确认、7 秒后重量判断、断秤/旧数据保护和未知结果不自动重试。
- APK SHA-256：`5d707b852938de14a626ff4de152b0ce9a27a9058e4ed5ed0b64f21764a3a27c`。
- 构建源码：`055f20e03754c397de90b873ab63aa324ae3a420`；交付提交只增加 APK 和下载记录。
- 已完成相关软件检查与独立代码/流程复核，详见 [build-record-v3.json](build-record-v3.json)。参考版由用户报告已能用；合并后的个人 V3 仍须核对实际停水。

历史归档：[v1 APK](OpenHOYI-77-Personal-v1.apk) · [v1 记录](build-record.json) · [v2 APK](OpenHOYI-77-Personal-v2.apk) · [v2 记录](build-record-v2.json)。V2 的启动问题未解决，不推荐继续使用；旧版号较低，不以降级代替恢复。

[安装与使用说明](../PERSONAL_BUILD.md) · [修复来源与边界](../docs/evidence/felicita-reference-adoption.md) · [源码许可证](../LICENSE) · [备用下载页及精简原生源码包](https://openhoyi-77-personal-download.franknoodle2.chatgpt.site)
