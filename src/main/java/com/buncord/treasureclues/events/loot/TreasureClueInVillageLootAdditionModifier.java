package com.buncord.treasureclues.events.loot;

import com.buncord.treasureclues.item.StructureFeatures;
import com.buncord.treasureclues.item.TreasureClueItem;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.List;

public class TreasureClueInVillageLootAdditionModifier extends LootModifier {
    private final Item addition;

    protected TreasureClueInVillageLootAdditionModifier(LootItemCondition[] conditionsIn, Item addition) {
        super(conditionsIn);
        this.addition = addition;
    }

    @Nonnull
    @Override
    protected List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() > 0.9f) {
            ItemStack item = new ItemStack(addition, 1);
            // Remove village from possible features used for this clue
            item.getOrCreateTag().putString(TreasureClueItem.USED_FEATURES, StructureFeatures.VILLAGE);
            generatedLoot.add(item);
        }
        return generatedLoot;
    }

    public static class Serializer extends GlobalLootModifierSerializer<TreasureClueInVillageLootAdditionModifier> {

        @Override
        public TreasureClueInVillageLootAdditionModifier read(ResourceLocation name, JsonObject object,
                                                              LootItemCondition[] conditionsIn) {
            Item addition = ForgeRegistries.ITEMS.getValue(
                    new ResourceLocation(GsonHelper.getAsString(object, "addition")));
            return new TreasureClueInVillageLootAdditionModifier(conditionsIn, addition);
        }

        @Override
        public JsonObject write(TreasureClueInVillageLootAdditionModifier instance) {
            JsonObject json = makeConditions(instance.conditions);
            json.addProperty("addition", ForgeRegistries.ITEMS.getKey(instance.addition).toString());
            return json;
        }
    }
}
