# 队伍界面

本分支使用 Minecraft 1.21.11、NeoForge 21.11.48 与 Java 21 构建。模组声明的 NeoForge 最低版本为 21.11.45。

默认按 `G` 打开界面，也可使用客户端命令 `/bpui`。按键可以在游戏控制设置中的山河同契分类修改。

界面包含成员、队伍聊天和入队邀请三个页签。未加入队伍时可创建队伍；队长可以邀请玩家、移出成员、转让队长和解散队伍。退出、移出成员、转让与解散均需确认。

邀请页面显示服务端计算的剩余有效时间。邀请玩家时可筛选在线玩家，也可手动输入服务器已识别的离线玩家名称。邀请与接受命令统一使用毫秒时间，邀请有效期为 60 秒。

界面打开期间每秒请求当前玩家的队伍快照。服务端分别限制快照请求与操作请求频率，并校验操作时的队名和玩家权限。操作结果通过独立网络消息返回，关闭命令聊天反馈时仍会显示成功或失败原因。

队伍聊天保留本次连接期间收到的最近 100 条消息，断开连接后清空。界面不会暂停游戏。

图标使用 AnvilLib `SdfGraphics` 的几何图形组合绘制，统一为白色填充、黑色描边，并启用抗锯齿。渲染模块随主模组内嵌。

固定界面文本必须使用 `Component.translatable(...)`。英文文本在 `BondedPeaksLangProvider` 中定义，通过 `./gradlew.bat runData --console=plain` 生成 `src/generated/resources/assets/bonded_peaks/lang/en_us.json`；简体中文必须手动维护 `src/main/resources/assets/bonded_peaks/lang/zh_cn.json`。

本界面没有新增配置文件或服务端配置项。

# Aleeve Atlas 联动

AnvilLib Integration 随主模组内嵌。客户端与服务端同时安装 Aleeve Atlas 和本模组后，Atlas 的 `team.provider=auto` 自动选用 `bonded_peaks`，也可显式设置 `team.provider=bonded_peaks`。显式设置 `vanilla` 时继续使用 Atlas 的原版队伍实现。

Atlas 的队友位置、地图增量同步、共享路径点、队伍标点和雷达同队判定均读取山河同契成员关系。未加入山河同契队伍的玩家不会因原版计分板队伍相同而被判定为队友。队伍显示名沿用山河同契名称，队伍颜色为白色。

公开成员表在登录、创建队伍、接受邀请、退出、移出成员、转让队长和解散后同步。成员表不包含邀请与聊天内容。客户端收到新成员表后立即通知 Atlas 在线成员变化；断开连接时清空缓存。服务端判定从当前服务器查询，客户端只使用服务端同步的数据。

# 构建联动适配器

编译期从 `https://server.cjsah.net:1002/maven/` 解析 `dev.dubhe.map:aleeve_atlas-neoforge-1.21.11:0.0.1+build.+`，运行时仍为可选联动。执行 `./gradlew.bat build --console=plain` 即可构建，持续集成也使用该 Maven 依赖。

开发构建使用 NeoForge 21.11.48。验证最低兼容版本时执行 `./gradlew.bat compileJava test runData '-PneoForgeVersion=21.11.45' --console=plain`。
