package com.buncord.treasureclues.ui;

import com.buncord.treasureclues.TreasureCluesMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ClueScreen extends Screen {
    public static final int PAGE_TEXT_X_OFFSET = 36;
    public static final ResourceLocation CLUE_TEXTURE = new ResourceLocation(
            TreasureCluesMod.MOD_ID, "textures/gui/clue.png"
    );
    protected static final int TEXT_WIDTH = 114;
    protected static final int IMAGE_WIDTH = 192;
    protected static final int IMAGE_HEIGHT = 192;

    private final TranslatableComponent clueText;

    public ClueScreen(TranslatableComponent clueText) {
        super(NarratorChatListener.NO_TITLE);
        this.clueText = clueText;
    }

    protected void init() {
        this.createMenuControls();
    }

    protected void createMenuControls() {
        this.addRenderableWidget(new Button(this.width / 2 - 100, IMAGE_WIDTH, 200, 20, CommonComponents.GUI_DONE, (p_98299_) -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
        }));
    }

    public void render(@NotNull PoseStack poseStack, int p_98283_, int p_98284_, float p_98285_) {
        this.renderBackground(poseStack);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, CLUE_TEXTURE);
        int pageStart = (this.width - IMAGE_WIDTH) / 2;
        this.blit(poseStack, pageStart, 2, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        List<FormattedCharSequence> lines = font.split(
                clueText,
                TEXT_WIDTH
        );
        for (int line = 0; line < lines.size(); ++line) {
            FormattedCharSequence lineText = lines.get(line);
            this.font.draw(poseStack, lineText, (float) (pageStart + PAGE_TEXT_X_OFFSET), (float) (32 + line * 9), 0);
        }

        super.render(poseStack, p_98283_, p_98284_, p_98285_);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}