package dev.dubhe.bonded.network;

import dev.dubhe.bonded.BondedPeaks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

/** 当前玩家的队伍与邀请快照，在线状态由服务端提供。 */
public record TeamSnapshot(String name, List<Member> members, List<Invite> invites) implements CustomPacketPayload {
    public static final Type<TeamSnapshot> TYPE = new Type<>(Identifier.fromNamespaceAndPath(BondedPeaks.MOD_ID, "team_snapshot"));
    public static final StreamCodec<FriendlyByteBuf, TeamSnapshot> CODEC = StreamCodec.of(
        (buffer, value) -> {
            buffer.writeUtf(value.name);
            buffer.writeCollection(value.members, (output, member) -> {
                output.writeUUID(member.id);
                output.writeUtf(member.name);
                output.writeBoolean(member.owner);
                output.writeBoolean(member.online);
            });
            buffer.writeCollection(value.invites, (output, invite) -> {
                output.writeUtf(invite.team);
                output.writeUUID(invite.inviterId);
                output.writeUtf(invite.inviter);
                output.writeVarInt(invite.remainingSeconds);
            });
        }, buffer -> new TeamSnapshot(buffer.readUtf(),
            buffer.readList(input -> new Member(input.readUUID(), input.readUtf(), input.readBoolean(), input.readBoolean())),
            buffer.readList(input -> new Invite(input.readUtf(), input.readUUID(), input.readUtf(), input.readVarInt()))));

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
