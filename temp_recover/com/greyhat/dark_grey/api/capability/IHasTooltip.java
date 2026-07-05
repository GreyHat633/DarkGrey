package com.greyhat.dark_grey.api.capability;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * Capability interface for components that contribute information lines to the
 * item's tooltip (hover text in inventory).
 *
 * <p>Called from the container's {@code addInformation()} override, which runs
 * client-side only. Each component appends its own description lines to the list.</p>
 *
 * <p>In Godot terms, this is like each component providing its own
 * {@code get_tooltip_text()} method, and the item node aggregates them all
 * into a single RichTextLabel for the UI.</p>
 *
 * <p>Example tooltip line: {@code "§4◆ Lifesteal §7| §c+15% damage healed"}</p>
 */
public interface IHasTooltip {

    /**
     * Appends tooltip lines for this component.
     *
     * @param itemStack    the ItemStack being hovered
     * @param player       the player viewing the tooltip (may be null in edge cases)
     * @param tooltipLines the mutable list of tooltip strings to append to
     * @param showAdvanced true if advanced tooltips are enabled (F3+H)
     */
    void addTooltipLines(ItemStack itemStack, EntityPlayer player,
                         List<String> tooltipLines, boolean showAdvanced);
}