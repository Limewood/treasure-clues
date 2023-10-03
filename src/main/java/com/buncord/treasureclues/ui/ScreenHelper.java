package com.buncord.treasureclues.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TranslatableComponent;

public class ScreenHelper {
    public static void openClueScreen(TranslatableComponent clueText) {
        Minecraft.getInstance().setScreen(new ClueScreen(clueText));
    }
}
