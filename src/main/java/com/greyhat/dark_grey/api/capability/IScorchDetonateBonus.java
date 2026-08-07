package com.greyhat.dark_grey.api.capability;

/**
 * Implemented by RPG components whose weapon grants a percentage bonus
 * to the Scorch-mark detonation damage.
 *
 * <p>
 * The base detonation damage is a fixed constant (default 50).
 * Components that implement this interface add their bonus on top:
 * {@code finalDmg = baseDmg * (1 + bonus)}.
 * Multiple components on the same item stack additively.
 * </p>
 */
public interface IScorchDetonateBonus {

    /**
     * @return the fractional bonus, e.g. 0.5 for +50 %.
     */
    float getScorchDetonateBonus();
}
