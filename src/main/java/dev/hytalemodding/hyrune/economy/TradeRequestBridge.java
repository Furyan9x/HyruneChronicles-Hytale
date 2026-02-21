package dev.hytalemodding.hyrune.economy;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Root-module bridge for economy trade request handling.
 * Allows social UI to trigger trade flows without compile-time dependency
 * on the economy module implementation classes.
 */
public final class TradeRequestBridge {
    private static final TradeRequestHandler NOOP =
        (requester, target) -> TradeRequestResult.failure("Trade service is unavailable.");

    private static volatile TradeRequestHandler handler = NOOP;

    private TradeRequestBridge() {
    }

    public static void registerHandler(@Nullable TradeRequestHandler tradeRequestHandler) {
        handler = tradeRequestHandler != null ? tradeRequestHandler : NOOP;
    }

    public static void clearHandler() {
        handler = NOOP;
    }

    @Nonnull
    public static TradeRequestResult requestTrade(@Nullable PlayerRef requester, @Nullable PlayerRef target) {
        if (requester == null || target == null) {
            return TradeRequestResult.failure("Unable to start trade: invalid player reference.");
        }
        try {
            TradeRequestResult result = handler.request(requester, target);
            return result != null ? result : TradeRequestResult.failure("Trade request failed.");
        } catch (RuntimeException ex) {
            return TradeRequestResult.failure("Trade request failed: " + ex.getMessage());
        }
    }

    @FunctionalInterface
    public interface TradeRequestHandler {
        @Nonnull TradeRequestResult request(@Nonnull PlayerRef requester, @Nonnull PlayerRef target);
    }

    public record TradeRequestResult(boolean success, @Nullable String message) {
        public static TradeRequestResult success(@Nullable String message) {
            return new TradeRequestResult(true, message);
        }

        public static TradeRequestResult failure(@Nonnull String message) {
            return new TradeRequestResult(false, message);
        }
    }
}
