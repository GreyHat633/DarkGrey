package com.greyhat.dark_grey.api.capability;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Capability interface for components that react when a block is broken with this tool.
 *
 * <p>Triggered by {@code ItemTool.onBlockDestroyed()} inside the container's override.
 * Useful for effects like auto-smelting, area mining, or fortune bonuses.</p>
 *
 * <p>Note: The coordinate parameters use {@code x, y, z} names because they directly
 * correspond to Minecraft's block coordinate system (similar to Godot's Vector3i).</p>
 */
public interface IOnBlockBreak {

    /**
     * Called when a block is successfully broken using this tool.
     *
     * @param toolStack     the ItemStack of the tool used
     * @param player        the player who broke the block
     * @param world         the world instance
     * @param x             block X coordinate
     * @param y             block Y coordinate
     * @param z             block Z coordinate
     * @param brokenBlock   the Block that was broken
     * @param blockMetadata the metadata of the broken block
     */
    void onBlockBreak(ItemStack toolStack, EntityPlayer player, World world,
                      int x, int y, int z, Block brokenBlock, int blockMetadata);
}