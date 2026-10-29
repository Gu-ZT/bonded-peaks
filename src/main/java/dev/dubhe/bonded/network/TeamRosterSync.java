package dev.dubhe.bonded.network;

import dev.dubhe.bonded.team.TeamManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** 玩家登录与队伍变更后推送完整成员表，关闭队伍界面时仍保持地图数据更新。 */
public final class TeamRosterSync {
    private TeamRosterSync() {
    }

    private static TeamRoster snapshot(MinecraftServer server) {
        return new TeamRoster(TeamManager.get(server).listTeams().stream()
            .map(team -> new TeamRoster.Entry(team.getName(), team.getMembers())).toList());
    }

    public static void sendTo(ServerPlayer player) {
        if (player.connection.hasChannel(TeamRoster.TYPE)) {
            PacketDistributor.sendToPlayer(player, snapshot(player.level().getServer()));
        }
    }

    public static void broadcast(MinecraftServer server) {
        TeamRoster roster = snapshot(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.connection.hasChannel(TeamRoster.TYPE)) {
                PacketDistributor.sendToPlayer(player, roster);
            }
        }
    }
}
