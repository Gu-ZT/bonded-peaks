package dev.dubhe.bonded.team;

import java.util.UUID;

public record TeamInvite(UUID inviterId, String inviterName, String teamName, UUID targetId, long sentAt) {
    public static final long EXPIRE_AFTER_MILLIS = 60_000L;

    public boolean isExpired(long now) {
        return now - this.sentAt >= EXPIRE_AFTER_MILLIS;
    }
}

