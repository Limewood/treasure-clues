package com.buncord.treasureclues.item;

import com.buncord.treasureclues.TreasureCluesMod;
import com.buncord.treasureclues.WorldHeightHelper;
import com.buncord.treasureclues.networking.ModNetwork;
import com.buncord.treasureclues.networking.packet.OpenClueServerToClientPacket;
import com.buncord.treasureclues.ui.ClueScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TreasureClueItem extends Item {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final String USED = "Used";
    public static final String CLUE_STEP = "ClueStep";
    public static final String FEATURE = "Feature";
    public static final String NORTH_SOUTH_DIRECTION = "NorthSouthDirection";
    public static final String WEST_EAST_DIRECTION = "WestEastDirection";
    public static final String NORTH_SOUTH_DISTANCE = "NorthSouthDistance";
    public static final String WEST_EAST_DISTANCE = "WestEastDistance";
    public static final String USED_FEATURES = "UsedFeatures";
    private static final String USED_FEATURES_DELIMITER = ";";
    private static final String CLUE_TEXT_TRANSLATION_ID_PREFIX = TreasureCluesMod.MOD_ID + ".clue.text.";
    private static final String DIRECTION_TRANSLATION_ID_PREFIX = TreasureCluesMod.MOD_ID + ".clue.direction.";
    private static final ResourceLocation LOOT_RESOURCE_LOCATION = new ResourceLocation(TreasureCluesMod.MOD_ID, "treasure/treasure");
    private static final String INVALID_DIMENSION_TRANSLATION_ID = TreasureCluesMod.MOD_ID + ".invalid_dimension";

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
            // We are on the server
            ServerLevel serverLevel = (ServerLevel) level;
            ServerPlayer serverPlayer = (ServerPlayer) player;
            BlockPos playerPos = serverPlayer.getOnPos();

            CompoundTag tag = itemStack.getOrCreateTag();
            if (tag.getBoolean(USED)) {
                // The clue has already been generated once
                return InteractionResultHolder.pass(itemStack);
            }
            if (level.dimension() != Level.OVERWORLD) {
                // Don't generate clue in the Nether or the End (for now)
                return InteractionResultHolder.pass(itemStack);
            }
            tag.putBoolean(USED, true);
            final int step = tag.getInt(CLUE_STEP);
            LOGGER.error("Used item with step " + step);
            // Find nearest structure feature
            // Remove already used features
            String usedFeaturesStr = tag.getString(USED_FEATURES);
            final String[] usedFeatures = !usedFeaturesStr.isEmpty()
                    ? usedFeaturesStr.split(USED_FEATURES_DELIMITER)
                    : new String[0];
            LOGGER.error("Used features: " + Arrays.toString(usedFeatures));
            List<String> availableFeatures = new ArrayList<>(Arrays.stream(StructureFeatures.STRUCTURE_FEATURES).toList());
            for (String uf : usedFeatures) {
                availableFeatures.remove(uf);
                LOGGER.error("Removed used feature: " + uf);
            }
            LOGGER.error("Available features: " + availableFeatures);

            // Get random structure type
            StructureFeatureData structureFeatureData = findNextCluePos(availableFeatures, serverLevel, serverPlayer);
            BlockPos featureBlockPos = structureFeatureData.position;
            boolean isUnderWaterFeature;
            boolean isFallbackChest = false;
            if (featureBlockPos == null) {
                // We've checked all features and found none nearby
                // Create final chest 10 blocks to the north
                featureBlockPos = playerPos.north(10);
                isUnderWaterFeature = true;
                isFallbackChest = true;
            } else {
                isUnderWaterFeature = isUnderWaterFeature(structureFeatureData.feature);
            }
            int zPos = featureBlockPos.getZ();
            BlockPos safePos = WorldHeightHelper.getOverworldSafePositionAt(
                    serverLevel, featureBlockPos.getX(), zPos, isUnderWaterFeature
            );
            while (safePos == null) {
                safePos = WorldHeightHelper.getOverworldSafePositionAt(
                        serverLevel, featureBlockPos.getX(), zPos--, isUnderWaterFeature
                );
            }
            LOGGER.error("Safe pos: " + safePos);
            // Check if there is already a chest here
            ChestBlockEntity chest = (ChestBlockEntity) serverLevel.getBlockEntity(safePos);
            if (chest != null) {
                do {
                    // Move it north one
                    safePos = WorldHeightHelper.getOverworldSafePositionAt(
                            serverLevel, featureBlockPos.getX(), zPos--, isUnderWaterFeature
                    );
                    while (safePos == null) {
                        safePos = WorldHeightHelper.getOverworldSafePositionAt(
                                serverLevel, featureBlockPos.getX(), zPos--, isUnderWaterFeature
                        );
                    }
                    chest = (ChestBlockEntity) serverLevel.getBlockEntity(safePos);
                } while (chest != null);
            }
            // Check if player is at this position
            if (safePos.equals(playerPos)) {
                // Move it north one
                safePos = safePos.north();
            }

            // Place chest at position
            serverLevel.setBlockAndUpdate(safePos, Blocks.CHEST.defaultBlockState());
            ChestBlockEntity entity = (ChestBlockEntity) serverLevel.getBlockEntity(safePos);
            LOGGER.error("Entity: " + entity);
            if (entity != null) {
                // Find approximate direction
                final DirectionsAndDistances dirDis = findDirectionsAndDistances(playerPos, safePos);
                LOGGER.error("Direction: " + dirDis.westEastDirection + " : " + dirDis.northSouthDirection);
                LOGGER.error("Distance: " + dirDis.westEastDistance + " : " + dirDis.northSouthDistance);

                String structureFeatureId;
                if (!isFallbackChest) {
                    structureFeatureId = structureFeatureData.feature.getFeatureName();
                    // Save feature
                    setFeature(tag, structureFeatureId);
                    // Save directions and distances
                    setDirectionsAndDistances(tag, dirDis);
                } else {
                    structureFeatureId = null;
                }

                // Random chance, unless only 3 features left
                if (isFallbackChest || availableFeatures.size() < 4) { // TODO Or random chance
                    entity.setLootTable(LOOT_RESOURCE_LOCATION, serverLevel.random.nextLong());
                } else {
                    LazyOptional<IItemHandler> itemHandler = entity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
                    itemHandler.ifPresent(
                            h -> {
                                TreasureClueItem item = (TreasureClueItem) ModItems.TREASURE_CLUE.get();
                                ItemStack clueItemStack = new ItemStack(item);
                                // Save step
                                setStep(clueItemStack, step + 1);
                                // Save used features
                                setUsedFeaturesString(clueItemStack, usedFeatures, structureFeatureId);
                                h.insertItem(
                                        13,
                                        clueItemStack,
                                        false
                                );
                            }
                    );
                }
                // Clue text
                TranslatableComponent clueText = isFallbackChest ? generateFallbackText() : generateClueTextFor(
                        structureFeatureId,
                        dirDis
                );
                // Tell client to show note GUI
                ModNetwork.sendToPlayer(new OpenClueServerToClientPacket(clueText), serverPlayer);

                if (!isFallbackChest && serverPlayer.gameMode.isCreative()) {
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
                    ).setStyle(
                            Style.EMPTY
                                    .withUnderlined(true)
                                    .withColor(Color.MAGENTA.getRGB())
                                    .withClickEvent(
                                            new ClickEvent(
                                                    ClickEvent.Action.RUN_COMMAND,
                                                    cmd
                                            )
                                    )
                    ), player.getUUID());
                }
            } else {
                // There is no chest where we placed it?!
                LOGGER.error("No chest entity found at chest location");
            }
        } else {
            // We are on the client
            CompoundTag tag = itemStack.getOrCreateTag();
            if (tag.getBoolean(USED)) {
                TranslatableComponent clueText = getClueText(tag);
                Minecraft.getInstance().setScreen(new ClueScreen(clueText));
            }
            if (level.dimension() != Level.OVERWORLD) {
                player.sendMessage(new TranslatableComponent(
                        INVALID_DIMENSION_TRANSLATION_ID
                ), player.getUUID());
            }
        }

        return InteractionResultHolder.pass(itemStack);
    }

    private StructureFeatureData findNextCluePos(List<String> availableFeatures, ServerLevel level, ServerPlayer player) {
        int structureIndex = level.random.nextInt(availableFeatures.size());
        LOGGER.error("Index: " + structureIndex + " array length: " + availableFeatures.size());
        final String structureFeatureId = availableFeatures.get(structureIndex);
        LOGGER.error("Structure feature id: " + structureFeatureId);
        StructureFeature<?> structureFeature = StructureFeature.STRUCTURES_REGISTRY.get(structureFeatureId);
        LOGGER.error("Structure feature: " + structureFeature.getFeatureName());
        BlockPos featureBlockPos = level.findNearestMapFeature(structureFeature, player.blockPosition(), 100, false);
        LOGGER.error("Nearest feature block pos: " + featureBlockPos);
        StructureFeatureData data;
        if (featureBlockPos == null && availableFeatures.size() > 1) {
            availableFeatures.remove(structureFeatureId);
            data = findNextCluePos(availableFeatures, level, player);
        } else if (featureBlockPos != null) {
            data = new StructureFeatureData(structureFeature, featureBlockPos);
        } else {
            data = null;
        }
        return data;
    }

    private void setStep(ItemStack itemStack, int step) {
        CompoundTag compoundTag = itemStack.getOrCreateTag();
        compoundTag.putInt(CLUE_STEP, step);
    }

    private void setFeature(CompoundTag compoundTag, String structureFeatureId) {
        compoundTag.putString(FEATURE, structureFeatureId);
    }

    private void setDirectionsAndDistances(CompoundTag compoundTag, DirectionsAndDistances directionsAndDistances) {
        compoundTag.putString(NORTH_SOUTH_DIRECTION, directionsAndDistances.northSouthDirection.getName());
        compoundTag.putString(WEST_EAST_DIRECTION, directionsAndDistances.westEastDirection.getName());
        compoundTag.putInt(NORTH_SOUTH_DISTANCE, directionsAndDistances.northSouthDistance);
        compoundTag.putInt(WEST_EAST_DISTANCE, directionsAndDistances.westEastDistance);
    }

    private void setUsedFeaturesString(ItemStack itemStack, String[] usedFeatures, String newUsedFeature) {
        CompoundTag compoundTag = itemStack.getOrCreateTag();
        StringBuilder builder = new StringBuilder();
        for (String feature : usedFeatures) {
            builder.append(feature).append(USED_FEATURES_DELIMITER);
        }
        builder.append(newUsedFeature);
        compoundTag.putString(USED_FEATURES, builder.toString());
    }

    private boolean isUnderWaterFeature(StructureFeature<?> structureFeature) {
        return structureFeature.getFeatureName().equals(StructureFeatures.OCEAN_RUIN.toLowerCase(Locale.ROOT))
                || structureFeature.getFeatureName().equals(StructureFeatures.SHIPWRECK.toLowerCase(Locale.ROOT))
                || structureFeature.getFeatureName().equals(StructureFeatures.RUINED_PORTAL.toLowerCase(Locale.ROOT));
    }

    private TranslatableComponent generateFallbackText() {
        return new TranslatableComponent(
                CLUE_TEXT_TRANSLATION_ID_PREFIX + "fallback"
        );
    }

    private String getClueTextTranslationIdFor(String structureFeatureId) {
        return CLUE_TEXT_TRANSLATION_ID_PREFIX + structureFeatureId;
    }

    private String getDirectionTranslationIdFor(String directionName) {
        return DIRECTION_TRANSLATION_ID_PREFIX + directionName;
    }

    private TranslatableComponent generateClueTextFor(
            String structureFeatureId,
            DirectionsAndDistances directionsAndDistances
    ) {
        return new TranslatableComponent(
                getClueTextTranslationIdFor(structureFeatureId),
                new TranslatableComponent(getDirectionTranslationIdFor(directionsAndDistances.northSouthDirection.getName())),
                directionsAndDistances.northSouthDistance,
                new TranslatableComponent(getDirectionTranslationIdFor(directionsAndDistances.westEastDirection.getName())),
                directionsAndDistances.westEastDistance
        );
    }

    private TranslatableComponent getClueText(CompoundTag tag) {
        // Get clue text from tag
        String structureFeatureId = tag.getString(FEATURE);
        String northSouthDirectionName = tag.getString(NORTH_SOUTH_DIRECTION);
        String westEastDirectionName = tag.getString(WEST_EAST_DIRECTION);
        int northSouthDistance = tag.getInt(NORTH_SOUTH_DISTANCE);
        int westEastDistance = tag.getInt(WEST_EAST_DISTANCE);
        return new TranslatableComponent(
                getClueTextTranslationIdFor(structureFeatureId),
                new TranslatableComponent(getDirectionTranslationIdFor(northSouthDirectionName)),
                northSouthDistance,
                new TranslatableComponent(getDirectionTranslationIdFor(westEastDirectionName)),
                westEastDistance
        );
    }

    private DirectionsAndDistances findDirectionsAndDistances(
            BlockPos playerPos,
            BlockPos destinationPos
    ) {
        DirectionsAndDistances dirDis = new DirectionsAndDistances();
        BlockPos diff = destinationPos.subtract(playerPos);
        dirDis.westEastDistance = Math.abs(diff.getX());
        dirDis.northSouthDistance = Math.abs(diff.getZ());
        if (diff.getX() > 0) {
            // East
            dirDis.westEastDirection = Direction.EAST;
        } else if (diff.getX() < 0) {
            // West
            dirDis.westEastDirection = Direction.WEST;
        }
        if (diff.getZ() > 0) {
            // South
            dirDis.northSouthDirection = Direction.SOUTH;
        } else if (diff.getZ() < 0) {
            // North
            dirDis.northSouthDirection = Direction.NORTH;
        }
        return dirDis;
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack itemStack) {
        return new TranslatableComponent(
                this.getDescriptionId(itemStack),
                itemStack.getOrCreateTag().getInt(CLUE_STEP) + 1
        );
    }

    private static class DirectionsAndDistances {
        public Direction westEastDirection;
        public Direction northSouthDirection;
        public int westEastDistance;
        public int northSouthDistance;
    }

    private static class StructureFeatureData {
        public StructureFeature<?> feature;
        public BlockPos position;

        public StructureFeatureData(@NotNull StructureFeature<?> feature, @NotNull BlockPos position) {
            this.feature = feature;
            this.position = position;
        }
    }
}
