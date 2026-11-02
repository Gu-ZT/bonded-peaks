package dev.dubhe.bonded.team;

import dev.dubhe.bonded.network.TeamRoster;
import net.minecraft.server.MinecraftServer;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import org.mockito.ArgumentCaptor;
import net.minecraft.world.level.storage.LevelResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.clearInvocations;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TeamLifecycleTest {
    @TempDir
    Path directory;
    private MinecraftServer server;
    private TeamManager teams;
    private PlayerList players;
    private final GameProfile owner = new GameProfile(UUID.randomUUID(), "foo");
    private final GameProfile member = new GameProfile(UUID.randomUUID(), "bar");
    private final GameProfile invited = new GameProfile(UUID.randomUUID(), "example");

    @BeforeEach
    void setUp() {
        this.server = mock(MinecraftServer.class);
        this.players = mock(PlayerList.class);
        when(this.server.getWorldPath(LevelResource.ROOT)).thenReturn(this.directory);
        when(this.server.getPlayerList()).thenReturn(players);
        when(players.getPlayers()).thenReturn(List.of());
        this.teams = TeamManager.get(this.server);
    }

    @Test
    void mutationsBroadcastAuthoritativeMembershipWithoutUiRequests() throws TeamManager.TeamException {
        ServerPlayer receiver = mock(ServerPlayer.class);
        receiver.connection = mock(ServerGamePacketListenerImpl.class);
        when(receiver.connection.hasChannel(TeamRoster.TYPE)).thenReturn(true);
        when(this.players.getPlayers()).thenReturn(List.of(receiver));
        this.teams.createTeam(this.owner, "example", System.currentTimeMillis());
        ArgumentCaptor<ClientboundCustomPayloadPacket> packet = ArgumentCaptor.forClass(ClientboundCustomPayloadPacket.class);
        verify(receiver.connection).send(packet.capture());
        TeamRoster created = assertInstanceOf(TeamRoster.class, packet.getValue().payload());
        assertEquals(TeamRoster.idFor("example"), created.teamOf(this.owner.getId()));
        clearInvocations(receiver.connection);
        this.teams.beginDisband(this.owner, System.currentTimeMillis());
        this.teams.confirmDisband(this.owner);
        verify(receiver.connection).send(packet.capture());
        assertTrue(assertInstanceOf(TeamRoster.class, packet.getValue().payload()).membership().isEmpty());
    }

    @AfterEach
    void unload() {
        TeamManager.unload(this.server);
    }

    private TeamRoster roster() {
        return new TeamRoster(this.teams.listTeams().stream()
            .map(team -> new TeamRoster.Entry(team.getName(), team.getMembers())).toList());
    }

    @Test
    void joiningLeavingAndKickingUpdatePublicMembership() throws TeamManager.TeamException {
        long now = System.currentTimeMillis();
        this.teams.createTeam(this.owner, "山河", now);
        this.teams.invite(this.owner, this.member, now);
        this.teams.acceptInvite(this.member, this.owner.getId(), now + 1);
        assertEquals(this.roster().teamOf(this.owner.getId()), this.roster().teamOf(this.member.getId()));
        assertThrows(TeamManager.TeamException.class, () -> this.teams.kick(this.member, this.owner.getId()));
        this.teams.leave(this.member);
        assertNull(this.roster().teamOf(this.member.getId()));
        this.teams.invite(this.owner, this.member, now + 2);
        this.teams.acceptInvite(this.member, this.owner.getId(), now + 3);
        this.teams.kick(this.owner, this.member.getId());
        assertNull(this.roster().teamOf(this.member.getId()));
        assertEquals(TeamRoster.idFor("山河"), this.roster().teamOf(this.owner.getId()));
    }

    @Test
    void transferInvalidatesInvitesAndDisbandClearsMembership() throws TeamManager.TeamException {
        long now = System.currentTimeMillis();
        this.teams.createTeam(this.owner, "example", now);
        this.teams.invite(this.owner, this.member, now);
        this.teams.acceptInvite(this.member, this.owner.getId(), now + 1);
        this.teams.invite(this.owner, this.invited, now + 2);
        this.teams.transfer(this.owner, this.member.getId());
        assertTrue(this.teams.getInvitesFor(this.invited.getId(), now + 3).isEmpty());
        assertThrows(TeamManager.TeamException.class, () -> this.teams.beginDisband(this.owner, now + 4));
        this.teams.beginDisband(this.member, now + 4);
        this.teams.confirmDisband(this.member);
        assertTrue(this.roster().membership().isEmpty());
    }

    @Test
    void reloadPreservesTeamsAndOfflineNamesButExpiresPendingInvites() throws TeamManager.TeamException {
        long now = System.currentTimeMillis();
        this.teams.createTeam(this.owner, "example", now);
        this.teams.invite(this.owner, this.member, now);
        this.teams.acceptInvite(this.member, this.owner.getId(), now + 1);
        this.teams.invite(this.owner, this.invited, now + 2);
        TeamManager.unload(this.server);
        this.teams = TeamManager.get(this.server);
        assertEquals("bar", this.teams.resolvePlayerName(this.member.getId()));
        assertEquals(2, this.teams.getTeamFor(this.owner.getId()).orElseThrow().getMemberCount());
        assertFalse(this.roster().membership().isEmpty());
        assertTrue(this.teams.getInvitesFor(this.invited.getId(), now + 3).isEmpty());
    }
}
