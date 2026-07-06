package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.entity.EntityScythe;

public class ComponentCalamity implements IRPGComponent, IOnRightClick, IHasTooltip {

    private static final int COOLDOWN_TICKS = 140; // 7 seconds

    @Override
    public String getComponentId() {
        return "劫难";
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        tooltip.add(
            "\u00A75\u2726 \u52AB\u96BE \u00A77| \u00A7d\u53F3\u952E\u91CA\u653E360\u5EA6\u6BC1\u706D\u6A2A\u626B (\u51B7\u53747\u79D2)");
    }

    @Override
    public ItemStack onRightClick(ItemStack weaponStack, World world, EntityPlayer player) {
        if (world.isRemote) {
            return weaponStack;
        }

        long current_time = world.getTotalWorldTime();
        long last_used_time = 0;

        if (weaponStack.hasTagCompound() && weaponStack.getTagCompound()
            .hasKey("calamity_last_used")) {
            last_used_time = weaponStack.getTagCompound()
                .getLong("calamity_last_used");
        }

        if (current_time - last_used_time < COOLDOWN_TICKS) {
            if (!world.isRemote) {
                long remaining_ticks = COOLDOWN_TICKS - (current_time - last_used_time);
                float remaining_seconds = remaining_ticks / 20.0f;
                player.addChatMessage(
                    new ChatComponentText(
                        EnumChatFormatting.RED + "【劫难】技能冷却中，还需等待 " + String.format("%.1f", remaining_seconds) + " 秒。"));
            }
            return weaponStack;
        }

        // Set the cooldown
        if (!weaponStack.hasTagCompound()) {
            weaponStack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        }
        weaponStack.getTagCompound()
            .setLong("calamity_last_used", current_time);

        // Spawn the scythe entity
        EntityScythe scythe_entity = new EntityScythe(world, player);
        world.spawnEntityInWorld(scythe_entity);

        return weaponStack;
    }
}
