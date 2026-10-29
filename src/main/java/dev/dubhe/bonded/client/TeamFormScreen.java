package dev.dubhe.bonded.client;

import dev.dubhe.bonded.network.TeamAction;
import dev.dubhe.bonded.network.TeamReply;
import dev.dubhe.bonded.network.TeamSnapshot;
import dev.dubhe.bonded.team.TeamManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class TeamFormScreen extends Screen {
    final TeamScreen parent;
    private final Action action;
    private final TeamSnapshot.@Nullable Member target;
    private final String team;
    private @Nullable EditBox input;
    private @Nullable TeamButton submit;
    private @Nullable Component error;
    private String draft = "";
    private boolean pending;
    private @Nullable UUID requestId;
    private int pendingTicks;
    private int left;
    private int top;
    private int formWidth;
    private int formHeight;
    private int playerScroll;
    private final List<TeamButton> playerButtons = new ArrayList<>();
    private List<String> playerNames = List.of();

    TeamFormScreen(TeamScreen parent, Action action, TeamSnapshot.@Nullable Member target) {
        super(TeamScreen.text(action.key));
        this.parent = parent;
        this.action = action;
        this.target = target;
        this.team = parent.snapshot().name();
    }

    @Override
    protected void init() {
        this.playerButtons.clear();
        if (this.input != null) {
            this.draft = this.input.getValue();
        }
        this.formWidth = Math.min(360, this.width - 32);
        this.formHeight = Math.min(this.action == Action.INVITE ? 270 : 190, this.height - 32);
        this.left = (this.width - this.formWidth) / 2;
        this.top = (this.height - this.formHeight) / 2;
        if (this.action == Action.CREATE || this.action == Action.INVITE) {
            this.input = this.addRenderableWidget(new EditBox(this.font, this.left + 16, this.top + 62,
                this.formWidth - 32, 20, TeamScreen.text(this.action == Action.CREATE ? "name_hint" : "player_hint")));
            this.input.setMaxLength(this.action == Action.CREATE ? 24 : 16);
            this.input.setHint(TeamScreen.text(this.action == Action.CREATE ? "name_hint" : "player_hint"));
            this.input.setValue(this.draft);
            this.input.setResponder(value -> {
                this.error = null;
                this.playerScroll = 0;
                this.validate();
                this.rebuildPlayers();
            });
            this.setInitialFocus(this.input);
        }
        int buttonWidth = (this.formWidth - 36) / 2;
        this.addRenderableWidget(new TeamButton(this.left + 16, this.top + this.formHeight - 36, buttonWidth,
            TeamScreen.text("cancel"), TeamIcon.CLOSE, this::onClose));
        this.submit = this.addRenderableWidget(new TeamButton(this.left + 20 + buttonWidth,
            this.top + this.formHeight - 36, buttonWidth, TeamScreen.text(this.action.key), TeamIcon.CHECK, this::submit));
        if (this.action != Action.CREATE && this.action != Action.INVITE) {
            this.submit.danger();
        }
        this.validate();
        this.rebuildPlayers();
    }

    private void validate() {
        boolean valid = this.parent.canSubmit() && this.team.equals(this.parent.snapshot().name());
        if (this.action != Action.CREATE && this.action != Action.LEAVE) {
            valid &= this.parent.isOwner();
        }
        if (this.target != null) {
            valid &= this.parent.snapshot().members().stream().anyMatch(member -> member.id().equals(this.target.id()) && !member.owner());
        }
        if (this.input != null) {
            String value = this.input.getValue().strip();
            if (this.action == Action.CREATE) {
                try {
                    TeamManager.validateTeamName(value);
                } catch (TeamManager.TeamException exception) {
                    valid = false;
                    if (!value.isEmpty()) {
                        this.error = exception.toComponent();
                    }
                }
            } else {
                valid &= !value.isEmpty() && !value.startsWith("@") && StringUtil.isValidPlayerName(value);
            }
        }
        if (this.submit != null) {
            this.submit.active = valid && !this.pending;
        }
        if (this.input != null) {
            this.input.setEditable(!this.pending);
        }
        for (TeamButton button : this.playerButtons) {
            button.active = !this.pending;
        }
    }

    private void submit() {
        this.validate();
        if (this.submit == null || !this.submit.active) {
            return;
        }
        String argument = this.input == null ? this.target == null ? "" : this.target.id().toString() : this.input.getValue().strip();
        this.requestId = this.parent.execute(TeamAction.Kind.valueOf(this.action.name()), argument);
        this.pending = this.requestId != null;
        this.pendingTicks = 0;
        this.error = null;
        this.validate();
    }

    void reply(TeamReply reply) {
        this.parent.reply(reply);
        if (!reply.requestId().equals(this.requestId)) {
            return;
        }
        if (reply.success()) {
            this.onClose();
            return;
        }
        this.pending = false;
        this.error = reply.message();
        this.validate();
    }

    @Override
    public void tick() {
        this.parent.tick();
        if (this.pending && ++this.pendingTicks >= 100) {
            this.pending = false;
            this.error = TeamScreen.text("waiting");
        }
        this.validate();
        if (this.action == Action.INVITE && !this.playerNames.equals(this.candidates())) {
            this.rebuildPlayers();
        }
    }

    private List<String> candidates() {
        var connection = this.minecraft.getConnection();
        if (connection == null || this.input == null) {
            return List.of();
        }
        String query = this.input.getValue().toLowerCase(Locale.ROOT);
        return connection.getOnlinePlayers().stream()
            .filter(info -> this.parent.snapshot().members().stream().noneMatch(member -> member.id().equals(info.getProfile().id())))
            .map(info -> info.getProfile().name())
            .filter(name -> name.toLowerCase(Locale.ROOT).contains(query))
            .sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private void rebuildPlayers() {
        for (TeamButton button : this.playerButtons) {
            this.removeWidget(button);
        }
        this.playerButtons.clear();
        if (this.action != Action.INVITE) {
            return;
        }
        this.playerNames = this.candidates();
        int rows = Math.max(0, (this.formHeight - 156) / 26);
        this.playerScroll = Math.clamp(this.playerScroll, 0, Math.max(0, this.playerNames.size() - rows));
        for (int row = 0; row < rows && row + this.playerScroll < this.playerNames.size(); row++) {
            String name = this.playerNames.get(row + this.playerScroll);
            TeamButton button = new TeamButton(this.left + 16, this.top + 90 + row * 26, this.formWidth - 32,
                TeamScreen.text("member_name", name), TeamIcon.MEMBER, () -> {
                    if (this.input != null) {
                        this.input.setValue(name);
                        this.setFocused(this.input);
                    }
                });
            button.active = !this.pending;
            this.playerButtons.add(this.addRenderableWidget(button));
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xC0081018);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(this.left - 1, this.top - 1, this.left + this.formWidth + 1, this.top + this.formHeight + 1, 0xFF53656C);
        graphics.fill(this.left, this.top, this.left + this.formWidth, this.top + this.formHeight, 0xFF18252F);
        graphics.fill(this.left, this.top, this.left + this.formWidth, this.top + 2, 0xFF89C9B5);
        graphics.drawString(this.font, this.title, this.left + 16, this.top + 16, TeamScreen.TEXT, false);
        Component description = TeamScreen.text(this.action.key + "_description", this.team,
            this.target == null ? "" : this.target.name());
        graphics.drawWordWrap(this.font, description, this.left + 16, this.top + 35, this.formWidth - 32, TeamScreen.MUTED, false);
        Component feedback = this.pending ? TeamScreen.text("pending") : this.error;
        if (feedback != null) {
            graphics.enableScissor(this.left + 16, this.top + this.formHeight - 66, this.left + this.formWidth - 16, this.top + this.formHeight - 40);
            graphics.drawWordWrap(this.font, feedback, this.left + 16, this.top + this.formHeight - 64, this.formWidth - 32, 0xFFE2B5A8, false);
            graphics.disableScissor();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.action == Action.INVITE && scrollY != 0 && mouseX >= this.left && mouseX < this.left + this.formWidth
            && mouseY >= this.top + 90 && mouseY < this.top + this.formHeight - 66) {
            int rows = Math.max(0, (this.formHeight - 156) / 26);
            this.playerScroll = Math.clamp(this.playerScroll + (scrollY > 0 ? -1 : 1), 0, Math.max(0, this.candidates().size() - rows));
            this.rebuildPlayers();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.input != null && this.input.isFocused() && (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            this.submit();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
        TeamClient.refresh();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    enum Action {
        CREATE("create"), INVITE("invite"), KICK("kick"), TRANSFER("transfer"), LEAVE("leave"), DISBAND("disband");

        private final String key;

        Action(String key) {
            this.key = key;
        }
    }
}
