package com.buncord.treasureclues.networking.packet;

import com.buncord.treasureclues.ui.ClueScreen;
import com.buncord.treasureclues.ui.ScreenHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenClueServerToClientPacket {
    private final TranslatableComponent clueText;

    public OpenClueServerToClientPacket(TranslatableComponent clueText) {
        this.clueText = clueText;
    }

    public OpenClueServerToClientPacket(FriendlyByteBuf buf) {
        this.clueText = (TranslatableComponent) buf.readComponent();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeComponent(clueText);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Here we are on the client
            // Open note GUI with clue text
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ScreenHelper.openClueScreen(clueText);
            });
        });

        context.setPacketHandled(true);
        return true;
    }
}
