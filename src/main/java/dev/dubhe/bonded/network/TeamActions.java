package dev.dubhe.bonded.network;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import dev.dubhe.bonded.team.TeamManager;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.UUID;

final class TeamActions {
    private static final Map<ServerPlayer, Long> LAST_ACTION = new WeakHashMap<>();

    private TeamActions() {
    }

    static void execute(TeamAction action, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        var server = player.level().getServer();
        long tick = server.overworld().getGameTime();
        Long previous = LAST_ACTION.get(player);
        if (previous != null && tick - previous < 5) {
            context.reply(new TeamReply(action.requestId(), false, Component.translatable("screen.bonded_peaks.busy")));
            return;
        }
        LAST_ACTION.put(player, tick);
        if (!action.validArgument()) {
            context.reply(new TeamReply(action.requestId(), false, Component.translatable("screen.bonded_peaks.invalid_action")));
            return;
        }
        TeamManager teams = TeamManager.get(server);
        String team = teams.getTeamFor(player.getUUID()).map(value -> value.getName()).orElse("");
        if (action.kind() == TeamAction.Kind.ACCEPT) {
            team = teams.getInvitesFor(player.getUUID(), System.currentTimeMillis()).stream()
                .filter(invite -> invite.inviterId().toString().equals(action.argument()))
                .map(invite -> invite.teamName()).findFirst().orElse("");
        }
        if (!team.equals(action.team())) {
            context.reply(new TeamReply(action.requestId(), false, Component.translatable("screen.bonded_peaks.changed")));
            TeamPackets.sendSnapshot(player, context);
            return;
        }
        Feedback feedback = new Feedback();
        var source = player.createCommandSourceStack().withSource(feedback);
        String command = "bondedpeaks " + action.kind().command();
        if (!action.argument().isEmpty()) {
            String argument = switch (action.kind()) {
                case ACCEPT, KICK, TRANSFER -> teams.resolvePlayerName(UUID.fromString(action.argument()));
                default -> action.argument();
            };
            command += " " + argument;
        }
        boolean success;
        try {
            success = server.getCommands().getDispatcher().execute(command, source) > 0;
            if (success && action.kind() == TeamAction.Kind.DISBAND) {
                success = server.getCommands().getDispatcher().execute("bondedpeaks confirm", source) > 0;
            }
        } catch (CommandSyntaxException exception) {
            feedback.message = Component.translatable("screen.bonded_peaks.invalid_action");
            success = false;
        } catch (RuntimeException exception) {
            LogUtils.getLogger().error("Team UI action {} failed for player {} in team {}", action.kind(), player.getUUID(), team, exception);
            feedback.message = Component.translatable("command.failed");
            success = false;
        }
        context.reply(new TeamReply(action.requestId(), success, feedback.message));
        TeamPackets.sendSnapshot(player, context);
    }

    private static final class Feedback implements CommandSource {
        private Component message = Component.translatable("screen.bonded_peaks.done");

        @Override
        public void sendSystemMessage(Component message) {
            this.message = message;
        }

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false;
        }
    }
}
