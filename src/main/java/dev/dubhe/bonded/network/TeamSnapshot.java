package dev.dubhe.bonded.network;

import dev.dubhe.bonded.BondedPeaks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

/** 当前玩家的队伍与邀请快照，在线状态由服务端提供。 */
public record TeamSnapshot(String name, List<Member> members, List<Invite> invites) implements CustomPacketPayload {
    public static final Type<TeamSnapshot> TYPE = new Type<>(Identifier.fromNamespaceAndPath(BondedPeaks.MOD_ID, "team_snapshot"));
    private static final StreamCodec<FriendlyByteBuf, Member> MEMBER_CODEC = StreamCodec.of((buffer, member) -> {
        buffer.writeUUID(member.id);
        buffer.writeUtf(member.name);
        buffer.writeBoolean(member.owner);
        buffer.writeBoolean(member.online);
    }, buffer -> new Member(buffer.readUUID(), buffer.readUtf(), buffer.readBoolean(), buffer.readBoolean()));
    private static final StreamCodec<FriendlyByteBuf, Invite> INVITE_CODEC = StreamCodec.of((buffer, invite) -> {
        buffer.writeUtf(invite.team);
        buffer.writeUUID(invite.inviterId);
        buffer.writeUtf(invite.inviter);
        buffer.writeVarInt(invite.remainingSeconds);
    }, buffer -> new Invite(buffer.readUtf(), buffer.readUUID(), buffer.readUtf(), buffer.readVarInt()));
    public static final StreamCodec<FriendlyByteBuf, TeamSnapshot> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, TeamSnapshot::name,
        MEMBER_CODEC.apply(ByteBufCodecs.list()), TeamSnapshot::members,
        INVITE_CODEC.apply(ByteBufCodecs.list()), TeamSnapshot::invites, TeamSnapshot::new);

    public TeamSnapshot {
        members = List.copyOf(members);
        invites = List.copyOf(invites);
    }

    @Override
    public Type<TeamSnapshot> type() {
        return TYPE;
    }

    /** 成员身份用于操作目标校验，名称仅用于显示。 */
    public record Member(UUID id, String name, boolean owner, boolean online) {
    }

    /** 剩余秒数由服务端计算，避免客户端时钟偏移。 */
    public record Invite(String team, UUID inviterId, String inviter, int remainingSeconds) {
    }
}
