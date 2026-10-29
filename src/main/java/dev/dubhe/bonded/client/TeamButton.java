package dev.dubhe.bonded.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

final class TeamButton extends Button {
    private final @Nullable TeamIcon icon;
    private boolean selected;
    private boolean danger;

    TeamButton(int x, int y, int width, Component label, @Nullable TeamIcon icon, Runnable action) {
        super(x, y, width, 24, label, button -> action.run(), DEFAULT_NARRATION);
        this.icon = icon;
        this.setTooltip(Tooltip.create(label));
    }

    TeamButton selected(boolean value) {
        this.selected = value;
        return this;
    }

    TeamButton danger() {
        this.danger = true;
        return this;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int border = this.isHoveredOrFocused() ? 0xFFB2D7D5 : 0xFF354A55;
        int fill = this.selected ? 0xFF305954 : this.danger ? 0xFF512F39 : 0xFF22333F;
        if (!this.active) {
            fill = 0xFF202931;
        } else if (this.isHoveredOrFocused()) {
            fill = this.danger ? 0xFF71434A : 0xFF3B565F;
        }
        graphics.fill(this.getX(), this.getY(), this.getRight(), this.getBottom(), border);
        graphics.fill(this.getX() + 1, this.getY() + 1, this.getRight() - 1, this.getBottom() - 1, fill);
        if (this.selected) {
            graphics.fill(this.getX() + 1, this.getBottom() - 2, this.getRight() - 1, this.getBottom() - 1, 0xFF89C9B5);
        }
        int textX = this.getX() + 8;
        if (this.icon != null) {
            this.icon.draw(graphics, this.getX() + 5, this.getY() + 4, 16);
            textX += 19;
        }
        if (this.width > 28) {
            var font = Minecraft.getInstance().font;
            graphics.enableScissor(textX, this.getY(), this.getRight() - 5, this.getBottom());
            graphics.text(font, this.getMessage(), textX, this.getY() + 8,
                this.active ? 0xFFF1F4F5 : 0xFF819099, false);
            graphics.disableScissor();
        }
    }
}
