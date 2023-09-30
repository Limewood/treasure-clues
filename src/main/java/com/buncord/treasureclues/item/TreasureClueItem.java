package com.buncord.treasureclues.item;

import com.buncord.treasureclues.TreasureCluesMod;
import com.buncord.treasureclues.WorldHeightHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.SetBlockCommand;
import net.minecraft.server.level.PlayerRespawnLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

public class TreasureClueItem extends Item {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final String USED = "Used";
    public static final String CLUE_STEP = "ClueStep";
    private static final ResourceLocation LOOT_RESOURCE_LOCATION = new ResourceLocation(TreasureCluesMod.MOD_ID, "treasure/treasure");

    public TreasureClueItem(Properties properties) {
        super(properties);
    }

    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level,
            @NotNull Player player,
            @NotNull InteractionHand interactionHand
    ) {
        ItemStack itemStack = player.getItemInHand(interactionHand);

        if (!level.isClientSide) {
            CompoundTag tag = itemStack.getOrCreateTag();
            if (tag.getBoolean(USED)) {
                return InteractionResultHolder.pass(itemStack);
            }
            tag.putBoolean(USED, true);
            final int step = tag.getInt(CLUE_STEP);
            LOGGER.error("Used item with step " + step);

            ServerLevel serverLevel = (ServerLevel) level;
            // Find nearest structure feature
            // Get random structure type
            int structureIndex = level.random.nextInt(StructureFeatures.STRUCTURE_FEATURES.length);
            LOGGER.error("Index: " + structureIndex + " array length: " + StructureFeatures.STRUCTURE_FEATURES.length);
            String structureFeatureId = StructureFeatures.STRUCTURE_FEATURES[structureIndex].toLowerCase(Locale.ROOT);
            LOGGER.error("Structure feature id: " + structureFeatureId);
            StructureFeature<?> structureFeature = StructureFeature.STRUCTURES_REGISTRY.get(structureFeatureId);
            LOGGER.error("Structure feature: " + structureFeature.getFeatureName());
            BlockPos featureBlockPos = serverLevel.findNearestMapFeature(structureFeature, player.blockPosition(), 100, false);
            LOGGER.error("Nearest feature block pos: " + featureBlockPos);
            if (featureBlockPos != null) {
                BlockPos safePos = WorldHeightHelper.getOverworldSafePositionAt(
                        serverLevel, featureBlockPos.getX(), featureBlockPos.getZ(), isUnderWaterFeature(structureFeature)
                );
                LOGGER.error("Safe pos: " + safePos);
                if (safePos != null) {
                    // Place chest at position
                    serverLevel.setBlockAndUpdate(safePos, Blocks.CHEST.defaultBlockState());
                    ChestBlockEntity entity = (ChestBlockEntity) serverLevel.getBlockEntity(safePos);
                    LOGGER.error("Entity: " + entity);
                    if (entity != null) {
                        if (step > 0) {
                            entity.setLootTable(LOOT_RESOURCE_LOCATION, serverLevel.random.nextLong());
                        } else {
                            LazyOptional<IItemHandler> itemHandler = entity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
                            itemHandler.ifPresent(
                                    h -> {
                                        TreasureClueItem item = (TreasureClueItem) ModItems.TREASURE_CLUE.get();
                                        ItemStack clueItemStack = new ItemStack(item);
                                        clueItemStack.getOrCreateTag().putInt(CLUE_STEP, step + 1);
                                        h.insertItem(
                                                13,
                                                clueItemStack,
                                                false
                                        );
                                    }
                            );
                        }
                    }

                    // Teleport command for creative mode
                    BlockPos above = safePos.above();
                    String cmd = String.format("/tp %d %d %d",
                            above.getX(),
                            above.getY(),
                            above.getZ()
                    );
                    LOGGER.error("Command " + cmd);
                    player.sendMessage(new TextComponent(
                            String.format("Found %s at %d %d %d", structureFeatureId, safePos.getX(), safePos.getY(), safePos.getZ())
                    ).setStyle(Style.EMPTY.withClickEvent(
                            new ClickEvent(
                                    ClickEvent.Action.RUN_COMMAND,
                                    cmd
                            )
                    )), player.getUUID());
                }
            }

            // TODO Save previous feature or position to not pick same twice
            // TODO Evaluate chance of being last chest
            // TODO Show note text in window

        }

        return InteractionResultHolder.pass(itemStack);
    }

    public void setStep(ItemStack itemStack, int step) {
        CompoundTag compoundTag = itemStack.getOrCreateTag();
        compoundTag.putInt(CLUE_STEP, step);
    }

    private boolean isUnderWaterFeature(StructureFeature<?> structureFeature) {
        return structureFeature.getFeatureName().equals(StructureFeatures.OCEAN_RUIN.toLowerCase(Locale.ROOT))
                || structureFeature.getFeatureName().equals(StructureFeatures.SHIPWRECK.toLowerCase(Locale.ROOT))
                || structureFeature.getFeatureName().equals(StructureFeatures.RUINED_PORTAL.toLowerCase(Locale.ROOT));
    }
}
