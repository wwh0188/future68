# dev-77 个人控制模式与曲线说明

范围：用户要求的个人测试 APK，不是上游正式发版。基线 `dev-77` / `69e31001d689f659a00055b121f86b574955f353`，工作分支 `feature/control-modes-curve-notes`。机器信息来自用户图片：HOYI-HYINHM、固件 1.1.4；本环境未连接该设备。

## 协议依据

直接分析用户提供下载链接中的官方 1.2.6 APK；APK SHA-256 `26133c2ac8ecf3d5ee7682bafa6555b1c78836749c937477b05214a88d8ea238`，bundle SHA-256 `4ed90ea99240b7f37373cdb5c9df0bf315103ca7aea6aaab6f37b2c58dd564d7`。

`scripts/generate_custom_control_oracle.py` 校验原 APK、bundle 与提取函数哈希后，在隔离的离线 Node 上下文执行原函数，不打开蓝牙。生成 `protocol-core/src/test/resources/custom_control_wire.tsv` 的四条拨杆帧和五条曲线帧，覆盖压力跟随、Ls/Hs、独立首段流量、8/12 秒和线格式时间边界。主线程独立重放并与提交 fixture 逐字节比较一致。

新增 `CustomCurveStartPolicy` 与独立且默认拒绝的 host permit；没有修改旧 `CustomPressureStartPolicy` 的许可范围。新参数仍须匹配当前选中的已保存配方，并经过固件、状态、新鲜设置和队列实际发送前的再次复核。未确认结果仍不自动重发。

## 体验和数据

- 拨杆四组合名称与官方两个布尔量对应，四种回读均可显示。
- 整体变流决定后续段单位，首段可以单独选择压力/流量；压力与流量输入分别缓存。首页、曲线库和分享/导入预览沿用逐段单位的渲染器。
- schema 3 保留控制标志、闷蒸/首段时间和公开曲线说明；原 schema 1/2 向后兼容，旧 FLOW_RAW 保持未知、禁止执行。其时间缺失不显示为已确认 0 秒。
- 说明可多行编辑、保存、重开、复制及分享/导入；参与存储校验和与冲突检测，不参与执行配方指纹。工厂副本继承原说明和模式/时间/水量参数。
- 独立个人包 `io.openhoyi.mobile.personal`，真实设备代码路径 `MOCK_MODE=false`；不替换原作者签名的应用身份。

## 独立复核与验证

由 Astra 进行独立源码复核，覆盖模式到单位、schema、adapter、host permit 的完整链、原数据兼容、说明流转、模式草稿恢复及首页预览；未发现剩余阻断问题。复核属于静态审查，不代表实际屏幕操作或机器验收。

已完成本地分支检查、差异空白检查、原 APK oracle 重放、语言目录和 Android 资源生成漂移校验。Gradle 验证与 APK 元数据结果记录在交付的构建记录中；不把编译、模拟 GATT 写入或单元测试声称为实机确认。

没有推送、PR、标签、公开 GitHub Release、安装、清理用户数据或机器操作。
