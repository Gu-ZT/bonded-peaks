package dev.dubhe.bonded.client;

import net.minecraft.client.gui.GuiGraphics;

enum TeamIcon {
    PEAKS, MEMBER, CROWN, CHAT, INVITE, SEND, LEAVE, CLOSE, CHECK, REMOVE;

    void draw(GuiGraphics graphics, int x, int y, int size) {
        var pose = graphics.pose().last().pose();
        graphics.drawSpecial(buffers -> {
            var vertices = buffers.getBuffer(TeamIconShader.ICON);
            int color = 0xFF000000 | this.ordinal() << 16;
            vertices.addVertex(pose, x, y, 0).setUv(0, 0).setColor(color);
            vertices.addVertex(pose, x, y + size, 0).setUv(0, 1).setColor(color);
            vertices.addVertex(pose, x + size, y + size, 0).setUv(1, 1).setColor(color);
            vertices.addVertex(pose, x + size, y, 0).setUv(1, 0).setColor(color);
        });
    }
}
