package dev.dubhe.bonded.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeamRosterTest {
    @Test
    void roundTripRetainsAllTeamsAndOfflineMembers() {
        UUID foo = UUID.randomUUID();
        UUID bar = UUID.randomUUID();
        TeamRoster original = new TeamRoster(List.of(new TeamRoster.Entry("山河", List.of(foo)), new TeamRoster.Entry("example", List.of(bar))));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            TeamRoster.CODEC.encode(buffer, original);
            TeamRoster decoded = TeamRoster.CODEC.decode(buffer);
            assertEquals(original.membership(), decoded.membership());
            assertEquals("山河", decoded.team(decoded.teamOf(foo)).name());
            assertEquals(List.of(bar), decoded.team(decoded.teamOf(bar)).members());
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void emptyRosterRemovesPreviousMembership() {
        UUID player = UUID.randomUUID();
        TeamRoster original = new TeamRoster(List.of(new TeamRoster.Entry("example", List.of(player))));
        assertEquals(TeamRoster.idFor("example"), original.teamOf(player));
        assertNull(TeamRoster.EMPTY.teamOf(player));
    }

    @Test
    void rejectsAmbiguousMembershipAndCaseInsensitiveTeamNames() {
        UUID player = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new TeamRoster(List.of(
            new TeamRoster.Entry("foo", List.of(player)), new TeamRoster.Entry("bar", List.of(player)))));
        assertThrows(IllegalArgumentException.class, () -> new TeamRoster(List.of(
            new TeamRoster.Entry("Example", List.of()), new TeamRoster.Entry("example", List.of()))));
    }

    @Test
    void ownsMemberListsAndDoesNotChangePreviouslyPublishedSnapshot() {
        UUID player = UUID.randomUUID();
        List<UUID> members = new ArrayList<>(List.of(player));
        TeamRoster roster = new TeamRoster(List.of(new TeamRoster.Entry("example", members)));
        members.clear();
        assertEquals(List.of(player), roster.team(TeamRoster.idFor("EXAMPLE")).members());
        assertThrows(UnsupportedOperationException.class, () -> roster.membership().clear());
    }
}
