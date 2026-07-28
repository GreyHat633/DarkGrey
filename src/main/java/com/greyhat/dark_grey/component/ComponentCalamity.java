package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.greyhat.dark_grey.api.CooldownHelper;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.entity.EntityScythe;

public class ComponentCalamity implements IRPGComponent, IOnRightClick, IHasTooltip {

    private static final int COOLDOWN_TICKS = 60; // 3 seconds
    private static final String LEGACY_COOLDOWN_KEY = "calamity_last_used";
    private static final String COOLDOWN_END_MILLIS_KEY = "calamity_cooldown_end_millis";

    @Override
    public String getComponentId() {
        return "劫难";
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        tooltip.add(
            "\u00A75\u2726 \u52AB\u96BE \u00A77| \u00A7d\u53F3\u952E\u91CA\u653E\u534A\u5F845\u683C\u7684360\u5EA6\u6BC1\u706D\u6A2A\u626B (\u51B7\u53743\u79D2)");
    }

    @Override
    public ItemStack onRightClick(ItemStack weaponStack, World world, EntityPlayer player) {
        if (world.isRemote) {
            return weaponStack;
        }

        if (!weaponStack.hasTagCompound()) {
            weaponStack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        }
        long cooldownMillis = CooldownHelper.ticksToMillis(COOLDOWN_TICKS);
        long remainingMillis = CooldownHelper.getRemainingMillis(
            weaponStack.getTagCompound(),
            COOLDOWN_END_MILLIS_KEY,
            cooldownMillis,
            LEGACY_COOLDOWN_KEY);
        if (remainingMillis > 0L) {
            player.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.RED + "【劫难】技能冷却中，还需等待 "
                        + String.format("%.1f", remainingMillis / 1000.0D)
                        + " 秒。"));
            return weaponStack;
        }

        CooldownHelper
            .start(weaponStack.getTagCompound(), COOLDOWN_END_MILLIS_KEY, cooldownMillis, LEGACY_COOLDOWN_KEY);

        // Spawn the scythe entity
        EntityScythe scythe_entity = new EntityScythe(world, player);
        world.spawnEntityInWorld(scythe_entity);

        return weaponStack;
    }
}
