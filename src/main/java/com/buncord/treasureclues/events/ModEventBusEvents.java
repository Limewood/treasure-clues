package com.buncord.treasureclues.events;

import com.buncord.treasureclues.TreasureCluesMod;
import com.buncord.treasureclues.events.loot.TreasureCluesLootAdditionModifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

@Mod.EventBusSubscriber(modid = TreasureCluesMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {
    private static final Logger LOGGER = LogManager.getLogger();
    @SubscribeEvent
    public static void registerModifierSerializers(
            @Nonnull final RegistryEvent.Register<GlobalLootModifierSerializer<?>> event
    ) {
        event.getRegistry().registerAll(
                new TreasureCluesLootAdditionModifier.Serializer().setRegistryName
                        (new ResourceLocation(TreasureCluesMod.MOD_ID,"treasure_clues_locations"))
        );
    }
}
