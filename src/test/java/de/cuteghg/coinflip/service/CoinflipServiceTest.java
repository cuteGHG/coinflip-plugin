package de.cuteghg.coinflip.service;

import de.cuteghg.coinflip.model.OpenCoinflip;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CoinflipServiceTest {

    @Test
    void createAndCancelByCreatorWorks() {
        CoinflipService service = new CoinflipService();
        UUID creator = UUID.randomUUID();

        OpenCoinflip flip = service.create(creator, 16);
        assertEquals(1, service.listOpenFlips().size());

        Optional<OpenCoinflip> cancelled = service.cancel(creator, flip.id());
        assertTrue(cancelled.isPresent());
        assertEquals(0, service.listOpenFlips().size());
    }

    @Test
    void selfJoinIsBlocked() {
        CoinflipService service = new CoinflipService();
        UUID creator = UUID.randomUUID();
        OpenCoinflip flip = service.create(creator, 8);

        assertTrue(service.lockForJoin(creator, flip.id()).isEmpty());
    }

    @Test
    void lockPreventsDoubleJoin() {
        CoinflipService service = new CoinflipService();
        UUID creator = UUID.randomUUID();
        UUID challengerA = UUID.randomUUID();
        UUID challengerB = UUID.randomUUID();

        OpenCoinflip flip = service.create(creator, 12);
        assertTrue(service.lockForJoin(challengerA, flip.id()).isPresent());
        assertTrue(service.lockForJoin(challengerB, flip.id()).isEmpty());
    }

    @Test
    void completeJoinRemovesFlipAndSetsPot() {
        CoinflipService service = new CoinflipService();
        UUID creator = UUID.randomUUID();
        UUID challenger = UUID.randomUUID();

        OpenCoinflip flip = service.create(creator, 20);
        assertTrue(service.lockForJoin(challenger, flip.id()).isPresent());

        Optional<CoinflipService.JoinOutcome> outcome = service.completeJoin(challenger, flip.id(), new FixedRandom(true));
        assertTrue(outcome.isPresent());
        assertEquals(40, outcome.get().totalPot());
        assertEquals(creator, outcome.get().winner());
        assertTrue(service.getFlip(flip.id()).isEmpty());
    }

    private static final class FixedRandom extends Random {
        private final boolean value;

        private FixedRandom(boolean value) {
            this.value = value;
        }

        @Override
        public boolean nextBoolean() {
            return value;
        }
    }
}
