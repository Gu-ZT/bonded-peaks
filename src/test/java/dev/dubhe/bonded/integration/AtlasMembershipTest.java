package dev.dubhe.bonded.integration;

import dev.dubhe.bonded.network.TeamRoster;
import dev.dubhe.map.api.team.TeamMembershipChangedEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasMembershipTest {
    private final List<TeamMembershipChangedEvent> changes = new ArrayList<>();
    private final Consumer<TeamMembershipChangedEvent> listener = event -> this.changes.add(event);

    @BeforeEach
    void listen() {
        NeoForge.EVENT_BUS.addListener(this.listener);
    }

    @AfterEach
    void stopListening() {
        NeoForge.EVENT_BUS.unregister(this.listener);
    }

    @Test
    void initializesOnlyOnlineMembersAndIgnoresRepeatedSnapshot() {
        UUID online = UUID.randomUUID();
        UUID offline = UUID.randomUUID();
        TeamRoster roster = new TeamRoster(List.of(new TeamRoster.Entry("example", List.of(online, offline))));
        AleeveAtlasIntegration.reportChanges(TeamRoster.EMPTY, roster, online::equals);
        assertEquals(List.of(online), this.changes.stream().map(TeamMembershipChangedEvent::playerId).toList());
        assertEquals(TeamMembershipChangedEvent.Change.JOINED, this.changes.getFirst().change());
        this.changes.clear();
        AleeveAtlasIntegration.reportChanges(roster, roster, online::equals);
        assertTrue(this.changes.isEmpty());
        AleeveAtlasIntegration.reportChanges(roster, TeamRoster.EMPTY, online::equals);
    }

    @Test
    void teamSwitchReportsLeaveBeforeJoinAndDisbandReportsLeave() {
        UUID player = UUID.randomUUID();
        TeamRoster before = new TeamRoster(List.of(new TeamRoster.Entry("foo", List.of(player))));
        TeamRoster after = new TeamRoster(List.of(new TeamRoster.Entry("bar", List.of(player))));
        AleeveAtlasIntegration.reportChanges(before, after, player::equals);
        assertEquals(List.of(TeamMembershipChangedEvent.Change.LEFT, TeamMembershipChangedEvent.Change.JOINED),
            this.changes.stream().map(TeamMembershipChangedEvent::change).toList());
        assertEquals(TeamRoster.idFor("foo"), this.changes.getFirst().teamId());
        assertEquals(TeamRoster.idFor("bar"), this.changes.getLast().teamId());
        this.changes.clear();
        AleeveAtlasIntegration.reportChanges(after, TeamRoster.EMPTY, player::equals);
        assertEquals(1, this.changes.size());
        assertEquals(TeamMembershipChangedEvent.Change.LEFT, this.changes.getFirst().change());
    }
}
