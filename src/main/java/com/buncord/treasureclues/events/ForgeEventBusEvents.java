package com.buncord.treasureclues.events;

import com.buncord.treasureclues.TreasureCluesMod;
import com.buncord.treasureclues.capability.ClueReceived;
import com.buncord.treasureclues.capability.ClueReceivedProvider;
import com.buncord.treasureclues.item.ModItems;
import com.buncord.treasureclues.item.TreasureClueItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

@Mod.EventBusSubscriber(modid = TreasureCluesMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEventBusEvents {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MIN_WORLD_AGE_FOR_FREE_CLUE = 24000 * 7; // 7 days

    @SubscribeEvent
    public static void onEntityJoinWorld(@Nonnull final EntityJoinWorldEvent event) {
        // Give player a treasure clue once when they login (if the world is old enough)
        if (event.getEntity() instanceof Player player && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(ClueReceivedProvider.CLUE_RECEIVED).ifPresent(clueReceived -> {
                LOGGER.error("Time: " + event.getWorld().getGameTime());
                if (!clueReceived.isReceived() && event.getWorld().getGameTime() > MIN_WORLD_AGE_FOR_FREE_CLUE) {
                    serverPlayer.addItem(new ItemStack(ModItems.TREASURE_CLUE.get()));
                    clueReceived.setReceived(true);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        // Attach capability to player
        if (event.getObject() instanceof Player player) {
            if (!player.getCapability(ClueReceivedProvider.CLUE_RECEIVED).isPresent()) {
                event.addCapability(
                        new ResourceLocation(TreasureCluesMod.MOD_ID, "properties"),
                        new ClueReceivedProvider()
                );
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        // Copy capability to new player on respawn
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(ClueReceivedProvider.CLUE_RECEIVED).ifPresent(oldStore -> {
            event.getEntity().getCapability(ClueReceivedProvider.CLUE_RECEIVED).ifPresent(newStore -> {
                newStore.copyFrom(oldStore);
            });
        });
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(ClueReceived.class);
    }
}
