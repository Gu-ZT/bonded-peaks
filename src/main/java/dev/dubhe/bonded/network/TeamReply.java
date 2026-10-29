package dev.dubhe.bonded.network;

import dev.dubhe.bonded.BondedPeaks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/** 操作结果独立于聊天与命令反馈设置，通过请求标识关联提交表单。 */
public record TeamReply(UUID requestId, boolean success, Component message) implements CustomPacketPayload {
    public static final Type<TeamReply> TYPE = new Type<>(Identifier.fromNamespaceAndPath(BondedPeaks.MOD_ID, "team_reply"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TeamReply> CODEC = StreamCodec.of((buffer, reply) -> {
        buffer.writeUUID(reply.requestId);
        buffer.writeBoolean(reply.success);
        ComponentSerialization.STREAM_CODEC.encode(buffer, reply.message);
    }, buffer -> new TeamReply(buffer.readUUID(), buffer.readBoolean(), ComponentSerialization.STREAM_CODEC.decode(buffer)));

    @Override
    public Type<TeamReply> type() {
        return TYPE;
    }
}
