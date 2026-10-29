package dev.dubhe.bonded.integration;

import dev.dubhe.bonded.BondedPeaks;
import dev.dubhe.bonded.client.ClientTeamRoster;
import dev.dubhe.bonded.network.TeamRoster;
import dev.dubhe.bonded.team.TeamManager;
import dev.dubhe.map.team.TeamProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

final class BondedTeamProvider implements TeamProvider {
    private static @Nullable MinecraftServer server() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && server.isSameThread() ? server : null;
    }

    @Override
    public String id() {
        return BondedPeaks.MOD_ID;
    }

    @Override
    public @Nullable String teamIdOf(UUID playerId) {
        MinecraftServer server = server();
        if (server != null) {
            return TeamManager.get(server).getTeamFor(playerId).map(team -> TeamRoster.idFor(team.getName())).orElse(null);
        }
        return FMLEnvironment.getDist() == Dist.CLIENT ? ClientAccess.roster().teamOf(playerId) : null;
    }

    @Override
    public List<UUID> onlineMembersOf(String teamId) {
        MinecraftServer server = server();
        if (server != null) {
            return TeamManager.get(server).listTeams().stream().filter(team -> TeamRoster.idFor(team.getName()).equals(teamId))
                .flatMap(team -> TeamManager.get(server).getOnlineMembers(team).stream()).map(player -> player.getUUID()).toList();
        }
        return FMLEnvironment.getDist() == Dist.CLIENT ? ClientAccess.onlineMembers(teamId) : List.of();
    }

    @Override
    public boolean sameTeam(UUID first, UUID second) {
        String team = this.teamIdOf(first);
        return team != null && team.equals(this.teamIdOf(second));
    }

    @Override
    public Component teamDisplayName(String teamId) {
        MinecraftServer server = server();
        String name;
        if (server != null) {
            name = TeamManager.get(server).listTeams().stream().filter(team -> TeamRoster.idFor(team.getName()).equals(teamId))
                .map(team -> team.getName()).findFirst().orElse("");
        } else {
            TeamRoster.Entry team = FMLEnvironment.getDist() == Dist.CLIENT ? ClientAccess.roster().team(teamId) : null;
            name = team == null ? "" : team.name();
        }
        return Component.translatable("screen.bonded_peaks.team_name", name);
    }

    @Override
    public int teamColor(String teamId) {
        return 0xFFFFFFFF;
    }

    private static final class ClientAccess {
        private static TeamRoster roster() {
            return ClientTeamRoster.current();
        }

        private static List<UUID> onlineMembers(String id) {
            TeamRoster.Entry team = roster().team(id);
            var connection = Minecraft.getInstance().getConnection();
            return team == null || connection == null ? List.of() : team.members().stream()
                .filter(player -> connection.getPlayerInfo(player) != null).toList();
        }
    }
}
