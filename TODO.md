## 命令体系

* /bondedpeaks create <队伍名>        — 创建队伍，自己成为队长（名字不可含特殊字符，长度≤12字）
* /bondedpeaks invite <玩家>          — 邀请一名玩家加入队伍（仅队长可用）
* /bondedpeaks accept [邀请者]        — 接受队伍邀请；若省略参数，自动接受最近一次邀请
* /bondedpeaks leave — 离开当前队伍（队长需先转移或解散）
* /bondedpeaks disband — 解散队伍（仅队长可用，二次确认）
* /bondedpeaks kick <玩家>            — 将成员踢出队伍（仅队长可用）
* /bondedpeaks transfer <玩家>        — 将队长之位转让给指定成员（仅队长可用）
* /bondedpeaks list — 列出服务器中所有现存队伍及其人数
* /bondedpeaks info [队伍名]          — 查看队伍详情（名称、队长、成员列表等）；不指定则显示自己所在队伍
* /bondedpeaks chat <消息...>         — 发送仅队伍成员可见的消息
* /bp <消息...>                       — /bondedpeaks chat 的快捷方式

## 设计细节

* 确认机制：disband 命令执行后，会提示“山河虽广，契阔难续。输入 /bondedpeaks confirm 确认解散”，防止误操作。
* 唯一约束：每名玩家同时只能属于一个队伍，创建或接受新邀请前需先离开当前队伍。
* 邀请超时：邀请发出后 60 秒内有效，过期需重新邀请；玩家离线则邀请暂存，上线后可接受。
* 反馈文本：所有提示均采用文言白话融合风格，例如：
    * 创建成功：山河为证，与子同契。队伍「%s」已立。
    * 邀请发送：山河有待，邀君同契。已向 %s 发出邀请。
    * 加入队伍：同契既成，万里同行。你已加入「%s」。

## 队伍数据的序列化与反序列化

队伍序列化至`<存档路径>/serverconfigs/bondedpeaks/`，邀请信息属于临时状态，不持久化。

### 内存结构

```java
public class Team {
    private final String name;                  // 队伍名，唯一
    private UUID owner;                         // 队长UUID
    private final List<UUID> members;           // 成员列表，包含队长
    private final long createTime;              // 创建时间戳
    // 以下为临时数据，不序列化
    private final Set<UUID> invitedPlayers = new HashSet<>();
    private final Map<UUID, Long> inviteTimes = new HashMap<>();
}
```
