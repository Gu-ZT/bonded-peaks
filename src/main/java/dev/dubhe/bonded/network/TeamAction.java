package dev.dubhe.bonded.network;

import dev.dubhe.bonded.BondedPeaks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringUtil;

import java.util.Locale;
import java.util.UUID;

/** 界面操作仅允许已列出的队伍命令，并携带操作时的队名以校验过期界面。 */
public record TeamAction(UUID requestId, Kind kind, String team, String argument) implements CustomPacketPayload {
    public static final Type<TeamAction> TYPE = new Type<>(Identifier.fromNamespaceAndPath(BondedPeaks.MOD_ID, "team_action"));
    public static final StreamCodec<FriendlyByteBuf, TeamAction> CODEC = StreamCodec.of((buffer, action) -> {
        buffer.writeUUID(action.requestId);
        buffer.writeEnum(action.kind);
        buffer.writeUtf(action.team, 24);
        buffer.writeUtf(action.argument, 239);
    }, buffer -> new TeamAction(buffer.readUUID(), buffer.readEnum(Kind.class), buffer.readUtf(24), buffer.readUtf(239)));

    @Override
    public Type<TeamAction> type() {
        return TYPE;
    }

    /** 使用普通聊天命令的字符规则，玩家操作必须携带唯一身份。 */
    public boolean validArgument() {
        if (this.argument.length() > 239 || !this.argument.chars().allMatch(StringUtil::isAllowedChatCharacter)) {
            return false;
        }
        return switch (this.kind) {
            case LEAVE, DISBAND -> this.argument.isEmpty();
            case CREATE, CHAT -> !this.argument.isBlank();
            case INVITE -> !this.argument.isEmpty() && !this.argument.startsWith("@") && StringUtil.isValidPlayerName(this.argument);
            case ACCEPT, KICK, TRANSFER -> {
                try {
                    yield UUID.fromString(this.argument).toString().equals(this.argument);
                } catch (IllegalArgumentException exception) {
                    yield false;
                }
            }
        };
    }

    /** 操作集合与服务端队伍命令对应。 */
    public enum Kind {
        CREATE, INVITE, ACCEPT, KICK, TRANSFER, LEAVE, DISBAND, CHAT;

        public String command() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
