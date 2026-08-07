package com.greyhat.dark_grey.api.capability;

import java.util.List;

import net.minecraft.util.EnumChatFormatting;

/**
 * Implemented by RPG components whose weapon grants the ability to ignite Scorch marks.
 */
public interface IScorchIgniter {

    /**
     * Call this in the component's addTooltipLines method.
     */
    default void addIgniterTooltip(List<String> tooltip) {
        tooltip.add(EnumChatFormatting.LIGHT_PURPLE + "[引燃] " + EnumChatFormatting.GRAY + "该武器造成的有效直接攻击可以引爆目标身上的灼痕印记。");
    }
}
