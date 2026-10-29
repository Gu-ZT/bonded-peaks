package dev.dubhe.bonded.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.dubhe.bonded.BondedPeaks;
import dev.dubhe.bonded.network.TeamAction;
import dev.dubhe.bonded.network.TeamPackets;
import dev.dubhe.bonded.network.TeamReply;
import dev.dubhe.bonded.network.TeamSnapshot;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** 注册客户端界面入口，并将服务端反馈交给当前队伍界面。 */
@EventBusSubscriber(modid = BondedPeaks.MOD_ID, value = Dist.CLIENT)
public final class TeamClient {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
        Identifier.fromNamespaceAndPath(BondedPeaks.MOD_ID, "team"));
    private static final KeyMapping OPEN = new KeyMapping("key.bonded_peaks.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    private static final List<Component> MESSAGES = new ArrayList<>();
    private static String chatTeam = "";

    private TeamClient() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN);
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(TeamSnapshot.TYPE, (snapshot, context) -> {
            if (!chatTeam.equals(snapshot.name())) {
                MESSAGES.clear();
                chatTeam = snapshot.name();
            }
            if (Minecraft.getInstance().screen instanceof TeamScreen screen) {
                screen.receive(snapshot);
            } else if (Minecraft.getInstance().screen instanceof TeamFormScreen form) {
                form.parent.receive(snapshot);
            }
        });
        event.register(TeamReply.TYPE, (reply, context) -> {
            if (Minecraft.getInstance().screen instanceof TeamScreen screen) {
                screen.reply(reply);
            } else if (Minecraft.getInstance().screen instanceof TeamFormScreen form) {
                form.reply(reply);
            }
        });
    }

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(LiteralArgumentBuilder.<net.minecraft.commands.CommandSourceStack>literal("bpui")
            .executes(context -> {
                Minecraft.getInstance().schedule(TeamClient::open);
                return 1;
            }));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        while (OPEN.consumeClick()) {
            if (Minecraft.getInstance().screen == null) {
                open();
            }
        }
    }

    private static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.setScreen(new TeamScreen());
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        MESSAGES.clear();
        chatTeam = "";
    }

    @SubscribeEvent
    public static void message(ClientChatReceivedEvent.System event) {
        if (!(event.getMessage().getContents() instanceof TranslatableContents contents)) {
            return;
        }
        String key = contents.getKey();
        if (!key.startsWith("commands.bonded_peaks.")) {
            return;
        }
        if (key.equals("commands.bonded_peaks.chat.format")) {
            Object[] arguments = contents.getArgs();
            if (arguments.length > 0 && arguments[0] instanceof String name) {
                if (!chatTeam.equals(name)) {
                    MESSAGES.clear();
                    chatTeam = name;
                }
                MESSAGES.add(event.getMessage().copy());
                if (MESSAGES.size() > 100) {
                    MESSAGES.removeFirst();
                }
                if (Minecraft.getInstance().screen instanceof TeamScreen screen) {
                    screen.chatReceived(event.getMessage());
                } else if (Minecraft.getInstance().screen instanceof TeamFormScreen form) {
                    form.parent.chatReceived(event.getMessage());
                }
            }
        }
    }

    static List<Component> messages() {
        return List.copyOf(MESSAGES);
    }

    static boolean supported() {
        var connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(TeamPackets.Request.TYPE) && connection.hasChannel(TeamAction.TYPE);
    }

    static void refresh() {
        if (supported()) {
            ClientPacketDistributor.sendToServer(new TeamPackets.Request());
        }
    }
}
