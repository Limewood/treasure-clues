package com.buncord.treasureclues.item;

import com.buncord.treasureclues.TreasureCluesMod;
import com.buncord.treasureclues.WorldHeightHelper;
import com.buncord.treasureclues.networking.ModNetwork;
import com.buncord.treasureclues.networking.packet.OpenClueServerToClientPacket;
import com.buncord.treasureclues.ui.ClueScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TreasureClueItem extends Item {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String USED = "Used";
    public static final String CLUE_STEP = "ClueStep";
    public static final String FEATURE = "Feature";
    public static final String BIOME = "Biome";
    public static final String NORTH_SOUTH_DIRECTION = "NorthSouthDirection";
    public static final String WEST_EAST_DIRECTION = "WestEastDirection";
    public static final String NORTH_SOUTH_DISTANCE = "NorthSouthDistance";
    public static final String WEST_EAST_DISTANCE = "WestEastDistance";
    public static final String USED_FEATURES = "UsedFeatures";
    public static final String READ_POS = "ReadPos";
    private static final String USED_FEATURES_DELIMITER = ";";
    private static final String CLUE_TEXT_TRANSLATION_ID_PREFIX = TreasureCluesMod.MOD_ID + ".clue.text.";
    private static final String DIRECTION_TRANSLATION_ID_PREFIX = TreasureCluesMod.MOD_ID + ".clue.direction.";
    private static final ResourceLocation LOOT_RESOURCE_LOCATION_3 = new ResourceLocation(TreasureCluesMod.MOD_ID, "treasure/treasure_3steps");
    private static final ResourceLocation LOOT_RESOURCE_LOCATION_4 = new ResourceLocation(TreasureCluesMod.MOD_ID, "treasure/treasure_4steps");
    private static final ResourceLocation LOOT_RESOURCE_LOCATION_5 = new ResourceLocation(TreasureCluesMod.MOD_ID, "treasure/treasure_5steps");
    private static final String INVALID_DIMENSION_TRANSLATION_ID = TreasureCluesMod.MOD_ID + ".invalid_dimension";
    private static final String CLUE_STEP_TRANSLATION_ID = TreasureCluesMod.MOD_ID + ".tooltip.clue_step";
    private static final String READ_POS_TRANSLATION_ID = TreasureCluesMod.MOD_ID + ".tooltip.read_position";
    private static final String NOT_READ_TRANSLATION_ID = TreasureCluesMod.MOD_ID + ".tooltip.not_read";
    private static final String PRESS_SHIFT_FOR_INFO_TRANSLATION_ID = TreasureCluesMod.MOD_ID + ".tooltip.press_shift";
    private static final String ZERO_DISTANCE_TRANSLATION_ID = TreasureCluesMod.MOD_ID + ".clue.text.zero_distance";
    private static final String STEPS_TRANSLATION_ID_PREFIX = TreasureCluesMod.MOD_ID + ".clue.text.steps";
    private static final String BIOME_TRANSLATION_ID_PREFIX = TreasureCluesMod.MOD_ID + ".clue.text.biome.";
    private static final int MAX_STEP = 4; // 5 steps max (step is zero based)
    private static final int MIN_AVAILABLE_FEATURES_LEFT = 3; // Minimum number of unused features before final chest
    private static final double MAX_CLUE_DISTANCE = 3000; // Max distance to next clue, in blocks

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
            BlockPos playerPos = serverPlayer.blockPosition();

            CompoundTag tag = itemStack.getOrCreateTag();
            if (tag.getBoolean(USED)) {
                // The clue has already been generated once
                return InteractionResultHolder.pass(itemStack);
            }
            if (level.dimension() != Level.OVERWORLD) {
                // Don't generate clue in the Nether or the End (for now)
                return InteractionResultHolder.pass(itemStack);
            }
            // Mark clue as read
            tag.putBoolean(USED, true);
            // Save position where the clue was read
            tag.putString(READ_POS, playerPos.toShortString());
            final int step = tag.getInt(CLUE_STEP);
            LOGGER.debug("Used item with step " + step);
            // Find nearest structure feature
            // Remove already used features
            String usedFeaturesStr = tag.getString(USED_FEATURES);
            final String[] usedFeatures = !usedFeaturesStr.isEmpty()
                    ? usedFeaturesStr.split(USED_FEATURES_DELIMITER)
                    : new String[0];
            List<String> availableFeatures = new ArrayList<>(Arrays.stream(StructureFeatures.STRUCTURE_FEATURES).toList());
            for (String uf : usedFeatures) {
                availableFeatures.remove(uf);
            }

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
            // TODO Check if block above is powdered snow?
            // Check if player is at this position
            if (safePos.equals(playerPos)) {
                // Move it north one
                safePos = safePos.north();
            }

            // Place chest at position
            serverLevel.setBlockAndUpdate(safePos, Blocks.CHEST.defaultBlockState());
            ChestBlockEntity entity = (ChestBlockEntity) serverLevel.getBlockEntity(safePos);
            if (entity != null) {
                // Find approximate direction
                final DirectionsAndDistances dirDis = findDirectionsAndDistances(playerPos, safePos);
                // Round distances
                dirDis.northSouthDistance = Math.round(
                        dirDis.northSouthDistance / 10f
                ) * 10;
                dirDis.westEastDistance = Math.round(
                        dirDis.westEastDistance / 10f
                ) * 10;

                // Get biome
                Biome.BiomeCategory biome = level.getBiome(safePos).getBiomeCategory();

                String structureFeatureId;
                if (!isFallbackChest) {
                    structureFeatureId = structureFeatureData.feature.getFeatureName();
                    // Save feature
                    setFeature(tag, structureFeatureId);
                    // Save directions and distances
                    setDirectionsAndDistances(tag, dirDis);
                    // Save biome
                    setBiome(tag, biome);
                } else {
                    structureFeatureId = null;
                }

                // Random chance or max steps or min available features reached or fallback
                if (isFallbackChest
                        || availableFeatures.size() < MIN_AVAILABLE_FEATURES_LEFT
                        || step == MAX_STEP
                        || step > 1 && level.random.nextInt(MAX_STEP) < step) {
                    // Use different loot tables depending on number of steps
                    ResourceLocation lootTable = switch (step) {
                        case 4 -> LOOT_RESOURCE_LOCATION_4;
                        case 5 -> LOOT_RESOURCE_LOCATION_5;
                        default -> LOOT_RESOURCE_LOCATION_3;
                    };
                    entity.setLootTable(lootTable, serverLevel.random.nextLong());
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
                        dirDis,
                        biome
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
                LOGGER.error("No chest entity found at chest location!");
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

    @Override
    public void appendHoverText(@NotNull ItemStack itemStack, @Nullable Level level,
                                @NotNull List<Component> components, @NotNull TooltipFlag flag) {
        super.appendHoverText(itemStack, level, components, flag);
        // Clue step
        components.add(new TranslatableComponent(
                        CLUE_STEP_TRANSLATION_ID,
                        itemStack.getOrCreateTag().getInt(CLUE_STEP) + 1
                ).withStyle(ChatFormatting.BLUE)
        );
        if (Screen.hasShiftDown()) {
            if (itemStack.getOrCreateTag().getString(READ_POS).isBlank()) {
                components.add(new TranslatableComponent(
                                NOT_READ_TRANSLATION_ID
                        ).withStyle(ChatFormatting.ITALIC)
                );
            } else {
                components.add(new TranslatableComponent(
                                READ_POS_TRANSLATION_ID,
                                itemStack.getOrCreateTag().getString(READ_POS)
                        ).withStyle(ChatFormatting.ITALIC)
                );
            }
        } else {
            components.add(new TranslatableComponent(
                            PRESS_SHIFT_FOR_INFO_TRANSLATION_ID
                    ).withStyle(ChatFormatting.AQUA)
            );
        }
    }

    private StructureFeatureData findNextCluePos(List<String> availableFeatures, ServerLevel level, ServerPlayer player) {
        List<String> freeFeatures = new ArrayList<>(availableFeatures);
        int structureIndex = level.random.nextInt(freeFeatures.size());
        final String structureFeatureId = freeFeatures.get(structureIndex);
        StructureFeature<?> structureFeature = StructureFeature.STRUCTURES_REGISTRY.get(structureFeatureId);
        LOGGER.debug("Found structure feature: " + structureFeatureId);
        BlockPos featureBlockPos = level.findNearestMapFeature(structureFeature, player.blockPosition(), 100, false);
        LOGGER.debug("Nearest feature block pos: " + featureBlockPos);
        StructureFeatureData data;
        if (featureBlockPos == null && freeFeatures.size() > 1) {
            freeFeatures.remove(structureFeatureId);
            data = findNextCluePos(freeFeatures, level, player);
        } else if (featureBlockPos != null) {
            // Check if it's too far away
            double distance = Math.sqrt(featureBlockPos.distSqr(player.blockPosition()));
            if (distance > MAX_CLUE_DISTANCE && freeFeatures.size() > 1) {
                LOGGER.debug("Too far to feature " + structureFeatureId + ", try another one");
                freeFeatures.remove(structureFeatureId);
                data = findNextCluePos(freeFeatures, level, player);
            } else {
                data = new StructureFeatureData(structureFeature, featureBlockPos);
            }
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

    private void setBiome(CompoundTag compoundTag, Biome.BiomeCategory biome) {
        compoundTag.putString(BIOME, biome.getName());
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
        return structureFeature.getFeatureName().equals(StructureFeatures.OCEAN_RUIN)
                || structureFeature.getFeatureName().equals(StructureFeatures.SHIPWRECK)
                || structureFeature.getFeatureName().equals(StructureFeatures.RUINED_PORTAL)
                || structureFeature.getFeatureName().equals(StructureFeatures.SWAMP_HUT);
    }

    private TranslatableComponent generateFallbackText() {
        return new TranslatableComponent(
                CLUE_TEXT_TRANSLATION_ID_PREFIX + "fallback"
        );
    }

    private String getClueTextTranslationIdFor(String structureFeatureId) {
        return CLUE_TEXT_TRANSLATION_ID_PREFIX + structureFeatureId;
    }

    private TranslatableComponent getDirectionTranslationFor(String directionName) {
        return new TranslatableComponent(DIRECTION_TRANSLATION_ID_PREFIX + directionName);
    }

    private TranslatableComponent generateClueTextFor(
            String structureFeatureId,
            DirectionsAndDistances directionsAndDistances,
            Biome.BiomeCategory biome
    ) {
        return getClueTextTranslationComponent(
                structureFeatureId,
                directionsAndDistances.northSouthDirection.getName(),
                directionsAndDistances.northSouthDistance,
                directionsAndDistances.westEastDirection.getName(),
                directionsAndDistances.westEastDistance,
                biome.getName()
        );
    }

    private TranslatableComponent getClueText(CompoundTag tag) {
        // Get clue text from tag
        String structureFeatureId = tag.getString(FEATURE);
        String northSouthDirectionName = tag.getString(NORTH_SOUTH_DIRECTION);
        String westEastDirectionName = tag.getString(WEST_EAST_DIRECTION);
        int northSouthDistance = tag.getInt(NORTH_SOUTH_DISTANCE);
        int westEastDistance = tag.getInt(WEST_EAST_DISTANCE);
        String biomeName = tag.getString(BIOME);
        return getClueTextTranslationComponent(
                structureFeatureId,
                northSouthDirectionName,
                northSouthDistance,
                westEastDirectionName,
                westEastDistance,
                biomeName
        );
    }

    private TranslatableComponent getClueTextTranslationComponent(
            String structureFeatureId,
            String northSouthDirectionName,
            int northSouthDistance,
            String westEastDirectionName,
            int westEastDistance,
            String biome
    ) {
        return new TranslatableComponent(
                getClueTextTranslationIdFor(structureFeatureId),
                getDirectionTranslationFor(northSouthDirectionName),
                getDistanceTranslationFor(northSouthDistance),
                getDirectionTranslationFor(westEastDirectionName),
                getDistanceTranslationFor(westEastDistance),
                getBiomeTranslationFor(biome)
        );
    }

    private TranslatableComponent getDistanceTranslationFor(int distance) {
        if (distance == 0) {
            return new TranslatableComponent(ZERO_DISTANCE_TRANSLATION_ID);
        }
        // Non-zero distance - pick a random step word
        int variant = (int) Math.ceil(Math.random()*3);
        return new TranslatableComponent(STEPS_TRANSLATION_ID_PREFIX + variant, distance);
    }

    private TranslatableComponent getBiomeTranslationFor(String biomeName) {
        return new TranslatableComponent(BIOME_TRANSLATION_ID_PREFIX + biomeName);
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
        } else {
            // Default
            dirDis.westEastDirection = Direction.WEST;
        }
        if (diff.getZ() > 0) {
            // South
            dirDis.northSouthDirection = Direction.SOUTH;
        } else if (diff.getZ() < 0) {
            // North
            dirDis.northSouthDirection = Direction.NORTH;
        } else {
            // Default
            dirDis.northSouthDirection = Direction.NORTH;
        }
        return dirDis;
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
