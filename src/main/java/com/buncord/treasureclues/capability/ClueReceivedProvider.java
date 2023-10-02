package com.buncord.treasureclues.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provider for capability of player to receive clue once on login
 */
public class ClueReceivedProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static Capability<ClueReceived> CLUE_RECEIVED = CapabilityManager.get(new CapabilityToken<>() {});

    private ClueReceived clueReceived = null;
    private final LazyOptional<ClueReceived> optional = LazyOptional.of(this::getClueReceived);

    private ClueReceived getClueReceived() {
        if (this.clueReceived == null) {
            this.clueReceived = new ClueReceived();
        }

        return this.clueReceived;
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == CLUE_RECEIVED) {
            return optional.cast();
        }

        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        getClueReceived().saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        getClueReceived().loadNBTData(nbt);
    }
}
