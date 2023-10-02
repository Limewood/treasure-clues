package com.buncord.treasureclues.capability;

import net.minecraft.nbt.CompoundTag;

/**
 * Capability of player to receive clue once on login
 */
public class ClueReceived {
    private static final String RECEIVED = "received";

    private boolean received;

    public boolean isReceived() {
        return received;
    }

    public void setReceived(boolean received) {
        this.received = received;
    }

    public void copyFrom(ClueReceived source) {
        this.received = source.received;
    }

    public void saveNBTData(CompoundTag nbt) {
        nbt.putBoolean(RECEIVED, received);
    }

    public void loadNBTData(CompoundTag nbt) {
        received = nbt.getBoolean(RECEIVED);
    }
}
