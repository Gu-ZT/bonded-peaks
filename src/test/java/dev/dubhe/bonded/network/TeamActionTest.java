package dev.dubhe.bonded.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamActionTest {
    @Test
    void chatRejectsControlAndFormattingCharacters() {
        for (String message : new String[]{"foo\nbar", "foo\rbar", "foo\u0000bar", "§afoo"}) {
            assertFalse(new TeamAction(UUID.randomUUID(), TeamAction.Kind.CHAT, "example", message).validArgument());
        }
        assertTrue(new TeamAction(UUID.randomUUID(), TeamAction.Kind.CHAT, "example", "山顶集合").validArgument());
    }

    @Test
    void memberActionsRequireUuidAndRejectSelectors() {
        assertFalse(new TeamAction(UUID.randomUUID(), TeamAction.Kind.KICK, "example", "@a").validArgument());
        assertFalse(new TeamAction(UUID.randomUUID(), TeamAction.Kind.TRANSFER, "example", "foo").validArgument());
        assertTrue(new TeamAction(UUID.randomUUID(), TeamAction.Kind.ACCEPT, "example", UUID.randomUUID().toString()).validArgument());
    }

    @Test
    void roundTripKeepsRequestAndTeamForStaleStateValidation() {
        TeamAction action = new TeamAction(UUID.randomUUID(), TeamAction.Kind.TRANSFER, "山河", UUID.randomUUID().toString());
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            TeamAction.CODEC.encode(buffer, action);
            assertEquals(action, TeamAction.CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsOversizedChatBeforeItReachesCommandExecution() {
        TeamAction action = new TeamAction(UUID.randomUUID(), TeamAction.Kind.CHAT, "example", "x".repeat(240));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            assertThrows(io.netty.handler.codec.EncoderException.class, () -> TeamAction.CODEC.encode(buffer, action));
        } finally {
            buffer.release();
        }
    }
}
