package de.cuteghg.coinflip.service;

import de.cuteghg.coinflip.model.OpenCoinflip;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public final class CoinflipService {
    private final Map<UUID, OpenCoinflip> flips = new HashMap<>();
    private final Map<UUID, UUID> creatorToFlip = new HashMap<>();

    public synchronized Optional<OpenCoinflip> getFlip(UUID flipId) {
        return Optional.ofNullable(flips.get(flipId));
    }

    public synchronized List<OpenCoinflip> listOpenFlips() {
        List<OpenCoinflip> result = new ArrayList<>(flips.values());
        result.sort(Comparator.comparing(OpenCoinflip::id));
        return result;
    }

    public synchronized Optional<OpenCoinflip> getCreatorFlip(UUID creator) {
        UUID flipId = creatorToFlip.get(creator);
        if (flipId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(flips.get(flipId));
    }

    public synchronized OpenCoinflip create(UUID creator, int stake) {
        if (stake <= 0) {
            throw new IllegalArgumentException("Stake must be > 0");
        }
        if (creatorToFlip.containsKey(creator)) {
            throw new IllegalStateException("Creator already has an open flip");
        }
        OpenCoinflip flip = new OpenCoinflip(UUID.randomUUID(), creator, stake);
        flips.put(flip.id(), flip);
        creatorToFlip.put(creator, flip.id());
        return flip;
    }

    public synchronized Optional<OpenCoinflip> cancel(UUID actor, UUID flipId) {
        OpenCoinflip flip = flips.get(flipId);
        if (flip == null || !flip.creator().equals(actor) || flip.locked()) {
            return Optional.empty();
        }
        removeFlip(flip);
        return Optional.of(flip);
    }

    public synchronized Optional<OpenCoinflip> cancelByCreator(UUID actor) {
        UUID flipId = creatorToFlip.get(actor);
        if (flipId == null) {
            return Optional.empty();
        }
        return cancel(actor, flipId);
    }

    public synchronized Optional<OpenCoinflip> lockForJoin(UUID challenger, UUID flipId) {
        OpenCoinflip flip = flips.get(flipId);
        if (flip == null || flip.locked()) {
            return Optional.empty();
        }
        if (flip.creator().equals(challenger)) {
            return Optional.empty();
        }

        flip.setLocked(true);
        return Optional.of(flip);
    }

    public synchronized Optional<JoinOutcome> completeJoin(UUID challenger, UUID flipId, Random random) {
        OpenCoinflip flip = flips.get(flipId);
        if (flip == null || !flip.locked() || flip.creator().equals(challenger)) {
            return Optional.empty();
        }
        UUID winner = random.nextBoolean() ? flip.creator() : challenger;
        UUID loser = winner.equals(flip.creator()) ? challenger : flip.creator();
        int pot = flip.stake() * 2;

        removeFlip(flip);
        return Optional.of(new JoinOutcome(flip.id(), flip.creator(), challenger, winner, loser, flip.stake(), pot));
    }

    public synchronized void abortJoinLock(UUID flipId) {
        OpenCoinflip flip = flips.get(flipId);
        if (flip != null) {
            flip.setLocked(false);
        }
    }

    public synchronized List<OpenCoinflip> drainAllOpenFlips() {
        List<OpenCoinflip> drained = new ArrayList<>(flips.values());
        flips.clear();
        creatorToFlip.clear();
        return drained;
    }

    private void removeFlip(OpenCoinflip flip) {
        flips.remove(flip.id());
        creatorToFlip.remove(flip.creator());
    }

    public record JoinOutcome(
            UUID flipId,
            UUID creator,
            UUID challenger,
            UUID winner,
            UUID loser,
            int stakePerPlayer,
            int totalPot
    ) {
    }
}
