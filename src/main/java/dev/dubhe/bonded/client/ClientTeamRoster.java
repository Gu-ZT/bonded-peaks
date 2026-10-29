package dev.dubhe.bonded.client;

import dev.dubhe.bonded.network.TeamRoster;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/** 客户端成员表与变更事件供地图适配器使用，连接关闭时必须清空。 */
public final class ClientTeamRoster {
    private static TeamRoster current = TeamRoster.EMPTY;

    private ClientTeamRoster() {
    }

    public static TeamRoster current() {
        return current;
    }

    static void update(TeamRoster roster) {
        TeamRoster previous = current;
        current = roster;
        if (!previous.membership().equals(roster.membership())) {
            NeoForge.EVENT_BUS.post(new Changed(previous, roster));
        }
    }

    /** 事件在客户端主线程发布，订阅方可以立即读取新的成员表。 */
    public static final class Changed extends Event {
        private final TeamRoster before;
        private final TeamRoster after;

        private Changed(TeamRoster before, TeamRoster after) {
            this.before = before;
            this.after = after;
        }

        public TeamRoster before() {
            return this.before;
        }

        public TeamRoster after() {
            return this.after;
        }
    }
}
