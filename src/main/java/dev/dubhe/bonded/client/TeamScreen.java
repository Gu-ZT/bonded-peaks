package dev.dubhe.bonded.client;

import dev.dubhe.bonded.network.TeamAction;
import dev.dubhe.bonded.network.TeamReply;
import dev.dubhe.bonded.network.TeamSnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

final class TeamScreen extends Screen {
    static final int TEXT = 0xFFEDF3F5;
    static final int MUTED = 0xFFA3B6C0;
    private TeamSnapshot snapshot = new TeamSnapshot("", List.of(), List.of());
    private Page page = Page.MEMBERS;
    private @Nullable UUID selected;
    private @Nullable UUID selectedInvite;
    private @Nullable EditBox chat;
    private @Nullable TeamButton send;
    private Component status = text("loading");
    private String draft = "";
    private int refreshTicks;
    private int waitingTicks;
    private int scroll;
    private int chatScroll;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private boolean received;
    private @Nullable UUID pendingRequest;
    private @Nullable String pendingChat;
    private int actionTicks;
    private final List<TeamButton> actionButtons = new ArrayList<>();

    TeamScreen() {
        super(text("title"));
    }

    static Component text(String key, Object... args) {
        return Component.translatable("screen.bonded_peaks." + key, args);
    }

    @Override
    protected void init() {
        this.actionButtons.clear();
        if (this.chat != null) {
            this.draft = this.chat.getValue();
        }
        this.chat = null;
        this.send = null;
        this.panelWidth = Math.min(680, this.width - 24);
        this.panelHeight = Math.min(420, this.height - 24);
        this.left = (this.width - this.panelWidth) / 2;
        this.top = (this.height - this.panelHeight) / 2;
        this.addRenderableWidget(new TeamButton(this.right() - 36, this.top + 12, 24,
            text("close"), TeamIcon.CLOSE, this::onClose));
        int tabWidth = (this.panelWidth - 32) / 3;
        for (Page value : Page.values()) {
            this.addRenderableWidget(new TeamButton(this.left + 12 + value.ordinal() * (tabWidth + 4),
                this.top + 54, tabWidth, text(value.key), value.icon, () -> this.switchPage(value))
                .selected(value == this.page));
        }
        if (!TeamClient.supported()) {
            this.status = text("unsupported");
            return;
        }
        if (!this.received) {
            TeamClient.refresh();
            return;
        }
        if (this.isOwner()) {
            this.addAction(new TeamButton(this.right() - 64, this.top + 12, 24,
                text("invite"), TeamIcon.INVITE, () -> this.form(TeamFormScreen.Action.INVITE, null)));
        }
        if (this.page == Page.INVITES) {
            this.inviteWidgets();
        } else if (this.snapshot.name().isEmpty()) {
            this.addAction(new TeamButton(this.left + (this.panelWidth - 140) / 2,
                this.contentTop() + 52, 140, text("create"), TeamIcon.INVITE,
                () -> this.form(TeamFormScreen.Action.CREATE, null)));
        } else if (this.page == Page.MEMBERS) {
            this.memberWidgets();
        } else {
            this.chatWidgets();
        }
        if (!this.snapshot.name().isEmpty() && this.page != Page.CHAT) {
            this.addAction(new TeamButton(this.right() - 124, this.bottom() - 32, 112,
                text(this.isOwner() ? "disband" : "leave"), TeamIcon.LEAVE,
                () -> this.form(this.isOwner() ? TeamFormScreen.Action.DISBAND : TeamFormScreen.Action.LEAVE, null)).danger());
        }
    }

    private void switchPage(Page value) {
        this.page = value;
        this.scroll = 0;
        this.rebuildWidgets();
    }

    private List<TeamSnapshot.Member> members() {
        return this.snapshot.members().stream().sorted(Comparator.comparing(TeamSnapshot.Member::owner).reversed()
            .thenComparing(TeamSnapshot.Member::online, Comparator.reverseOrder())
            .thenComparing(TeamSnapshot.Member::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private void memberWidgets() {
        List<TeamSnapshot.Member> members = this.members();
        this.scroll = Math.clamp(this.scroll, 0, Math.max(0, members.size() - this.visibleRows()));
        for (int row = 0; row < this.visibleRows() && row + this.scroll < members.size(); row++) {
            TeamSnapshot.Member member = members.get(row + this.scroll);
            this.addRenderableWidget(new TeamButton(this.left + 12, this.contentTop() + row * 28,
                this.listWidth(), text("member_name", member.name()), member.owner() ? TeamIcon.CROWN : TeamIcon.MEMBER,
                () -> {
                    this.selected = member.id();
                    this.rebuildWidgets();
                }).selected(member.id().equals(this.selected)));
        }
        int x = this.detailLeft();
        int width = this.right() - x - 12;
        if (this.isOwner()) {
            TeamSnapshot.Member member = this.selectedMember();
            if (member != null && !member.owner()) {
                int actionWidth = (width - 4) / 2;
                this.addAction(new TeamButton(x, this.contentTop() + 56, actionWidth,
                    text("transfer"), TeamIcon.CROWN, () -> this.form(TeamFormScreen.Action.TRANSFER, member)));
                this.addAction(new TeamButton(x + actionWidth + 4, this.contentTop() + 56, actionWidth,
                    text("kick"), TeamIcon.REMOVE, () -> this.form(TeamFormScreen.Action.KICK, member)).danger());
            }
        }
    }

    private void inviteWidgets() {
        List<TeamSnapshot.Invite> invites = this.snapshot.invites();
        this.scroll = Math.clamp(this.scroll, 0, Math.max(0, invites.size() - this.visibleRows()));
        for (int row = 0; row < this.visibleRows() && row + this.scroll < invites.size(); row++) {
            TeamSnapshot.Invite invite = invites.get(row + this.scroll);
            this.addRenderableWidget(new TeamButton(this.left + 12, this.contentTop() + row * 28,
                this.listWidth(), text("team_name", invite.team()), TeamIcon.INVITE, () -> {
                    this.selectedInvite = invite.inviterId();
                    this.rebuildWidgets();
                }).selected(invite.inviterId().equals(this.selectedInvite)));
        }
        TeamSnapshot.Invite invite = this.selectedInvitation();
        if (invite != null && this.snapshot.name().isEmpty()) {
            this.addAction(new TeamButton(this.detailLeft(), this.contentTop() + 56,
                this.right() - this.detailLeft() - 12, text("accept"), TeamIcon.CHECK,
                () -> this.execute(TeamAction.Kind.ACCEPT, invite.inviterId().toString())));
        }
    }

    private void chatWidgets() {
        int y = this.bottom() - 34;
        this.chat = this.addRenderableWidget(new EditBox(this.font, this.left + 16, y + 8,
            this.panelWidth - 112, 16, text("chat_hint")));
        this.chat.setMaxLength(239);
        this.chat.setBordered(false);
        this.chat.setTextColor(TEXT);
        this.chat.setHint(text("chat_hint"));
        this.chat.setValue(this.draft);
        this.send = this.addRenderableWidget(new TeamButton(this.right() - 82, y, 70,
            text("send"), TeamIcon.SEND, this::sendChat));
        this.send.active = this.canSubmit() && !this.draft.isBlank();
        this.chat.setResponder(value -> {
            if (this.send != null) {
                this.send.active = this.canSubmit() && !value.isBlank();
            }
        });
    }

    private void sendChat() {
        if (!this.canSubmit() || this.chat == null || this.chat.getValue().isBlank()) {
            return;
        }
        this.execute(TeamAction.Kind.CHAT, this.chat.getValue().strip());
        this.chatScroll = 0;
        this.setFocused(this.chat);
    }

    void receive(TeamSnapshot value) {
        boolean changed = !this.received || !this.snapshot.name().equals(value.name())
            || !this.snapshot.members().equals(value.members())
            || this.invitationsChanged(value.invites());
        if (!this.snapshot.name().equals(value.name())) {
            this.selected = null;
            this.draft = "";
            if (this.chat != null) {
                this.chat.setValue("");
            }
            this.chatScroll = 0;
            this.scroll = 0;
        }
        this.snapshot = value;
        if (!this.received) {
            this.status = text("ready");
        }
        this.received = true;
        this.waitingTicks = 0;
        if (this.selectedMember() == null) {
            this.selected = value.members().isEmpty() ? null : value.members().getFirst().id();
        }
        if (this.selectedInvitation() == null) {
            this.selectedInvite = value.invites().isEmpty() ? null : value.invites().getFirst().inviterId();
        }
        if (changed && this.minecraft.screen == this) {
            this.rebuildWidgets();
        }
    }

    private boolean invitationsChanged(List<TeamSnapshot.Invite> invites) {
        if (this.snapshot.invites().size() != invites.size()) {
            return true;
        }
        for (int index = 0; index < invites.size(); index++) {
            TeamSnapshot.Invite before = this.snapshot.invites().get(index);
            TeamSnapshot.Invite after = invites.get(index);
            if (!before.inviterId().equals(after.inviterId()) || !before.team().equals(after.team()) || !before.inviter().equals(after.inviter())) {
                return true;
            }
        }
        return false;
    }

    void chatReceived(Component message) {
        if (this.chatScroll > 0) {
            this.chatScroll += this.font.split(message, this.panelWidth - 40).size();
        }
    }

    void reply(TeamReply reply) {
        if (reply.requestId().equals(this.pendingRequest)) {
            if (reply.success() && this.pendingChat != null) {
                if (this.pendingChat.equals(this.draft)) {
                    this.draft = "";
                }
                if (this.chat != null && this.pendingChat.equals(this.chat.getValue())) {
                    this.chat.setValue("");
                }
            }
            this.status = reply.message();
            this.pendingRequest = null;
            this.pendingChat = null;
            this.updateActions();
        }
    }

    @Nullable UUID execute(TeamAction.Kind kind, String argument) {
        if (!this.canSubmit()) {
            return null;
        }
        UUID request = UUID.randomUUID();
        this.pendingRequest = request;
        this.pendingChat = kind == TeamAction.Kind.CHAT && this.chat != null ? this.chat.getValue() : null;
        this.actionTicks = 0;
        TeamSnapshot.Invite invitation = this.selectedInvitation();
        String expectedTeam = kind == TeamAction.Kind.ACCEPT && invitation != null ? invitation.team() : this.snapshot.name();
        ClientPacketDistributor.sendToServer(new TeamAction(request, kind, expectedTeam, argument));
        this.status = text("pending");
        this.refreshTicks = 19;
        this.updateActions();
        return request;
    }

    boolean canSubmit() {
        return this.received && this.pendingRequest == null && TeamClient.supported();
    }

    private void addAction(TeamButton button) {
        button.active = this.canSubmit();
        this.actionButtons.add(this.addRenderableWidget(button));
    }

    private void updateActions() {
        for (TeamButton button : this.actionButtons) {
            button.active = this.canSubmit();
        }
        if (this.send != null && this.chat != null) {
            this.send.active = this.canSubmit() && !this.chat.getValue().isBlank();
        }
    }

    TeamSnapshot snapshot() {
        return this.snapshot;
    }

    boolean isOwner() {
        return this.minecraft.player != null && this.snapshot.members().stream()
            .anyMatch(member -> member.owner() && member.id().equals(this.minecraft.player.getUUID()));
    }

    private TeamSnapshot.@Nullable Member selectedMember() {
        return this.snapshot.members().stream().filter(member -> member.id().equals(this.selected)).findFirst().orElse(null);
    }

    private TeamSnapshot.@Nullable Invite selectedInvitation() {
        return this.snapshot.invites().stream().filter(invite -> invite.inviterId().equals(this.selectedInvite)).findFirst().orElse(null);
    }

    private void form(TeamFormScreen.Action action, TeamSnapshot.@Nullable Member member) {
        if (this.canSubmit()) {
            this.minecraft.setScreen(new TeamFormScreen(this, action, member));
        }
    }

    @Override
    public void tick() {
        if (this.pendingRequest != null && ++this.actionTicks >= 100) {
            this.pendingRequest = null;
            this.pendingChat = null;
            this.status = text("waiting");
            this.updateActions();
        }
        if (++this.refreshTicks >= 20) {
            this.refreshTicks = 0;
            TeamClient.refresh();
        }
        if (++this.waitingTicks == 100 && TeamClient.supported()) {
            this.status = text("waiting");
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xB0081018);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(this.left - 1, this.top - 1, this.right() + 1, this.bottom() + 1, 0xFF53656C);
        graphics.fill(this.left, this.top, this.right(), this.bottom(), 0xF018252F);
        graphics.fill(this.left, this.top, this.right(), this.top + 46, 0xFF243843);
        graphics.fill(this.left, this.top, this.right(), this.top + 2, 0xFF89C9B5);
        TeamIcon.PEAKS.draw(graphics, this.left + 14, this.top + 9, 30);
        this.label(graphics, this.snapshot.name().isEmpty() ? this.title : text("team_name", this.snapshot.name()),
            this.left + 52, this.top + 11, this.panelWidth - 124, TEXT);
        this.label(graphics, this.snapshot.name().isEmpty() ? text("tagline")
            : text("summary", this.snapshot.members().stream().filter(TeamSnapshot.Member::online).count(), this.snapshot.members().size()),
            this.left + 52, this.top + 28, this.panelWidth - 124, MUTED);
        graphics.fill(this.left + 8, this.contentTop() - 4, this.right() - 8, this.bottom() - 40, 0xFF131F28);
        if (!TeamClient.supported() || !this.received) {
            this.empty(graphics, TeamClient.supported() ? "loading" : "unsupported");
        } else if (this.page == Page.INVITES) {
            this.drawInvites(graphics);
        } else if (this.snapshot.name().isEmpty()) {
            graphics.drawCenteredString(this.font, text("no_team"), this.width / 2, this.contentTop() + 10, TEXT);
            graphics.drawCenteredString(this.font, text("create_hint"), this.width / 2, this.contentTop() + 30, MUTED);
        } else if (this.page == Page.MEMBERS) {
            this.drawMember(graphics);
        } else {
            this.drawChat(graphics);
        }
        if (this.page != Page.CHAT || this.snapshot.name().isEmpty()) {
            int statusWidth = this.snapshot.name().isEmpty() ? this.panelWidth - 24 : this.panelWidth - 150;
            this.label(graphics, this.status, this.left + 12, this.bottom() - 24, statusWidth, MUTED);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawMember(GuiGraphics graphics) {
        TeamSnapshot.Member member = this.selectedMember();
        if (member == null) {
            return;
        }
        int x = this.detailLeft();
        int width = this.right() - x - 12;
        this.label(graphics, text("member_name", member.name()), x, this.contentTop() + 5, width, TEXT);
        this.label(graphics, text(member.owner() ? "owner" : "member"), x, this.contentTop() + 23, width, MUTED);
        this.label(graphics, text(member.online() ? "online" : "offline"), x, this.contentTop() + 41, width,
            member.online() ? 0xFFA1D8BA : MUTED);
        this.scrollbar(graphics, this.snapshot.members().size());
    }

    private void drawInvites(GuiGraphics graphics) {
        TeamSnapshot.Invite invite = this.selectedInvitation();
        if (invite == null) {
            this.empty(graphics, "no_invites");
            return;
        }
        int x = this.detailLeft();
        int width = this.right() - x - 12;
        this.label(graphics, text("team_name", invite.team()), x, this.contentTop() + 5, width, TEXT);
        this.label(graphics, text("invited_by", invite.inviter()), x, this.contentTop() + 24, width, MUTED);
        this.label(graphics, text("expires", invite.remainingSeconds()), x, this.contentTop() + 44, width, MUTED);
        this.scrollbar(graphics, this.snapshot.invites().size());
    }

    private List<FormattedCharSequence> chatLines() {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (Component message : TeamClient.messages()) {
            lines.addAll(this.font.split(message, this.panelWidth - 40));
        }
        return lines;
    }

    private void drawChat(GuiGraphics graphics) {
        List<FormattedCharSequence> lines = this.chatLines();
        if (lines.isEmpty()) {
            this.empty(graphics, "chat_empty");
        }
        int visible = Math.max(1, (this.bottom() - 64 - this.contentTop()) / 12);
        this.chatScroll = Math.clamp(this.chatScroll, 0, Math.max(0, lines.size() - visible));
        int start = Math.max(0, lines.size() - visible - this.chatScroll);
        for (int i = 0; i < visible && start + i < lines.size(); i++) {
            graphics.drawString(this.font, lines.get(start + i), this.left + 18, this.contentTop() + 4 + i * 12, TEXT, false);
        }
        this.label(graphics, this.status, this.left + 16, this.bottom() - 52, this.panelWidth - 32, MUTED);
        graphics.fill(this.left + 12, this.bottom() - 34, this.right() - 90, this.bottom() - 10, 0xFF0C171F);
        graphics.fill(this.left + 12, this.bottom() - 11, this.right() - 90, this.bottom() - 10, 0xFF719D9D);
    }

    private void empty(GuiGraphics graphics, String key) {
        graphics.drawWordWrap(this.font, text(key), this.left + 24, this.contentTop() + 22, this.panelWidth - 48, MUTED, false);
    }

    private void label(GuiGraphics graphics, Component value, int x, int y, int available, int color) {
        graphics.enableScissor(x, y, x + Math.max(0, available), y + this.font.lineHeight);
        graphics.drawString(this.font, value, x, y, color, false);
        graphics.disableScissor();
    }

    private void scrollbar(GuiGraphics graphics, int count) {
        if (count <= this.visibleRows()) {
            return;
        }
        int height = this.visibleRows() * 28;
        int thumb = Math.max(12, height * this.visibleRows() / count);
        int y = this.contentTop() + (height - thumb) * this.scroll / (count - this.visibleRows());
        graphics.fill(this.left + this.listWidth() + 16, y, this.left + this.listWidth() + 18, y + thumb, 0xFF89C9B5);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX < this.left + 8 || mouseX >= this.right() - 8 || mouseY < this.contentTop() || mouseY >= this.bottom() - 40) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (this.page == Page.CHAT && !this.snapshot.name().isEmpty()) {
            this.chatScroll = Math.max(0, this.chatScroll + (scrollY > 0 ? 3 : -3));
        } else if (scrollY != 0) {
            int count = this.page == Page.MEMBERS ? this.snapshot.members().size() : this.snapshot.invites().size();
            int offset = Math.clamp(this.scroll + (scrollY > 0 ? -1 : 1), 0, Math.max(0, count - this.visibleRows()));
            if (offset != this.scroll) {
                this.scroll = offset;
                this.rebuildWidgets();
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if ((event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)
            && this.chat != null && this.chat.isFocused()) {
            this.sendChat();
            return true;
        }
        return super.keyPressed(event);
    }

    private int right() {
        return this.left + this.panelWidth;
    }

    private int bottom() {
        return this.top + this.panelHeight;
    }

    private int contentTop() {
        return this.top + 90;
    }

    private int listWidth() {
        return (this.panelWidth - 40) / 2;
    }

    private int detailLeft() {
        return this.left + this.listWidth() + 28;
    }

    private int visibleRows() {
        return Math.max(1, (this.panelHeight - 136) / 28);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum Page {
        MEMBERS("members", TeamIcon.MEMBER), CHAT("chat", TeamIcon.CHAT), INVITES("invitations", TeamIcon.INVITE);

        private final String key;
        private final TeamIcon icon;

        Page(String key, TeamIcon icon) {
            this.key = key;
            this.icon = icon;
        }
    }
}
