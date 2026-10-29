package dev.dubhe.bonded.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamSnapshotTest {
    @Test
    void roundTripPreservesIdentityPermissionsAndInvites() {
        UUID owner = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        TeamSnapshot snapshot = new TeamSnapshot("山河", List.of(
            new TeamSnapshot.Member(owner, "foo", true, true),
            new TeamSnapshot.Member(member, "bar", false, false)), List.of(
            new TeamSnapshot.Invite("example", owner, "foo", 42)));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            TeamSnapshot.CODEC.encode(buffer, snapshot);
            assertEquals(snapshot, TeamSnapshot.CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void supportsPlayerWithoutTeamOrInvites() {
        TeamSnapshot snapshot = new TeamSnapshot("", List.of(), List.of());
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            TeamSnapshot.CODEC.encode(buffer, snapshot);
            assertEquals(snapshot, TeamSnapshot.CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void snapshotOwnsItsLists() {
        List<TeamSnapshot.Member> members = new ArrayList<>();
        List<TeamSnapshot.Invite> invites = new ArrayList<>();
        TeamSnapshot snapshot = new TeamSnapshot("example", members, invites);
        members.add(new TeamSnapshot.Member(UUID.randomUUID(), "foo", true, true));
        invites.add(new TeamSnapshot.Invite("bar", UUID.randomUUID(), "foo", 10));
        assertTrue(snapshot.members().isEmpty());
        assertTrue(snapshot.invites().isEmpty());
    }
}
