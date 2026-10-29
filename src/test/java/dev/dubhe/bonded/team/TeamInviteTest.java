package dev.dubhe.bonded.team;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamInviteTest {
    @Test
    void invitationExpiresUsingElapsedWallClockTime() {
        long sentAt = System.currentTimeMillis();
        TeamInvite invite = new TeamInvite(UUID.randomUUID(), "foo", "example", UUID.randomUUID(), sentAt);
        assertFalse(invite.isExpired(sentAt));
        assertFalse(invite.isExpired(sentAt + TeamInvite.EXPIRE_AFTER_MILLIS - 1));
        assertTrue(invite.isExpired(sentAt + TeamInvite.EXPIRE_AFTER_MILLIS));
    }
}
