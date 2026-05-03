package dev.dubhe.bonded.team;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import dev.dubhe.bonded.BondedPeaks;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class TeamManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<MinecraftServer, TeamManager> INSTANCES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Pattern TEAM_NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}]+$");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MinecraftServer server;
    private final Path storageFile;
    private final Map<String, Team> teamsByName = new LinkedHashMap<>();
    private final Map<UUID, String> teamByMember = new LinkedHashMap<>();
    private final Map<UUID, List<TeamInvite>> invitesByTarget = new LinkedHashMap<>();
    private final Map<UUID, Long> pendingDisbands = new LinkedHashMap<>();
    private final Map<UUID, String> knownPlayerNames = new LinkedHashMap<>();

    private TeamManager(MinecraftServer server) {
        this.server = server;
        this.storageFile = server.getWorldPath(LevelResource.ROOT)
            .resolve("serverconfigs")
            .resolve(BondedPeaks.MOD_ID)
            .resolve("teams.json");
        this.load();
    }

    public static TeamManager get(MinecraftServer server) {
        synchronized (INSTANCES) {
            return INSTANCES.computeIfAbsent(server, TeamManager::new);
        }
    }

    public static void unload(MinecraftServer server) {
        synchronized (INSTANCES) {
            TeamManager manager = INSTANCES.remove(server);
            if (manager != null) {
                manager.save();
            }
        }
    }

    @SuppressWarnings("resource")
    public void onPlayerLogin(ServerPlayer player) {
        this.rememberPlayer(new NameAndId(player.getUUID(), player.getGameProfile().name()));
        this.purgeExpiredInvites(player.level().getServer().overworld().getGameTime());
        List<TeamInvite> invites = this.invitesByTarget.getOrDefault(player.getUUID(), List.of());
        for (TeamInvite invite : invites) {
            Team team = this.getTeamByName(invite.teamName()).orElse(null);
            if (team == null || !team.isOwner(invite.inviterId())) {
                continue;
            }
            player.sendSystemMessage(Component.translatable(
                "commands.bonded_peaks.invite.received",
                invite.inviterName(),
                invite.teamName()
            ));
        }
    }

    public void onPlayerLogout(ServerPlayer player) {
        this.pendingDisbands.remove(player.getUUID());
    }

    public Team createTeam(NameAndId owner, String name, long now) throws TeamException {
        this.rememberPlayer(owner);
        this.validateCanJoinNewTeam(owner.id());
        validateTeamName(name);
        String teamKey = normalizeTeamName(name);
        if (this.teamsByName.containsKey(teamKey)) {
            throw new TeamException("commands.bonded_peaks.create.duplicate", name);
        }

        Team team = Team.create(name, owner.id(), now);
        this.teamsByName.put(teamKey, team);
        this.teamByMember.put(owner.id(), teamKey);
        this.clearIncomingInvites(owner.id());
        this.pendingDisbands.remove(owner.id());
        this.save();
        return team;
    }

    public Team invite(NameAndId inviter, NameAndId target, long now) throws TeamException {
        this.rememberPlayer(inviter);
        this.rememberPlayer(target);
        this.purgeExpiredInvites(now);

        if (inviter.id().equals(target.id())) {
            throw new TeamException("commands.bonded_peaks.invite.self");
        }

        Team team = this.getRequiredTeamFor(inviter.id());
        if (!team.isOwner(inviter.id())) {
            throw new TeamException("commands.bonded_peaks.owner_only");
        }
        if (this.teamByMember.containsKey(target.id())) {
            throw new TeamException("commands.bonded_peaks.team.already_in_team.target", target.name());
        }

        List<TeamInvite> invites = this.invitesByTarget.computeIfAbsent(target.id(), ignored -> new ArrayList<>());
        invites.removeIf(invite -> invite.inviterId().equals(inviter.id()) || normalizeTeamName(invite.teamName()).equals(normalizeTeamName(team.getName())));
        invites.add(new TeamInvite(inviter.id(), inviter.name(), team.getName(), target.id(), now));
        return team;
    }

    public Team acceptInvite(NameAndId target, @Nullable UUID inviterId, long now) throws TeamException {
        this.rememberPlayer(target);
        this.validateCanJoinNewTeam(target.id());
        this.purgeExpiredInvites(now);

        List<TeamInvite> invites = this.invitesByTarget.getOrDefault(target.id(), List.of());
        if (invites.isEmpty()) {
            throw new TeamException("commands.bonded_peaks.accept.none");
        }

        TeamInvite selected = inviterId == null
            ? invites.stream().max(Comparator.comparingLong(TeamInvite::sentAt)).orElse(null)
            : invites.stream().filter(invite -> invite.inviterId().equals(inviterId)).findFirst().orElse(null);
        if (selected == null) {
            throw new TeamException("commands.bonded_peaks.accept.not_found");
        }

        Team team = this.getTeamByName(selected.teamName()).orElse(null);
        if (team == null || !team.isOwner(selected.inviterId())) {
            this.removeInvite(target.id(), selected);
            throw new TeamException("commands.bonded_peaks.accept.invalidated");
        }

        team.addMember(target.id());
        this.teamByMember.put(target.id(), normalizeTeamName(team.getName()));
        this.clearIncomingInvites(target.id());
        this.pendingDisbands.remove(target.id());
        this.save();
        return team;
    }

    public Team leave(NameAndId player) throws TeamException {
        this.rememberPlayer(player);
        Team team = this.getRequiredTeamFor(player.id());
        if (team.isOwner(player.id())) {
            throw new TeamException("commands.bonded_peaks.leave.owner_forbidden");
        }

        team.removeMember(player.id());
        this.teamByMember.remove(player.id());
        this.pendingDisbands.remove(player.id());
        this.save();
        return team;
    }

    @SuppressWarnings("UnusedReturnValue")
    public Team beginDisband(NameAndId player, long now) throws TeamException {
        this.rememberPlayer(player);
        Team team = this.getRequiredTeamFor(player.id());
        if (!team.isOwner(player.id())) {
            throw new TeamException("commands.bonded_peaks.owner_only");
        }

        this.pendingDisbands.put(player.id(), now);
        return team;
    }

    public Team confirmDisband(NameAndId player) throws TeamException {
        this.rememberPlayer(player);
        Team team = this.getRequiredTeamFor(player.id());
        if (!team.isOwner(player.id())) {
            throw new TeamException("commands.bonded_peaks.owner_only");
        }
        if (!this.pendingDisbands.containsKey(player.id())) {
            throw new TeamException("commands.bonded_peaks.confirm.none");
        }

        this.pendingDisbands.remove(player.id());
        this.disband(team);
        return team;
    }

    public Team kick(NameAndId owner, UUID targetId) throws TeamException {
        this.rememberPlayer(owner);
        Team team = this.getRequiredTeamFor(owner.id());
        if (!team.isOwner(owner.id())) {
            throw new TeamException("commands.bonded_peaks.owner_only");
        }
        if (owner.id().equals(targetId)) {
            throw new TeamException("commands.bonded_peaks.kick.self");
        }
        if (!team.hasMember(targetId)) {
            throw new TeamException("commands.bonded_peaks.kick.not_member");
        }

        team.removeMember(targetId);
        this.teamByMember.remove(targetId);
        this.pendingDisbands.remove(targetId);
        this.save();
        return team;
    }

    public Team transfer(NameAndId owner, UUID targetId) throws TeamException {
        this.rememberPlayer(owner);
        Team team = this.getRequiredTeamFor(owner.id());
        if (!team.isOwner(owner.id())) {
            throw new TeamException("commands.bonded_peaks.owner_only");
        }
        if (owner.id().equals(targetId)) {
            throw new TeamException("commands.bonded_peaks.transfer.self");
        }
        if (!team.hasMember(targetId)) {
            throw new TeamException("commands.bonded_peaks.transfer.not_member");
        }

        team.transferOwnership(targetId);
        this.pendingDisbands.remove(owner.id());
        this.pendingDisbands.remove(targetId);
        this.removeInvitesForTeam(team.getName());
        this.save();
        return team;
    }

    public Optional<Team> getTeamByName(String name) {
        return Optional.ofNullable(this.teamsByName.get(normalizeTeamName(name)));
    }

    public Optional<Team> getTeamFor(UUID playerId) {
        String teamKey = this.teamByMember.get(playerId);
        return teamKey == null ? Optional.empty() : Optional.ofNullable(this.teamsByName.get(teamKey));
    }

    public List<Team> listTeams() {
        return this.teamsByName.values().stream()
            .sorted(Comparator.comparing(Team::getCreateTime).thenComparing(Team::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    public List<ServerPlayer> getOnlineMembers(Team team) {
        List<ServerPlayer> players = new ArrayList<>();
        for (UUID memberId : team.getMembers()) {
            ServerPlayer player = this.server.getPlayerList().getPlayer(memberId);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }

    public String resolvePlayerName(UUID playerId) {
        ServerPlayer player = this.server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            return player.getGameProfile().name();
        }
        return this.knownPlayerNames.getOrDefault(playerId, playerId.toString());
    }

    public List<String> resolveMemberNames(Team team) {
        List<String> names = new ArrayList<>();
        for (UUID memberId : team.getMembers()) {
            names.add(this.resolvePlayerName(memberId));
        }
        return names;
    }

    public String formatCreateTime(long createTime) {
        return TIME_FORMATTER.format(Instant.ofEpochMilli(createTime).atZone(ZoneId.systemDefault()));
    }

    public static void validateTeamName(@Nullable String name) throws TeamException {
        if (name == null || name.isBlank()) {
            throw new TeamException("commands.bonded_peaks.create.invalid_blank");
        }
        int length = name.codePointCount(0, name.length());
        if (length > 12) {
            throw new TeamException("commands.bonded_peaks.create.invalid_length");
        }
        if (!TEAM_NAME_PATTERN.matcher(name).matches()) {
            throw new TeamException("commands.bonded_peaks.create.invalid_characters");
        }
    }

    private void validateCanJoinNewTeam(UUID playerId) throws TeamException {
        if (this.teamByMember.containsKey(playerId)) {
            throw new TeamException("commands.bonded_peaks.team.already_in_team.self");
        }
    }

    private Team getRequiredTeamFor(UUID playerId) throws TeamException {
        return this.getTeamFor(playerId).orElseThrow(() -> new TeamException("commands.bonded_peaks.team.none"));
    }

    private void disband(Team team) {
        this.teamsByName.remove(normalizeTeamName(team.getName()));
        for (UUID memberId : team.getMembers()) {
            this.teamByMember.remove(memberId);
            this.pendingDisbands.remove(memberId);
        }
        this.removeInvitesForTeam(team.getName());
        this.save();
    }

    private void clearIncomingInvites(UUID targetId) {
        this.invitesByTarget.remove(targetId);
    }

    private void removeInvitesForTeam(String teamName) {
        String teamKey = normalizeTeamName(teamName);
        this.invitesByTarget.values().removeIf(invites -> {
            invites.removeIf(invite -> normalizeTeamName(invite.teamName()).equals(teamKey));
            return invites.isEmpty();
        });
    }

    private void removeInvite(UUID targetId, TeamInvite invite) {
        List<TeamInvite> invites = this.invitesByTarget.get(targetId);
        if (invites == null) {
            return;
        }
        invites.remove(invite);
        if (invites.isEmpty()) {
            this.invitesByTarget.remove(targetId);
        }
    }

    private void purgeExpiredInvites(long now) {
        this.invitesByTarget.values().removeIf(invites -> {
            invites.removeIf(invite -> invite.isExpired(now));
            return invites.isEmpty();
        });
    }

    private void rememberPlayer(NameAndId player) {
        this.knownPlayerNames.put(player.id(), player.name());
    }

    private void load() {
        if (!Files.exists(this.storageFile)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(this.storageFile, StandardCharsets.UTF_8)) {
            StorageData data = GSON.fromJson(reader, StorageData.class);
            if (data == null) {
                return;
            }
            this.teamsByName.clear();
            this.teamByMember.clear();
            this.knownPlayerNames.clear();

            data.knownPlayerNames.forEach((uuid, name) -> this.knownPlayerNames.put(UUID.fromString(uuid), name));
            for (StoredTeam storedTeam : data.teams) {
                UUID owner = UUID.fromString(storedTeam.owner);
                List<UUID> members = new ArrayList<>();
                for (String member : storedTeam.members) {
                    members.add(UUID.fromString(member));
                }
                Team team = new Team(storedTeam.name, owner, members, storedTeam.createTime);
                String teamKey = normalizeTeamName(team.getName());
                this.teamsByName.put(teamKey, team);
                for (UUID memberId : team.getMembers()) {
                    this.teamByMember.put(memberId, teamKey);
                }
            }
        } catch (IOException | JsonParseException | IllegalArgumentException exception) {
            LOGGER.error("Failed to load bonded peaks team data from {}", this.storageFile, exception);
        }
    }

    private void save() {
        StorageData data = new StorageData();
        for (Map.Entry<UUID, String> entry : this.knownPlayerNames.entrySet()) {
            data.knownPlayerNames.put(entry.getKey().toString(), entry.getValue());
        }
        for (Team team : this.teamsByName.values()) {
            StoredTeam storedTeam = new StoredTeam(
                team.getName(),
                team.getOwner().toString(),
                team.getCreateTime()
            );
            for (UUID memberId : team.getMembers()) {
                storedTeam.members.add(memberId.toString());
            }
            data.teams.add(storedTeam);
        }

        try {
            Files.createDirectories(this.storageFile.getParent());
            try (Writer writer = Files.newBufferedWriter(this.storageFile, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException exception) {
            LOGGER.error("Failed to save bonded peaks team data to {}", this.storageFile, exception);
        }
    }

    private static String normalizeTeamName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public static NameAndId singleProfile(Collection<NameAndId> profiles) throws TeamException {
        Objects.requireNonNull(profiles, "profiles");
        if (profiles.isEmpty()) {
            throw new TeamException("commands.bonded_peaks.player.not_found");
        }
        if (profiles.size() > 1) {
            throw new TeamException("commands.bonded_peaks.player.multiple");
        }
        return profiles.iterator().next();
    }

    public static final class TeamException extends Exception {
        private final String translationKey;
        private final transient Object[] arguments;

        public TeamException(String translationKey, Object... arguments) {
            super(translationKey);
            this.translationKey = translationKey;
            this.arguments = arguments;
        }

        public Component toComponent() {
            return Component.translatable(this.translationKey, this.arguments);
        }
    }

    private static final class StorageData {
        private final List<StoredTeam> teams = new ArrayList<>();
        private final Map<String, String> knownPlayerNames = new LinkedHashMap<>();
    }

    private static final class StoredTeam {
        private final String name;
        private final String owner;
        private final List<String> members = new ArrayList<>();
        private final long createTime;

        private StoredTeam(String name, String owner, long createTime) {
            this.name = name;
            this.owner = owner;
            this.createTime = createTime;
        }
    }
}


