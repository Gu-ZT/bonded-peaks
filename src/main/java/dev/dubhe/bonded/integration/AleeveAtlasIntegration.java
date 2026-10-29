package dev.dubhe.bonded.integration;

import dev.anvilcraft.lib.v2.integration.Integration;
import dev.dubhe.bonded.client.ClientTeamRoster;
import dev.dubhe.bonded.network.TeamRoster;
import dev.dubhe.map.AleeveAtlas;
import dev.dubhe.map.api.team.TeamMembershipChangedEvent;
import dev.dubhe.map.team.TeamMembershipWatcher;
import dev.dubhe.map.team.TeamProviders;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.client.Minecraft;

import java.util.UUID;
import java.util.function.Predicate;

/** AnvilLib 仅在 Atlas 存在时加载本适配器，注册发生在全部模组完成构造之后。 */
@Integration(value = "aleeve_atlas", version = "[0.0.1,)")
public final class AleeveAtlasIntegration {
    public void apply() {
        TeamProviders.register(new BondedTeamProvider());
        TeamProviders.select(AleeveAtlas.CONFIG.team.provider);
    }

    public void applyClient() {
        NeoForge.EVENT_BUS.addListener(AleeveAtlasIntegration::changed);
    }

    private static void changed(ClientTeamRoster.Changed event) {
        if (!TeamProviders.active().id().equals("bonded_peaks")) {
            return;
        }
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            reportChanges(event.before(), event.after(), player -> connection.getPlayerInfo(player) != null);
        }
    }

    static void reportChanges(TeamRoster before, TeamRoster after, Predicate<UUID> online) {
        before.membership().forEach((player, team) -> {
            if (online.test(player) && !team.equals(after.teamOf(player))) {
                TeamMembershipWatcher.notifyChange(player, team, TeamMembershipChangedEvent.Change.LEFT);
            }
        });
        after.membership().forEach((player, team) -> {
            if (online.test(player) && !team.equals(before.teamOf(player))) {
                TeamMembershipWatcher.notifyChange(player, team, TeamMembershipChangedEvent.Change.JOINED);
            }
        });
    }
}
