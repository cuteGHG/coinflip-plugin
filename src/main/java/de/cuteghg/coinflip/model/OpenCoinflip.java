package de.cuteghg.coinflip.model;

import java.util.UUID;

public final class OpenCoinflip {
    private final UUID id;
    private final UUID creator;
    private final int stake;
    private boolean locked;

    public OpenCoinflip(UUID id, UUID creator, int stake) {
        this.id = id;
        this.creator = creator;
        this.stake = stake;
        this.locked = false;
    }

    public UUID id() {
        return id;
    }

    public UUID creator() {
        return creator;
    }

    public int stake() {
        return stake;
    }

    public boolean locked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}
