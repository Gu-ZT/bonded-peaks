package dev.dubhe.bonded.network;

import dev.dubhe.bonded.BondedPeaks;
import dev.dubhe.bonded.team.TeamInvite;
import dev.dubhe.bonded.team.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** 界面只请求自己的队伍数据，修改操作复用服务端命令权限校验。 */
@EventBusSubscriber(modid = BondedPeaks.MOD_ID)
public final class TeamPackets {
    private static final Map<ServerPlayer, Long> LAST_REQUEST = new WeakHashMap<>();

    private TeamPackets() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("team-ui-1").optional();
        registrar.playToServer(Request.TYPE, Request.CODEC, TeamPackets::snapshot);
        registrar.playToServer(TeamAction.TYPE, TeamAction.CODEC, TeamActions::execute);
        registrar.playToClient(TeamSnapshot.TYPE, TeamSnapshot.CODEC);
        registrar.playToClient(TeamReply.TYPE, TeamReply.CODEC);
        registrar.playToClient(TeamRoster.TYPE, TeamRoster.CODEC);
    }

    private static void snapshot(Request request, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        long tick = player.level().getServer().overworld().getGameTime();
        Long previous = LAST_REQUEST.get(player);
        if (previous != null && tick - previous < 10) {
            return;
        }
        LAST_REQUEST.put(player, tick);
        sendSnapshot(player, context);
    }

    static void sendSnapshot(ServerPlayer player, IPayloadContext context) {
        TeamManager teams = TeamManager.get(player.level().getServer());
        var team = teams.getTeamFor(player.getUUID()).orElse(null);
        List<TeamSnapshot.Member> members = team == null ? List.of() : team.getMembers().stream()
            .map(id -> new TeamSnapshot.Member(id, teams.resolvePlayerName(id), team.isOwner(id),
                player.level().getServer().getPlayerList().getPlayer(id) != null))
            .toList();
        long now = System.currentTimeMillis();
        List<TeamSnapshot.Invite> invites = teams.getInvitesFor(player.getUUID(), now).stream()
            .map(invite -> new TeamSnapshot.Invite(invite.teamName(), invite.inviterId(), invite.inviterName(),
                (int) ((TeamInvite.EXPIRE_AFTER_MILLIS - (now - invite.sentAt()) + 999) / 1000)))
            .toList();
        context.reply(new TeamSnapshot(team == null ? "" : team.getName(), members, invites));
    }

    /** 不携带目标玩家，防止请求他人的邀请数据。 */
    public record Request() implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(Identifier.fromNamespaceAndPath(BondedPeaks.MOD_ID, "team_request"));
        public static final StreamCodec<FriendlyByteBuf, Request> CODEC = StreamCodec.unit(new Request());

        @Override
        public Type<Request> type() {
            return TYPE;
        }
    }
}
