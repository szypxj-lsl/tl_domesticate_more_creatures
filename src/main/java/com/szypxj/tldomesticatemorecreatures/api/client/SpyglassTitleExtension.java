package com.szypxj.tldomesticatemorecreatures.api.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public interface SpyglassTitleExtension {
    int width(Font font, SpyglassTitleContext context);
    void render(GuiGraphics graphics, Font font, SpyglassTitleContext context, int x, int y);
}
