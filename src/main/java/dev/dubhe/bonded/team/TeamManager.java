package dev.dubhe.bonded.team;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import dev.dubhe.bonded.BondedPeaks;
import dev.dubhe.bonded.network.TeamRosterSync;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.authlib.GameProfile;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class TeamManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<MinecraftServer, TeamManager> INSTANCES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Pattern TEAM_NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}]+$");
    private static final Pattern TEAM_FILE_SAFE_PATTERN = Pattern.compile("[^a-z0-9_-]");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MinecraftServer server;
    private final Path storageDirectory;
    private final Path teamsDirectory;
    private final Path playersFile;
    private final Map<String, Team> teamsByName = new LinkedHashMap<>();
    private final Map<UUID, String> teamByMember = new LinkedHashMap<>();
    private final Map<UUID, List<TeamInvite>> invitesByTarget = new LinkedHashMap<>();
    private final Map<UUID, Long> pendingDisbands = new LinkedHashMap<>();
    private final Map<UUID, String> knownPlayerNames = new LinkedHashMap<>();

    private TeamManager(MinecraftServer server) {
        this.server = server;
        this.storageDirectory = server.getWorldPath(LevelResource.ROOT)
            .resolve("serverconfig")
            .resolve(BondedPeaks.MOD_ID);
        this.teamsDirectory = this.storageDirectory.resolve("teams");
        this.playersFile = this.storageDirectory.resolve("players.json");
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

    public void onPlayerLogin(ServerPlayer player) {
        this.rememberPlayer(new GameProfile(player.getUUID(), player.getGameProfile().getName()));
        this.purgeExpiredInvites(System.currentTimeMillis());
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

    public Team createTeam(GameProfile owner, String name, long now) throws TeamException {
        this.rememberPlayer(owner);
        this.validateCanJoinNewTeam(owner.getId());
        validateTeamName(name);
        String teamKey = normalizeTeamName(name);
        if (this.teamsByName.containsKey(teamKey)) {
            throw new TeamException("commands.bonded_peaks.create.duplicate", name);
        }

        Team team = Team.create(name, owner.getId(), now);
        this.teamsByName.put(teamKey, team);
        this.teamByMember.put(owner.getId(), teamKey);
        this.clearIncomingInvites(owner.getId());
        this.pendingDisbands.remove(owner.getId());
        this.changed();
        return team;
    }

    public Team invite(GameProfile inviter, GameProfile target, long now) throws TeamException {
        this.rememberPlayer(inviter);
        this.rememberPlayer(target);
        this.purgeExpiredInvites(now);

        if (inviter.getId().equals(target.getId())) {
            throw new TeamException("commands.bonded_peaks.invite.self");
        }

        Team team = this.getRequiredTeamFor(inviter.getId());
        if (!team.isOwner(inviter.getId())) {
            throw new TeamException("commands.bonded_peaks.owner_only");
        }
        if (this.teamByMember.containsKey(target.getId())) {
            throw new TeamException("commands.bonded_peaks.team.already_in_team.target", target.getName());
        }

        List<TeamInvite> invites = this.invitesByTarget.computeIfAbsent(target.getId(), ignored -> new ArrayList<>());
        invites.removeIf(invite -> invite.inviterId().equals(inviter.getId()) || normalizeTeamName(invite.teamName()).equals(normalizeTeamName(team.getName())));
        invites.add(new TeamInvite(inviter.getId(), inviter.getName(), team.getName(), target.getId(), now));
        return team;
    }

    public Team acceptInvite(GameProfile target, @Nullable UUID inviterId, long now) throws TeamException {
        this.rememberPlayer(target);
        this.validateCanJoinNewTeam(target.getId());
        this.purgeExpiredInvites(now);

        List<TeamInvite> invites = this.invitesByTarget.getOrDefault(target.getId(), List.of());
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
            this.removeInvite(target.getId(), selected);
            throw new TeamException("commands.bonded_peaks.accept.invalidated");
        }

        team.addMember(target.getId());
        this.teamByMember.put(target.getId(), normalizeTeamName(team.getName()));
        this.clearIncomingInvites(target.getId());
        this.pendingDisbands.remove(target.getId());
        this.changed();
        return team;
    }

    public Team leave(GameProfile player) throws TeamException {
        this.rememberPlayer(player);
        Team team = this.getRequiredTeamFor(player.getId());
        if (team.isOwner(player.getId())) {
            throw new TeamException("commands.bonded_peaks.leave.owner_forbidden");
        }

        team.removeMember(player.getId());
        this.teamByMember.remove(player.getId());
        this.pendingDisbands.remove(player.getId());
        this.changed();
        return team;
    }

    @SuppressWarnings("UnusedReturnValue")
    public Team beginDisband(GameProfile player, long now) throws TeamException {
        this.rememberPlayer(player);
        Team team = this.getRequiredTeamFor(player.getId());
        if (!team.isOwner(player.getId())) {
            throw new TeamException("commands.bonded_peaks.owner_only");
        }

        this.pendingDisbands.put(player.getId(), now);
        return team;
    }

    public Team confirmDisband(GameProfile player) throws TeamException {
        this.rememberPlayer(player);
        Team team = this.getRequiredTeamFor(player.getId());
        if (!team.isOwner(player.getId())) {
            throw new TeamException("commands.bonded_peaks.owner_only");
        }
        if (!this.pendingDisbands.containsKey(player.getId())) {
            throw new TeamException("commands.bonded_peaks.confirm.none");
        }

        this.pendingDisbands.remove(player.getId());
        this.disband(team);
        return team;
    }

    public Team kick(GameProfile owner, UUID targetId) throws TeamException {
        this.rememberPlayer(owner);
        Team team = this.getRequiredTeamFor(owner.getId());
        if (!team.isOwner(owner.getId())) {
            throw new TeamException("commands.bonded_peaks.owner_only");
        }
        if (owner.getId().equals(targetId)) {
            throw new TeamException("commands.bonded_peaks.kick.self");
        }
        if (!team.hasMember(targetId)) {
            throw new TeamException("commands.bonded_peaks.kick.not_member");
        }

        team.removeMember(targetId);
        this.teamByMember.remove(targetId);
        this.pendingDisbands.remove(targetId);
        this.changed();
        return team;
    }

    public Team transfer(GameProfile owner, UUID targetId) throws TeamException {
        this.rememberPlayer(owner);
        Team team = this.getRequiredTeamFor(owner.getId());
        if (!team.isOwner(owner.getId())) {
            throw new TeamException("commands.bonded_peaks.owner_only");
        }
        if (owner.getId().equals(targetId)) {
            throw new TeamException("commands.bonded_peaks.transfer.self");
        }
        if (!team.hasMember(targetId)) {
            throw new TeamException("commands.bonded_peaks.transfer.not_member");
        }

        team.transferOwnership(targetId);
        this.pendingDisbands.remove(owner.getId());
        this.pendingDisbands.remove(targetId);
        this.removeInvitesForTeam(team.getName());
        this.changed();
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

    /** 返回仍有效且队长未变更的邀请，用于玩家自己的邀请列表。 */
    public List<TeamInvite> getInvitesFor(UUID playerId, long now) {
        this.purgeExpiredInvites(now);
        return this.invitesByTarget.getOrDefault(playerId, List.of()).stream()
            .filter(invite -> this.getTeamByName(invite.teamName())
                .filter(team -> team.isOwner(invite.inviterId())).isPresent())
            .toList();
    }

    public String resolvePlayerName(UUID playerId) {
        ServerPlayer player = this.server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            return player.getGameProfile().getName();
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
        this.changed();
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

    private void rememberPlayer(GameProfile player) {
        this.knownPlayerNames.put(player.getId(), player.getName());
    }

    private void load() {
        this.teamsByName.clear();
        this.teamByMember.clear();
        this.knownPlayerNames.clear();

        this.loadPlayers();
        this.loadTeams();
    }

    private void changed() {
        this.save();
        TeamRosterSync.broadcast(this.server);
    }

    private void save() {
        try {
            Files.createDirectories(this.storageDirectory);
            Files.createDirectories(this.teamsDirectory);

            PlayersStorageData playersData = new PlayersStorageData();
            for (Map.Entry<UUID, String> entry : this.knownPlayerNames.entrySet()) {
                playersData.knownPlayerNames.put(entry.getKey().toString(), entry.getValue());
            }
            this.writeJson(this.playersFile, playersData);

            Set<String> expectedTeamFiles = new HashSet<>();
            for (Team team : this.teamsByName.values()) {
                StoredTeam storedTeam = toStoredTeam(team);
                String fileName = teamFileName(team.getName());
                expectedTeamFiles.add(fileName);
                this.writeJson(this.teamsDirectory.resolve(fileName), storedTeam);
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.teamsDirectory, "*.json")) {
                for (Path file : stream) {
                    String currentName = file.getFileName().toString();
                    if (!expectedTeamFiles.contains(currentName)) {
                        Files.deleteIfExists(file);
                    }
                }
            }
        } catch (IOException exception) {
            LOGGER.error("Failed to save bonded peaks team data under {}", this.storageDirectory, exception);
        }
    }

    private void loadPlayers() {
        if (!Files.exists(this.playersFile)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(this.playersFile, StandardCharsets.UTF_8)) {
            PlayersStorageData data = GSON.fromJson(reader, PlayersStorageData.class);
            if (data == null) {
                return;
            }
            data.knownPlayerNames.forEach((uuid, name) -> this.knownPlayerNames.put(UUID.fromString(uuid), name));
        } catch (IOException | JsonParseException | IllegalArgumentException exception) {
            LOGGER.error("Failed to load bonded peaks player cache from {}", this.playersFile, exception);
        }
    }

    private void loadTeams() {
        if (!Files.isDirectory(this.teamsDirectory)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.teamsDirectory, "*.json")) {
            for (Path file : stream) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    StoredTeam storedTeam = GSON.fromJson(reader, StoredTeam.class);
                    Team team = fromStoredTeam(storedTeam);
                    String teamKey = normalizeTeamName(team.getName());
                    if (this.teamsByName.containsKey(teamKey)) {
                        LOGGER.warn("Duplicate team definition found for {}, file {} is ignored", team.getName(), file);
                        continue;
                    }
                    this.teamsByName.put(teamKey, team);
                    for (UUID memberId : team.getMembers()) {
                        this.teamByMember.put(memberId, teamKey);
                    }
                } catch (IOException | JsonParseException | IllegalArgumentException exception) {
                    LOGGER.error("Failed to load bonded peaks team data from {}", file, exception);
                }
            }
        } catch (IOException exception) {
            LOGGER.error("Failed to enumerate bonded peaks team files in {}", this.teamsDirectory, exception);
        }
    }

    private void writeJson(Path path, Object data) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static String teamFileName(String teamName) {
        String normalized = normalizeTeamName(teamName);
        String safeName = TEAM_FILE_SAFE_PATTERN.matcher(normalized).replaceAll("_");
        if (safeName.isBlank()) {
            safeName = "team";
        }
        if (safeName.length() > 32) {
            safeName = safeName.substring(0, 32);
        }
        String hash = UUID.nameUUIDFromBytes(normalized.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "").substring(0, 8);
        return safeName + "_" + hash + ".json";
    }

    private static StoredTeam toStoredTeam(Team team) {
        StoredTeam storedTeam = StoredTeam.builder()
            .name(team.getName())
            .owner(team.getOwner().toString())
            .createTime(team.getCreateTime())
            .build();
        for (UUID memberId : team.getMembers()) {
            storedTeam.members.add(memberId.toString());
        }
        return storedTeam;
    }

    private static Team fromStoredTeam(@Nullable StoredTeam storedTeam) {
        if (storedTeam == null) {
            throw new IllegalArgumentException("Invalid team storage data.");
        }
        UUID owner = UUID.fromString(storedTeam.owner());
        List<UUID> members = new ArrayList<>();
        for (String member : storedTeam.members()) {
            members.add(UUID.fromString(member));
        }
        return new Team(storedTeam.name(), owner, members, storedTeam.createTime());
    }

    private static String normalizeTeamName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public static GameProfile singleProfile(Collection<GameProfile> profiles) throws TeamException {
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


    @Builder
    @AllArgsConstructor
    @Accessors(fluent = true)
    @Getter
    private static final class StoredTeam {
        private final String name;
        private final String owner;
        private final List<String> members = new ArrayList<>();
        private final long createTime;
    }

    private static final class PlayersStorageData {
        private final Map<String, String> knownPlayerNames = new LinkedHashMap<>();
    }
}


