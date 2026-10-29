package dev.dubhe.bonded.client;

import dev.anvilcraft.lib.v2.rendering.sdf.SdfGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;

enum TeamIcon {
    PEAKS, MEMBER, CROWN, CHAT, INVITE, SEND, LEAVE, CLOSE, CHECK, REMOVE;

    void draw(GuiGraphicsExtractor graphics, int x, int y, int size) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y).scale(size / 24f);
        for (int pass = 0; pass < 2; pass++) {
            float edge = pass == 0 ? 1.25f : 0;
            int color = pass == 0 ? 0xFF000000 : 0xFFFFFFFF;
            switch (this) {
                case PEAKS -> {
                    triangle(graphics, 8, 14, 6, edge, color);
                    triangle(graphics, 16, 11, 8, edge, color);
                }
                case MEMBER -> {
                    circle(graphics, 12, 7, 3.5f, edge, color);
                    box(graphics, 12, 17, 13, 7, 3, edge, color);
                }
                case CROWN -> {
                    line(graphics, 5, 17, 3, 7, edge + 1.5f, color);
                    line(graphics, 3, 7, 8, 11, edge + 1.5f, color);
                    line(graphics, 8, 11, 12, 4, edge + 1.5f, color);
                    line(graphics, 12, 4, 16, 11, edge + 1.5f, color);
                    line(graphics, 16, 11, 21, 7, edge + 1.5f, color);
                    line(graphics, 21, 7, 19, 17, edge + 1.5f, color);
                    box(graphics, 12, 18, 15, 4, 1, edge, color);
                }
                case CHAT -> {
                    box(graphics, 12, 10, 17, 12, 3, edge, color);
                    line(graphics, 7, 14, 5, 19, edge + 1.5f, color);
                }
                case INVITE -> {
                    line(graphics, 12, 5, 12, 19, edge + 1.5f, color);
                    line(graphics, 5, 12, 19, 12, edge + 1.5f, color);
                }
                case SEND, LEAVE -> {
                    line(graphics, 5, 12, 19, 12, edge + 1.5f, color);
                    line(graphics, 14, 6, 20, 12, edge + 1.5f, color);
                    line(graphics, 20, 12, 14, 18, edge + 1.5f, color);
                    if (this == LEAVE) {
                        line(graphics, 5, 4, 5, 20, edge + 1.5f, color);
                    }
                }
                case CLOSE -> {
                    line(graphics, 6, 6, 18, 18, edge + 1.5f, color);
                    line(graphics, 18, 6, 6, 18, edge + 1.5f, color);
                }
                case CHECK -> {
                    line(graphics, 5, 12, 10, 17, edge + 1.5f, color);
                    line(graphics, 10, 17, 20, 6, edge + 1.5f, color);
                }
                case REMOVE -> line(graphics, 5, 12, 19, 12, edge + 1.5f, color);
            }
        }
        graphics.pose().popMatrix();
    }

    private static void circle(GuiGraphicsExtractor graphics, float x, float y, float radius, float edge, int color) {
        SdfGraphics.getInstance().reset().circle(x, y, radius + edge).center(true).color(color).smooth(0.5f).draw(graphics);
    }

    private static void box(GuiGraphicsExtractor graphics, float x, float y, float width, float height,
                            float radius, float edge, int color) {
        SdfGraphics.getInstance().reset().box(x, y, width + edge * 2, height + edge * 2)
            .center(true).round(radius + edge).color(color).smooth(0.5f).draw(graphics);
    }

    private static void triangle(GuiGraphicsExtractor graphics, float x, float y, float radius, float edge, int color) {
        SdfGraphics.getInstance().reset().triangleEquilateral(x, y, radius + edge * 2.4f)
            .center(true).color(color).smooth(0.5f).draw(graphics);
    }

    private static void line(GuiGraphicsExtractor graphics, float x0, float y0, float x1, float y1, float radius, int color) {
        SdfGraphics.getInstance().reset().segment(x0, y0, x1, y1)
            .center(true).round(radius).color(color).smooth(0.5f).draw(graphics);
    }
}
