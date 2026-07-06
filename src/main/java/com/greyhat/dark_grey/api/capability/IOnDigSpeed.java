package com.greyhat.dark_grey.api.capability;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

/**
 * Capability interface for components that modify a tool's dig (mining) speed.
 *
 * <p>
 * Triggered by the Forge method {@code Item.getDigSpeed(ItemStack, Block, int)}.
 * Multiple components are chained in a pipeline: each receives the current speed
 * (possibly already modified by previous components) and returns the new speed.
 * </p>
 *
 * <p>
 * In Godot terms, this is like a series of {@code AnimationTree} blend nodes,
 * where each node modifies the animation parameter before passing it downstream.
 * </p>
 */
public interface IOnDigSpeed {

    /**
     * Modifies the dig speed for this tool against the given block.
     *
     * @param toolStack     the ItemStack of the tool
     * @param targetBlock   the block being mined
     * @param blockMetadata the metadata of the block being mined
     * @param currentSpeed  the current dig speed (from material or previous components)
     * @return the modified dig speed
     */
    float modifyDigSpeed(ItemStack toolStack, Block targetBlock, int blockMetadata, float currentSpeed);
}
