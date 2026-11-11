# 个人测试安装包

[下载 OpenHOYI 77 扩展 v2 APK](https://github.com/wwh0188/future68/raw/refs/heads/feature/control-modes-curve-notes/downloads/OpenHOYI-77-Personal-v2.apk)

如果打开的是 APK 文件详情，在文件右侧选择下载原始文件。此仓库为私有，需要登录仓库所有者的 GitHub 账号。

- 应用名：OpenHOYI 77 扩展。
- 包名：`io.openhoyi.mobile.personal`，可与原版并存。
- 版本：`0.1.77-personal.2`，versionCode 79；Android 8.0 及以上。
- 可直接覆盖升级个人版 v1（78），保持相同包名和原测试签名；不要卸载或清数据。
- 新增：连接 Felicita ART 后，在电子秤设置中显式启用「ART 按重停止（测试）」；曲线目标重量必须大于 0。普通 Felicita 不会自动升级。
- APK SHA-256：`3abc3fc20052f057a769741ac5532254951d5719c5e952c0a6d917653d791a85`。
- 构建源码提交：`8d17e974bd42e05e35d1a563bc999ec65a29dd1f`；后续交付提交只增加下载说明、APK 和构建记录。
- 软件检查、签名和构建信息见 [build-record-v2.json](build-record-v2.json)。真实按重停水尚未实机验收；保留至少 7 秒后的重量判断、前置去皮确认、断秤/旧读数保护和未知结果不自动重试。

旧版归档：[v1 APK](OpenHOYI-77-Personal-v1.apk) · [v1 构建记录](build-record.json)。旧版版本号更低，不建议用降级替代修复。

[安装与使用说明](../PERSONAL_BUILD.md) · [源码许可证](../LICENSE) · [备用下载页及精简原生源码包](https://openhoyi-77-personal-download.franknoodle2.chatgpt.site)
