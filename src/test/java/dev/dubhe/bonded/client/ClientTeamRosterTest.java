package dev.dubhe.bonded.client;

import dev.dubhe.bonded.network.TeamRoster;
import net.neoforged.neoforge.common.NeoForge;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ClientTeamRosterTest {
    @AfterEach
    void clear() {
        ClientTeamRoster.update(TeamRoster.EMPTY);
    }

    @Test
    void replacesStateBeforePublishingAndClearsOnDisconnect() {
        UUID player = UUID.randomUUID();
        List<ClientTeamRoster.Changed> changes = new ArrayList<>();
        Consumer<ClientTeamRoster.Changed> listener = event -> {
            assertSame(event.after(), ClientTeamRoster.current());
            changes.add(event);
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            TeamRoster roster = new TeamRoster(List.of(new TeamRoster.Entry("example", List.of(player))));
            ClientTeamRoster.update(roster);
            ClientTeamRoster.update(new TeamRoster(List.of(new TeamRoster.Entry("example", List.of(player)))));
            assertEquals(1, changes.size());
            assertEquals(TeamRoster.idFor("example"), ClientTeamRoster.current().teamOf(player));
            ClientTeamRoster.update(TeamRoster.EMPTY);
            assertNull(ClientTeamRoster.current().teamOf(player));
            assertEquals(2, changes.size());
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
    }
}
