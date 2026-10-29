package dev.dubhe.bonded.network;

import dev.dubhe.bonded.BondedPeaks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** 公开队伍成员表用于地图队友判定，不包含邀请或聊天内容。 */
public final class TeamRoster implements CustomPacketPayload {
    public static final TeamRoster EMPTY = new TeamRoster(List.of());
    public static final Type<TeamRoster> TYPE = new Type<>(Identifier.fromNamespaceAndPath(BondedPeaks.MOD_ID, "team_roster"));
    public static final StreamCodec<FriendlyByteBuf, TeamRoster> CODEC = StreamCodec.of((buffer, roster) -> {
        buffer.writeCollection(roster.entries, (output, team) -> {
            output.writeUtf(team.name);
            output.writeCollection(team.members, (target, member) -> target.writeUUID(member));
        });
    }, buffer -> new TeamRoster(buffer.readList(input -> new Entry(input.readUtf(), input.readList(source -> source.readUUID())))));

    private final List<Entry> entries;
    private final Map<String, Entry> teams;
    private final Map<UUID, String> membership;

    public TeamRoster(List<Entry> entries) {
        this.entries = List.copyOf(entries);
        Map<String, Entry> teams = new HashMap<>();
        Map<UUID, String> membership = new HashMap<>();
        for (Entry entry : this.entries) {
            String id = idFor(entry.name);
            if (teams.put(id, entry) != null) {
                throw new IllegalArgumentException("Duplicate team: " + id);
            }
            for (UUID member : entry.members) {
                if (membership.put(member, id) != null) {
                    throw new IllegalArgumentException("Player belongs to multiple teams: " + member);
                }
            }
        }
        this.teams = Map.copyOf(teams);
        this.membership = Map.copyOf(membership);
    }

    /** 队名忽略大小写，前缀避免与其他队伍实现的标识冲突。 */
    public static String idFor(String name) {
        return BondedPeaks.MOD_ID + ":" + name.toLowerCase(Locale.ROOT);
    }

    public @Nullable String teamOf(UUID player) {
        return this.membership.get(player);
    }

    public @Nullable Entry team(String id) {
        return this.teams.get(id);
    }

    public Map<UUID, String> membership() {
        return this.membership;
    }

    @Override
    public Type<TeamRoster> type() {
        return TYPE;
    }

    /** 队伍保留原始名称用于显示，成员列表在快照创建后不可修改。 */
    public record Entry(String name, List<UUID> members) {
        public Entry {
            members = List.copyOf(members);
        }
    }
}
