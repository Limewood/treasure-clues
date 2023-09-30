package com.buncord.treasureclues;

import com.buncord.treasureclues.item.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TreasureCluesMod.MOD_ID)
public class TreasureCluesMod {
	public static final String MOD_ID = "treasureclues";

	public TreasureCluesMod() {
		IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

		ModItems.register(eventBus);
	}
}
