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
        this.add("commands.bonded_peaks.accept.not_found", "No matching invitation from that player was found.");
        this.add("commands.bonded_peaks.accept.invalidated", "That invitation is no longer valid. Ask for a new one.");
        this.add("commands.bonded_peaks.leave.success", "You have left \"%s\".");
        this.add("commands.bonded_peaks.leave.owner_forbidden", "The owner must transfer leadership or disband the team before leaving.");
        this.add("commands.bonded_peaks.disband.pending", "The oath is not broken lightly. Type /bondedpeaks confirm to disband.");
        this.add("commands.bonded_peaks.disband.success", "Team \"%s\" has been disbanded.");
        this.add("commands.bonded_peaks.confirm.none", "There is no pending disband confirmation.");
        this.add("commands.bonded_peaks.kick.success", "%s has been removed from \"%s\".");
        this.add("commands.bonded_peaks.kick.received", "You have been removed from team \"%s\".");
        this.add("commands.bonded_peaks.kick.self", "Use leave to leave the team.");
        this.add("commands.bonded_peaks.kick.not_member", "That player is not a member of this team.");
        this.add("commands.bonded_peaks.transfer.success", "Leadership of \"%2$s\" has been transferred to %1$s.");
        this.add("commands.bonded_peaks.transfer.self", "You are already the owner.");
        this.add("commands.bonded_peaks.transfer.not_member", "That player is not a member of this team.");
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
        this.add("key.category.bonded_peaks.team", "Bonded Peaks");
        this.add("key.bonded_peaks.open", "Open team");
        this.add("screen.bonded_peaks.title", "Bonded Peaks");
        this.add("screen.bonded_peaks.tagline", "Together, beyond the peaks");
        this.add("screen.bonded_peaks.team_name", "%s");
        this.add("screen.bonded_peaks.member_name", "%s");
        this.add("screen.bonded_peaks.summary", "%1$s online / %2$s members");
        this.add("screen.bonded_peaks.members", "Members");
        this.add("screen.bonded_peaks.chat", "Team chat");
        this.add("screen.bonded_peaks.invitations", "Invitations");
        this.add("screen.bonded_peaks.close", "Close");
        this.add("screen.bonded_peaks.cancel", "Cancel");
        this.add("screen.bonded_peaks.create", "Create team");
        this.add("screen.bonded_peaks.invite", "Invite player");
        this.add("screen.bonded_peaks.accept", "Join team");
        this.add("screen.bonded_peaks.transfer", "Transfer leadership");
        this.add("screen.bonded_peaks.kick", "Remove member");
        this.add("screen.bonded_peaks.leave", "Leave team");
        this.add("screen.bonded_peaks.disband", "Disband team");
        this.add("screen.bonded_peaks.owner", "Team owner");
        this.add("screen.bonded_peaks.member", "Member");
        this.add("screen.bonded_peaks.online", "Online");
        this.add("screen.bonded_peaks.offline", "Offline");
        this.add("screen.bonded_peaks.loading", "Loading your team...");
        this.add("screen.bonded_peaks.unsupported", "This server does not support the Bonded Peaks team interface.");
        this.add("screen.bonded_peaks.ready", "Select a member to view details");
        this.add("screen.bonded_peaks.pending", "Waiting for the server...");
        this.add("screen.bonded_peaks.done", "Action completed");
        this.add("screen.bonded_peaks.busy", "Please wait a moment before trying again.");
        this.add("screen.bonded_peaks.changed", "Your team has changed. Reopen this action.");
        this.add("screen.bonded_peaks.invalid_action", "This action could not be processed. Check the player or team name.");
        this.add("screen.bonded_peaks.waiting", "The server has not replied. Check your connection.");
        this.add("screen.bonded_peaks.no_team", "Your journey starts here");
        this.add("screen.bonded_peaks.create_hint", "Create a team or accept an invitation.");
        this.add("screen.bonded_peaks.no_invites", "No active invitations. Ask a team owner to invite you.");
        this.add("screen.bonded_peaks.invited_by", "From %s");
        this.add("screen.bonded_peaks.expires", "Expires in %s seconds");
        this.add("screen.bonded_peaks.chat_hint", "Message your team...");
        this.add("screen.bonded_peaks.chat_empty", "Team messages received during this connection appear here.");
        this.add("screen.bonded_peaks.send", "Send");
        this.add("screen.bonded_peaks.name_hint", "Team name");
        this.add("screen.bonded_peaks.player_hint", "Player name");
        this.add("screen.bonded_peaks.create_description", "Enter up to 12 letters or digits for your team name.");
        this.add("screen.bonded_peaks.invite_description", "Select an online player or enter a known player's name.");
        this.add("screen.bonded_peaks.transfer_description", "Transfer leadership of %1$s to %2$s? You will become a member.");
        this.add("screen.bonded_peaks.kick_description", "Remove %2$s from %1$s? A new invitation will be required to rejoin.");
        this.add("screen.bonded_peaks.leave_description", "Leave %1$s? You will need a new invitation to rejoin.");
        this.add("screen.bonded_peaks.disband_description", "Disband %1$s? All members will leave and pending invitations will expire.");
    }
}
