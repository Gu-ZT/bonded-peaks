package dev.dubhe.bonded;

import dev.dubhe.bonded.command.BondedPeaksCommands;
import dev.dubhe.bonded.team.TeamManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Objects;

@Mod(BondedPeaks.MOD_ID)
@SuppressWarnings("resource")
public class BondedPeaks {
    public static final String MOD_ID = "bonded_peaks";

    public BondedPeaks(IEventBus modEventBus, ModContainer modContainer) {
        Objects.requireNonNull(modEventBus, "modEventBus");
        Objects.requireNonNull(modContainer, "modContainer");
        NeoForge.EVENT_BUS.addListener(BondedPeaks::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(BondedPeaks::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(BondedPeaks::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(BondedPeaks::onServerStopped);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        BondedPeaksCommands.register(event.getDispatcher());
    }

    private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            TeamManager.get(player.level().getServer()).onPlayerLogin(player);
        }
    }

    private static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            TeamManager.get(player.level().getServer()).onPlayerLogout(player);
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        TeamManager.unload(event.getServer());
    }
}
