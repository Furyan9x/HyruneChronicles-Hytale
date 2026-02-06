package dev.hytalemodding.origins.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;

/**
 * Component for storing game mode selection and starter kit status on player entities.
 * This data persists across sessions.
 */
public class GameModeDataComponent implements Component<EntityStore> {

    public static final BuilderCodec<GameModeDataComponent> CODEC = BuilderCodec.builder(GameModeDataComponent.class, GameModeDataComponent::new)
            .addField(new KeyedCodec<>("GameMode", Codec.STRING),
                    GameModeDataComponent::setGameMode,
                    GameModeDataComponent::getGameMode)
            .addField(new KeyedCodec<>("HasReceivedStarter", Codec.BOOLEAN),
                    GameModeDataComponent::setHasReceivedStarter,
                    GameModeDataComponent::hasReceivedStarter)
            .addField(new KeyedCodec<>("SelectionLocked", Codec.BOOLEAN),
                    GameModeDataComponent::setSelectionLocked,
                    GameModeDataComponent::isSelectionLocked)
            .build();
    @Nullable
    private String gameMode;

    private boolean hasReceivedStarter;
    private boolean selectionLocked;
    /**
     * Default constructor - creates component with no game mode selected
     */
    public GameModeDataComponent() {
        this.gameMode = null;
        this.hasReceivedStarter = false;
        this.selectionLocked = false;
    }


    /**
     * Constructor with initial values
     */
    public GameModeDataComponent(@Nullable String gameMode, boolean hasReceivedStarter, boolean selectionLocked) {
        this.gameMode = gameMode;
        this.hasReceivedStarter = hasReceivedStarter;
        this.selectionLocked = selectionLocked;
    }

    /**
     * Get the player's selected game mode.
     *
     * @return The game mode string, or null if not yet selected
     */
    @Nullable
    public String getGameMode() {
        return gameMode;
    }

    /**
     * Set the player's game mode.
     * Should only be called once per player.
     *
     * @param gameMode The game mode to set
     */
    public void setGameMode(@Nullable String gameMode) {
        this.gameMode = gameMode;
    }

    /**
     * Check if the player has received their starter kit.
     *
     * @return true if starter kit has been given
     */
    public boolean hasReceivedStarter() {
        return hasReceivedStarter;
    }

    /**
     * Mark that the player has received their starter kit.
     */
    public void setHasReceivedStarter(boolean hasReceivedStarter) {
        this.hasReceivedStarter = hasReceivedStarter;
    }

    /**
     * Check if the player's game mode selection is locked.
     * Once locked, they cannot change their game mode.
     *
     * @return true if selection is locked
     */
    public boolean isSelectionLocked() {
        return selectionLocked;
    }

    /**
     * Lock/unlock the player's game mode selection.
     *
     * @param selectionLocked true to lock selection
     */
    public void setSelectionLocked(boolean selectionLocked) {
        this.selectionLocked = selectionLocked;
    }

    /**
     * Check if the player can select a game mode.
     *
     * @return true if they haven't selected or selection isn't locked
     */
    public boolean canSelectGameMode() {
        return !selectionLocked;
    }

    /**
     * Check if the player has completed initial setup.
     *
     * @return true if they have a game mode and received starter kit
     */
    public boolean hasCompletedSetup() {
        return gameMode != null && hasReceivedStarter && selectionLocked;
    }

    /**
     * Mark that the player has received their starter kit.
     * Helper method for compatibility with old API.
     */
    public void markStarterReceived() {
        this.hasReceivedStarter = true;
    }

    /**
     * Lock the player's game mode selection.
     * Helper method for compatibility with old API.
     */
    public void lockSelection() {
        this.selectionLocked = true;
    }

    @Override
    public GameModeDataComponent clone() {
        GameModeDataComponent cloned = new GameModeDataComponent(gameMode, hasReceivedStarter, selectionLocked);
        return cloned;
    }

    @Override
    public String toString() {
        return "GameModeDataComponent{" +
                "gameMode='" + gameMode + '\'' +
                ", hasReceivedStarter=" + hasReceivedStarter +
                ", selectionLocked=" + selectionLocked +
                '}';
    }

    }