package com.greyhat.dark_grey.api;

/**
 * Marker interface implemented by all RPG item container classes
 * ({@code ItemRPGWeapon}, {@code ItemRPGTool}, {@code ItemRPGHoe},
 * {@code ItemRPGArmor}, {@code ItemRPGAccessory}, {@code ItemRPGBow}).
 *
 * <p>
 * Purpose: enables the {@link RPGItemDataManager} to hot-reload
 * all registered RPG items via a single {@code instanceof} check
 * instead of an ever-growing chain of type checks.
 * </p>
 */
public interface IRPGItemContainer {

    /** Returns the unique RPG item ID (matches the JSON "id" field). */
    String getRpgItemId();

    /**
     * Re-reads component definitions from {@link RPGItemDataManager}
     * and rebuilds all internal capability-typed sublists.
     * Called automatically by the hot-reload system when the JSON config changes.
     */
    void rebuildComponents();

    /**
     * Returns the cached list of player death event handlers.
     */
    java.util.List<com.greyhat.dark_grey.api.capability.IOnPlayerDeath> getPlayerDeathHandlers();
}
