package dev.dubhe.bonded.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.dubhe.bonded.team.Team;
import dev.dubhe.bonded.team.TeamManager;
import dev.dubhe.bonded.team.TeamManager.TeamException;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import org.apache.commons.lang3.function.TriFunction;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("resource")
@Slf4j
public final class BondedPeaksCommands {
    private BondedPeaksCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("bondedpeaks")
                .executes(BondedPeaksCommands::showHelp)
                .then(
                    Commands.literal("create")
                        .then(
                            Commands.argument("name", StringArgumentType.word())
                                .executes(BondedPeaksCommands::createTeam)
                        )
                )
                .then(
                    Commands.literal("invite")
                        .then(
                            Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(BondedPeaksCommands::invitePlayer)
                        )
                )
                .then(
                    Commands.literal("accept")
                        .executes(context -> acceptInvite(context, null))
                        .then(
                            Commands.argument("inviter", GameProfileArgument.gameProfile())
                                .executes(BondedPeaksCommands::acceptInviteByInviter)
                        )
                )
                .then(
                    Commands.literal("leave")
                        .executes(BondedPeaksCommands::leaveTeam)
                )
                .then(
                    Commands.literal("disband")
                        .executes(BondedPeaksCommands::disbandTeam)
                )
                .then(
                    Commands.literal("confirm")
                        .executes(BondedPeaksCommands::confirmDisband)
                )
                .then(
                    Commands.literal("kick")
                        .then(
                            Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(BondedPeaksCommands::kickPlayer)
                        )
                )
                .then(
                    Commands.literal("transfer")
                        .then(
                            Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(BondedPeaksCommands::transferOwner)
                        )
                )
                .then(
                    Commands.literal("list")
                        .executes(BondedPeaksCommands::listTeams)
                )
                .then(
                    Commands.literal("info")
                        .executes(BondedPeaksCommands::showOwnTeamInfo)
                        .then(
                            Commands.argument("name", StringArgumentType.word())
                                .executes(BondedPeaksCommands::showNamedTeamInfo)
                        )
                )
                .then(
                    Commands.literal("chat")
                        .then(
                            Commands.argument("message", StringArgumentType.greedyString())
                                .executes(BondedPeaksCommands::chat)
                        )
                )
        );

        dispatcher.register(Commands.literal("bp")
            .then(
                Commands.argument("message", StringArgumentType.greedyString())
                    .executes(BondedPeaksCommands::chat))
        );
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        BondedPeaksCommands.sendSuccess(context.getSource(), Component.translatable("commands.bonded_peaks.help.1"));
        BondedPeaksCommands.sendSuccess(context.getSource(), Component.translatable("commands.bonded_peaks.help.2"));
        BondedPeaksCommands.sendSuccess(context.getSource(), Component.translatable("commands.bonded_peaks.help.3"));
        return 1;
    }

    private static int createTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        TeamManager manager = TeamManager.get(context.getSource().getServer());
        String teamName = StringArgumentType.getString(context, "name");
        NameAndId owner = nameAndId(player);

        try {
            Team team = manager.createTeam(owner, teamName, context.getSource().getServer().overworld().getGameTime());
            BondedPeaksCommands.sendSuccess(
                context.getSource(),
                Component.translatable("commands.bonded_peaks.create.success", team.getName())
            );
            return 1;
        } catch (TeamException exception) {
            BondedPeaksCommands.sendFailure(context.getSource(), exception);
            return 0;
        }
    }

    private static int invitePlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        TeamManager manager = TeamManager.get(context.getSource().getServer());
        NameAndId inviter = nameAndId(player);

        try {
            NameAndId target = resolveSingleProfile(context, "player");
            Team team = manager.invite(inviter, target, context.getSource().getServer().overworld().getGameTime());
            BondedPeaksCommands.sendSuccess(
                context.getSource(),
                Component.translatable("commands.bonded_peaks.invite.sent", target.name())
            );

            ServerPlayer targetPlayer = context.getSource().getServer().getPlayerList().getPlayer(target.id());
            if (targetPlayer != null) {
                targetPlayer.sendSystemMessage(Component.translatable(
                    "commands.bonded_peaks.invite.received",
                    inviter.name(),
                    team.getName()
                ));
            }
            return 1;
        } catch (TeamException exception) {
            BondedPeaksCommands.sendFailure(context.getSource(), exception);
            return 0;
        }
    }

    private static int acceptInviteByInviter(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            return BondedPeaksCommands.acceptInvite(context, BondedPeaksCommands.resolveSingleProfile(context, "inviter").id());
        } catch (TeamException exception) {
            BondedPeaksCommands.sendFailure(context.getSource(), exception);
            return 0;
        }
    }

    private static int acceptInvite(CommandContext<CommandSourceStack> context, @Nullable UUID inviterId) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        TeamManager manager = TeamManager.get(context.getSource().getServer());
        NameAndId target = BondedPeaksCommands.nameAndId(player);

        try {
            Team team = manager.acceptInvite(target, inviterId, context.getSource().getServer().overworld().getGameTime());
            Component joinedMessage = Component.translatable("commands.bonded_peaks.accept.success", team.getName());
            player.sendSystemMessage(joinedMessage);
            Component notice = Component.translatable("commands.bonded_peaks.member.joined", target.name(), team.getName());
            for (ServerPlayer member : manager.getOnlineMembers(team)) {
                if (!member.getUUID().equals(player.getUUID())) {
                    member.sendSystemMessage(notice);
                }
            }
            return 1;
        } catch (TeamException exception) {
            BondedPeaksCommands.sendFailure(context.getSource(), exception);
            return 0;
        }
    }

    private static int leaveTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        TeamManager manager = TeamManager.get(context.getSource().getServer());
        NameAndId member = BondedPeaksCommands.nameAndId(player);

        try {
            Team team = manager.leave(member);
            BondedPeaksCommands.sendSuccess(
                context.getSource(),
                Component.translatable("commands.bonded_peaks.leave.success", team.getName())
            );
            Component notice = Component.translatable("commands.bonded_peaks.member.left", member.name(), team.getName());
            for (ServerPlayer onlineMember : manager.getOnlineMembers(team)) {
                onlineMember.sendSystemMessage(notice);
            }
            return 1;
        } catch (TeamException exception) {
            BondedPeaksCommands.sendFailure(context.getSource(), exception);
            return 0;
        }
    }

    private static int disbandTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        TeamManager manager = TeamManager.get(context.getSource().getServer());

        try {
            manager.beginDisband(nameAndId(player), context.getSource().getServer().overworld().getGameTime());
            BondedPeaksCommands.sendSuccess(context.getSource(), Component.translatable("commands.bonded_peaks.disband.pending"));
            return 1;
        } catch (TeamException exception) {
            BondedPeaksCommands.sendFailure(context.getSource(), exception);
            return 0;
        }
    }

    private static int confirmDisband(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        TeamManager manager = TeamManager.get(context.getSource().getServer());

        try {
            Team team = manager.confirmDisband(nameAndId(player));
            Component notice = Component.translatable("commands.bonded_peaks.disband.success", team.getName());
            for (UUID memberId : team.getMembers()) {
                ServerPlayer member = context.getSource().getServer().getPlayerList().getPlayer(memberId);
                if (member != null && !member.getUUID().equals(player.getUUID())) {
                    member.sendSystemMessage(notice);
                }
            }
            BondedPeaksCommands.sendSuccess(context.getSource(), notice);
            return 1;
        } catch (TeamException exception) {
            BondedPeaksCommands.sendFailure(context.getSource(), exception);
            return 0;
        }
    }

    private static int kickPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        TeamManager manager = TeamManager.get(context.getSource().getServer());

        try {
            NameAndId target = BondedPeaksCommands.resolveSingleProfile(context, "player");
            Team team = manager.kick(nameAndId(player), target.id());
            BondedPeaksCommands.sendSuccess(
                context.getSource(),
                Component.translatable("commands.bonded_peaks.kick.success", target.name(), team.getName())
            );
            ServerPlayer targetPlayer = context.getSource().getServer().getPlayerList().getPlayer(target.id());
            if (targetPlayer != null) {
                targetPlayer.sendSystemMessage(Component.translatable("commands.bonded_peaks.kick.received", team.getName()));
            }
            Component notice = Component.translatable("commands.bonded_peaks.member.kicked", target.name(), team.getName());
            for (ServerPlayer onlineMember : manager.getOnlineMembers(team)) {
                if (!onlineMember.getUUID().equals(player.getUUID())) {
                    onlineMember.sendSystemMessage(notice);
                }
            }
            return 1;
        } catch (TeamException exception) {
            BondedPeaksCommands.sendFailure(context.getSource(), exception);
            return 0;
        }
    }

    private static int transferOwner(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        TeamManager manager = TeamManager.get(context.getSource().getServer());

        try {
            NameAndId target = BondedPeaksCommands.resolveSingleProfile(context, "player");
            Team team = manager.transfer(BondedPeaksCommands.nameAndId(player), target.id());
            Component notice = Component.translatable("commands.bonded_peaks.transfer.success", target.name(), team.getName());
            BondedPeaksCommands.sendSuccess(context.getSource(), notice);
            ServerPlayer targetPlayer = context.getSource().getServer().getPlayerList().getPlayer(target.id());
            if (targetPlayer != null && !targetPlayer.getUUID().equals(player.getUUID())) {
                targetPlayer.sendSystemMessage(notice);
            }
            for (ServerPlayer onlineMember : manager.getOnlineMembers(team)) {
                if (!onlineMember.getUUID().equals(player.getUUID()) && !onlineMember.getUUID().equals(target.id())) {
                    onlineMember.sendSystemMessage(notice);
                }
            }
            return 1;
        } catch (TeamException exception) {
            BondedPeaksCommands.sendFailure(context.getSource(), exception);
            return 0;
        }
    }

    private static int listTeams(CommandContext<CommandSourceStack> context) {
        TeamManager manager = TeamManager.get(context.getSource().getServer());
        List<Team> teams = manager.listTeams();
        if (teams.isEmpty()) {
            BondedPeaksCommands.sendSuccess(context.getSource(), Component.translatable("commands.bonded_peaks.list.empty"));
            return 1;
        }

        BondedPeaksCommands.sendSuccess(context.getSource(), Component.translatable("commands.bonded_peaks.list.header", teams.size()));
        for (Team team : teams) {
            BondedPeaksCommands.sendSuccess(
                context.getSource(), Component.translatable(
                    "commands.bonded_peaks.list.entry",
                    team.getName(),
                    team.getMemberCount(),
                    manager.resolvePlayerName(team.getOwner())
                )
            );
        }
        return teams.size();
    }

    private static int showOwnTeamInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return BondedPeaksCommands.preprocessTeamAndPlayer(
            context,
            (_, manager, team) -> {
                renderTeamInfo(context.getSource(), manager, team);
                return 1;
            }
        );
    }

    private static int showNamedTeamInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return BondedPeaksCommands.preprocessTeamAndPlayer(
            context,
            (_, manager, team) -> {
                renderTeamInfo(context.getSource(), manager, team);
                return 1;
            }
        );
    }

    private static int chat(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return BondedPeaksCommands.preprocessTeamAndPlayer(
            context,
            (player, manager, team) -> {
                String message = StringArgumentType.getString(context, "message");
                Component formatted = Component.translatable(
                    "commands.bonded_peaks.chat.format",
                    team.getName(),
                    player.getDisplayName(),
                    Component.literal(message)
                );
                for (ServerPlayer member : manager.getOnlineMembers(team)) {
                    member.sendSystemMessage(formatted);
                }
                return 1;
            }
        );
    }

    private static int preprocessTeamAndPlayer(
        CommandContext<CommandSourceStack> context,
        TriFunction<ServerPlayer, TeamManager, Team, Integer> consumer
    ) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        TeamManager manager = TeamManager.get(context.getSource().getServer());
        Team team = manager.getTeamFor(player.getUUID()).orElse(null);
        if (team == null) {
            BondedPeaksCommands.sendFailure(context.getSource(), new TeamException("commands.bonded_peaks.team.none"));
            return 0;
        }
        return consumer.apply(player, manager, team);
    }

    private static void renderTeamInfo(CommandSourceStack source, TeamManager manager, Team team) {
        BondedPeaksCommands.sendSuccess(source, Component.translatable("commands.bonded_peaks.info.header", team.getName()));
        BondedPeaksCommands.sendSuccess(
            source,
            Component.translatable("commands.bonded_peaks.info.owner", manager.resolvePlayerName(team.getOwner()))
        );
        BondedPeaksCommands.sendSuccess(source, Component.translatable("commands.bonded_peaks.info.count", team.getMemberCount()));
        BondedPeaksCommands.sendSuccess(
            source,
            Component.translatable("commands.bonded_peaks.info.members", String.join("、", manager.resolveMemberNames(team)))
        );
        BondedPeaksCommands.sendSuccess(
            source,
            Component.translatable("commands.bonded_peaks.info.created_at", manager.formatCreateTime(team.getCreateTime()))
        );
    }

    private static NameAndId resolveSingleProfile(CommandContext<CommandSourceStack> context, String argumentName) throws TeamException {
        try {
            Collection<NameAndId> profiles = GameProfileArgument.getGameProfiles(context, argumentName);
            return TeamManager.singleProfile(profiles);
        } catch (TeamException exception) {
            log.error(exception.getLocalizedMessage(), exception);
            throw exception;
        } catch (CommandSyntaxException exception) {
            throw new TeamException("commands.bonded_peaks.player.not_found");
        }
    }

    private static NameAndId nameAndId(ServerPlayer player) {
        return new NameAndId(player.getUUID(), player.getGameProfile().name());
    }

    private static void sendSuccess(CommandSourceStack source, Component component) {
        source.sendSuccess(() -> component, false);
    }

    private static void sendFailure(CommandSourceStack source, TeamException exception) {
        source.sendFailure(exception.toComponent());
    }
}



