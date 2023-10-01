package com.buncord.treasureclues;

import com.buncord.treasureclues.item.ModItems;
import com.buncord.treasureclues.networking.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TreasureCluesMod.MOD_ID)
public class TreasureCluesMod {
	public static final String MOD_ID = "treasureclues";

	public TreasureCluesMod() {
		IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

		ModItems.register(eventBus);

		eventBus.addListener(this::commonSetup);

		MinecraftForge.EVENT_BUS.register(this);
	}

	private void commonSetup(final FMLCommonSetupEvent event) {
		event.enqueueWork(ModNetwork::register);
	}
}
