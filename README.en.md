<div align="center">

<img src="src/main/resources/pack.png" width="256" height="256" alt="Bonded Peaks icon">

# Bonded Peaks

**Sworn by the mountains and rivers, bonded as one team.**

[简体中文](README.md) | English

</div>

**Bonded Peaks** (山河同契) is a lightweight team/party mod for **NeoForge 26.1 / Minecraft 26.1**. Create teams, invite friends to join, talk over a team-only chat channel, and let the team owner manage members. All data is persisted with the world save and survives server restarts.

Current version: `0.0.1` (early development)

## Features

- Create and disband teams, team names up to 12 characters
- Invite-only joining: owner invites → player accepts (invites expire after 60 seconds)
- Team-only chat: `/bp <message>` quick command
- Full owner management: kick, transfer ownership, two-step confirmed disband
- Team list and detail queries; offline members shown from a cached name registry
- Data auto-persisted with the world save

## Requirements

| Item | Requirement |
|---|---|
| Minecraft | 26.1.2 (declared range `[26.1, 26.2)`) |
| NeoForge | 26.1.2.8-beta or newer (declared range `[26,)`) |
| Java | JDK 25 required to build |
| Dependencies | AnvilLib (network, config modules), bundled via JarJar — no separate install |

## Installation

1. Download `bonded-peaks-26.1.2-<version>.jar` (the main jar only; dependencies are bundled)
2. Place it into the client's `.minecraft/mods/` folder, or the server's `mods/` folder
3. Launch the game and type `/bondedpeaks` in chat to see the help

> [!TIP]
> All logic runs on the server, but message text is translated on the client. If a client does not have the mod installed, they will see raw translation keys (e.g. `commands.bonded_peaks.help.1`). **It is recommended that the server and all players install the mod.**

## User Guide

### Basics

- Each team has exactly one **Owner**; everyone else is a **Member**
- A player can belong to only one team at a time
- Owner-only powers: invite, kick, transfer ownership, disband
- Member actions: leave the team, team chat

### Command Reference

| Command | Permission | Description |
|---|---|---|
| `/bondedpeaks` | Everyone | Show help |
| `/bondedpeaks create <name>` | Everyone | Create a team and become its owner |
| `/bondedpeaks invite <player>` | Owner | Invite a player to your team |
| `/bondedpeaks accept` | Everyone | Accept the most recent invite you received |
| `/bondedpeaks accept <inviter>` | Everyone | Accept an invite from a specific inviter |
| `/bondedpeaks leave` | Member | Leave your current team |
| `/bondedpeaks disband` | Owner | Start disbanding (requires confirmation) |
| `/bondedpeaks confirm` | Owner | Confirm and disband the team |
| `/bondedpeaks kick <player>` | Owner | Remove a member from the team |
| `/bondedpeaks transfer <player>` | Owner | Transfer ownership to a member |
| `/bondedpeaks list` | Everyone | List all teams on the server |
| `/bondedpeaks info [name]` | Everyone | Show team details (your own team if no name given) |
| `/bondedpeaks chat <message>` | Member | Send a message to the team; shorthand `/bp <message>` |

### Common Flows

**Create a team**

```
/bondedpeaks create 山河
```

Name rules:

- Letters and digits only (Unicode letters such as Chinese are allowed); no spaces or symbols
- Up to 12 characters long
- Case-insensitive (`Abc` and `abc` are the same name); must not collide with an existing team

**Invite and join**

1. The owner runs `/bondedpeaks invite <player>` to send an invite
2. The invited player runs `/bondedpeaks accept` to join; with multiple invites pending, `/bondedpeaks accept <inviter>` picks a specific one
3. Invites expire **60 seconds** after being sent; the owner must invite again afterwards
4. The target only needs to have been recognized by the server before (i.e. logged in once); they may be invited while offline and will be reminded of pending invites when they log in

**Team chat**

```
/bp Meet at the summit at 7pm!
```

Messages are delivered to all online members as `[Team·name] player: message`; only team members see them.

**Manage the team (owner)**

- Kick: `/bondedpeaks kick <player>`
- Transfer ownership: `/bondedpeaks transfer <player>` (target must be a member; all pending invites for the team are invalidated)
- Disband: run `/bondedpeaks disband`, then `/bondedpeaks confirm`
  - If the owner logs out before confirming, the pending disband is cancelled
  - All members are removed and related invites invalidated

**Leave the team (member)**

```
/bondedpeaks leave
```

The owner cannot leave directly; transfer ownership first, or disband the team.

### Rules & Limitations

- One team per player; you cannot create or accept invites while already in a team
- The invite target must not be in any team; you cannot invite yourself
- The owner cannot kick themselves (use `leave` or `transfer`) nor transfer ownership to themselves
- A pending disband is cancelled if the owner logs out
- Invites and pending disbands live in memory only and are **lost on server restart** — invite / disband again
- All commands require an in-game player; the console cannot use them
- No permission system is integrated yet; every player can use all commands

### Data Storage & Backups

Team data is stored under `serverconfig/bonded_peaks/` inside the world save:

```
<world>/serverconfig/bonded_peaks/
├── teams/           # one JSON file per team (<name>_<hash>.json)
└── players.json     # player name cache (for offline member names)
```

Data is saved automatically on every change; backing up the world save backs up team data too. **Do not edit these files while the server is running.**

## Building from Source

Requirements: JDK 25 + Git

```bash
git clone <repository-url>
cd bonded-peaks
./gradlew build
```

Artifacts land in `build/libs/`:

- `bonded-peaks-26.1.2-<version>.jar` — main jar
- `bonded-peaks-26.1.2-<version>-sources.jar` — sources jar

Common dev commands:

```bash
./gradlew runClient    # run the client
./gradlew runServer    # run the server
./gradlew runData      # run data generation
./gradlew build        # build
```

Report issues via GitHub Issues and contribute via Pull Requests (PRs run an automated build check).

## FAQ

**Q: Messages show as English key names (e.g. `commands.bonded_peaks.help.1`)?**

A: The client does not have the mod installed, so it cannot translate the text. Have all players install the mod.

**Q: Accepting an invite says the invite is no longer valid?**

A: The invite expired (60 s), or the team was disbanded / ownership changed. Ask the owner to invite again.

**Q: How does the owner leave the team?**

A: Run `/bondedpeaks transfer <member>` first, then `/bondedpeaks leave`; or disband the team.

**Q: Do teams survive a server restart?**

A: Yes. Team data is persisted with the world save; only pending invites and disbands are lost.

**Q: Can the console use these commands?**

A: No. Every command requires an in-game player.

## License

- Code: `GNU LGPL 3.0` (see [`LICENSE`](./LICENSE))
- Assets: all rights reserved unless explicitly stated (see [`ASSETS_LICENSE`](./ASSETS_LICENSE))
