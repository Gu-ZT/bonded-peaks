package dev.dubhe.bonded.data.lang;

import dev.dubhe.bonded.BondedPeaks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class BondedPeaksLangProvider extends LanguageProvider {
    public BondedPeaksLangProvider(PackOutput output) {
        super(output, BondedPeaks.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        this.add(
            "commands.bonded_peaks.help.1",
            "Bonded Peaks commands: create, invite, accept, leave, disband, confirm, kick, transfer, list, info, chat."
        );
        this.add("commands.bonded_peaks.help.2", "Use /bondedpeaks chat <message...> for team chat, or the shortcut /bp <message...>.");
        this.add("commands.bonded_peaks.help.3", "Team names may only contain letters or digits, up to 12 characters.");
        this.add("commands.bonded_peaks.create.success", "Sworn by mountains and rivers, team \"%s\" is now formed.");
        this.add("commands.bonded_peaks.create.duplicate", "The team name \"%s\" is already taken.");
        this.add("commands.bonded_peaks.create.invalid_blank", "Team name cannot be empty.");
        this.add("commands.bonded_peaks.create.invalid_length", "Team name must be at most 12 characters long.");
        this.add("commands.bonded_peaks.create.invalid_characters", "Team name may only contain letters and digits.");
        this.add("commands.bonded_peaks.invite.sent", "The invitation has been sent to %s.");
        this.add("commands.bonded_peaks.invite.received", "%s invites you to join team \"%s\". Use /bondedpeaks accept within 60 seconds.");
        this.add("commands.bonded_peaks.invite.self", "You cannot invite yourself.");
        this.add("commands.bonded_peaks.accept.success", "The bond is forged. You joined \"%s\".");
        this.add("commands.bonded_peaks.accept.none", "There is no active invitation to accept.");
        this.add("commands.bonded_peaks.accept.not_found", "No matching invitation from that inviter was found.");
        this.add("commands.bonded_peaks.accept.invalidated", "That invitation is no longer valid. Ask for a new one.");
        this.add("commands.bonded_peaks.leave.success", "You have left \"%s\".");
        this.add("commands.bonded_peaks.leave.owner_forbidden", "The owner must transfer leadership or disband the team before leaving.");
        this.add("commands.bonded_peaks.disband.pending", "The oath is not broken lightly. Type /bondedpeaks confirm to disband.");
        this.add("commands.bonded_peaks.disband.success", "Team \"%s\" has been disbanded.");
        this.add("commands.bonded_peaks.confirm.none", "There is no pending disband confirmation.");
        this.add("commands.bonded_peaks.kick.success", "%s has been removed from \"%s\".");
        this.add("commands.bonded_peaks.kick.received", "You have been removed from team \"%s\".");
        this.add("commands.bonded_peaks.kick.self", "Use leave if you want to leave yourself.");
        this.add("commands.bonded_peaks.kick.not_member", "That player is not a member of this team.");
        this.add("commands.bonded_peaks.transfer.success", "Leadership of \"%2$s\" has been transferred to %1$s.");
        this.add("commands.bonded_peaks.transfer.self", "You are already the owner.");
        this.add("commands.bonded_peaks.transfer.not_member", "That player is not a member of the team.");
        this.add("commands.bonded_peaks.list.empty", "There are no teams on this server yet.");
        this.add("commands.bonded_peaks.list.header", "There are %s teams on this server:");
        this.add("commands.bonded_peaks.list.entry", "- \"%s\": %s members, owner %s");
        this.add("commands.bonded_peaks.info.header", "Team \"%s\" details:");
        this.add("commands.bonded_peaks.info.owner", "Owner: %s");
        this.add("commands.bonded_peaks.info.count", "Members: %s");
        this.add("commands.bonded_peaks.info.members", "Roster: %s");
        this.add("commands.bonded_peaks.info.created_at", "Created at: %s");
        this.add("commands.bonded_peaks.info.not_found", "No team named \"%s\" was found.");
        this.add("commands.bonded_peaks.chat.format", "[Bonded · %1$s] %2$s: %3$s");
        this.add("commands.bonded_peaks.owner_only", "Only the team owner may use this command.");
        this.add("commands.bonded_peaks.team.none", "You are not currently in a team.");
        this.add("commands.bonded_peaks.team.already_in_team.self", "You are already in a team. Leave it before joining another.");
        this.add("commands.bonded_peaks.team.already_in_team.target", "%s is already in a team and cannot be invited.");
        this.add(
            "commands.bonded_peaks.player.not_found",
            "That player could not be found. Only players known to the server can be targeted."
        );
        this.add("commands.bonded_peaks.player.multiple", "Please target exactly one player.");
        this.add("commands.bonded_peaks.member.joined", "%s joined \"%s\".");
        this.add("commands.bonded_peaks.member.left", "%s left \"%s\".");
        this.add("commands.bonded_peaks.member.kicked", "%s was removed from \"%s\".");
    }
}
