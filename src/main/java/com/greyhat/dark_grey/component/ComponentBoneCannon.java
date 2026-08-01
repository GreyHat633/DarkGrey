package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnHeldTick;
import com.greyhat.dark_grey.api.capability.IOnLeftClick;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick;
import com.greyhat.dark_grey.entity.EntityBoneMarrowProjectile;

import cpw.mods.fml.common.registry.GameRegistry;

public class ComponentBoneCannon
    implements IRPGComponent, IOnWeaponUsingTick, IOnHeldTick, IOnRightClick, IOnLeftClick, IHasTooltip {

    private int loadTicksRequired = 50;
    private int slownessAmplifier = 2; // Level 3 (0-indexed)
    private int slownessDurationTicks = 10;
    private float projectileDamage = 23.0f;
    private float projectileSpeed = 3.5f;

    public int getLoadTicksRequired() {
        return this.loadTicksRequired;
    }

    @Override
    public String getComponentId() {
        return "骸骨大炮";
    }

    @Override
    public void configure(JsonObject params) {
        if (params.has("loadTicksRequired")) {
            this.loadTicksRequired = params.get("loadTicksRequired")
                .getAsInt();
        }
        if (params.has("slownessAmplifier")) {
            this.slownessAmplifier = params.get("slownessAmplifier")
                .getAsInt();
        }
        if (params.has("projectileDamage")) {
            this.projectileDamage = params.get("projectileDamage")
                .getAsFloat();
        }
        if (params.has("projectileSpeed")) {
            this.projectileSpeed = params.get("projectileSpeed")
                .getAsFloat();
        }
    }

    @Override
    public void onHeldTick(ItemStack stack, World world, EntityPlayer player) {
        // Apply Slowness III (amplifier 2) for 10 ticks while holding the weapon
        if (!player.worldObj.isRemote) {
            PotionEffect slowness = new PotionEffect(Potion.moveSlowdown.id, 20, 2, true);
            player.addPotionEffect(slowness);
        }
    }

    @Override
    public ItemStack onRightClick(ItemStack weaponStack, World world, EntityPlayer player) {
        NBTTagCompound nbt = weaponStack.getTagCompound();
        boolean isLoaded = nbt != null && nbt.getBoolean("DarkGreyBoneCannonLoaded");

        if (!isLoaded) {
            // Weapon is NOT loaded, start reloading
            player.setItemInUse(
                weaponStack,
                weaponStack.getItem()
                    .getMaxItemUseDuration(weaponStack));
        }

        return weaponStack;
    }

    @Override
    public boolean onLeftClick(ItemStack weaponStack, EntityPlayer player) {
        World world = player.worldObj;
        NBTTagCompound nbt = weaponStack.getTagCompound();
        boolean isLoaded = nbt != null && nbt.getBoolean("DarkGreyBoneCannonLoaded");

        if (isLoaded) {
            // Weapon is loaded, FIRE!

            if (!world.isRemote) {
                // Spawn projectile
                EntityBoneMarrowProjectile projectile = new EntityBoneMarrowProjectile(
                    world,
                    player,
                    projectileDamage,
                    100);

                // Adjust aim vector manually (like EntityArrow)
                projectile.setThrowableHeading(
                    player.getLookVec().xCoord,
                    player.getLookVec().yCoord,
                    player.getLookVec().zCoord,
                    projectileSpeed,
                    0.0f);

                world.spawnEntityInWorld(projectile);

                // Sound
                world.playSoundAtEntity(
                    player,
                    "random.explode",
                    1.0F,
                    1.0F / (world.rand.nextFloat() * 0.4F + 1.2F) + 0.5F);

                // Clear loaded state
                if (nbt != null) {
                    nbt.removeTag("DarkGreyBoneCannonLoaded");
                }
            }

            return true; // Cancel default swing behavior since we fired
        }

        return false;
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
        // count goes down from maxItemUseDuration
        int maxDuration = stack.getItem()
            .getMaxItemUseDuration(stack);
        int ticksInUse = maxDuration - count;

        NBTTagCompound nbt = stack.getTagCompound();
        boolean isLoaded = nbt != null && nbt.getBoolean("DarkGreyBoneCannonLoaded");

        // Don't reload if already loaded
        if (isLoaded) {
            player.clearItemInUse();
            return;
        }

        if (ticksInUse == loadTicksRequired) {
            // Reached reload threshold, try to consume ammo
            boolean hasAmmo = false;
            Item marrowItem = GameRegistry.findItem("dark_grey", "hardened_bone_marrow");

            if (player.capabilities.isCreativeMode) {
                hasAmmo = true;
            } else if (marrowItem != null && player.inventory.consumeInventoryItem(marrowItem)) {
                hasAmmo = true;
            }

            if (hasAmmo) {
                if (!player.worldObj.isRemote) {
                    if (nbt == null) {
                        nbt = new NBTTagCompound();
                        stack.setTagCompound(nbt);
                    }
                    nbt.setBoolean("DarkGreyBoneCannonLoaded", true);
                    player.worldObj.playSoundAtEntity(player, "random.click", 1.0F, 1.2F);
                    player.stopUsingItem();

                    if (player instanceof EntityPlayerMP) {
                        ((EntityPlayerMP) player).updateHeldItem(); // Sync NBT visually
                    }
                }
            } else {
                if (!player.worldObj.isRemote) {
                    // Out of ammo sound
                    player.worldObj.playSoundAtEntity(player, "random.click", 1.0F, 1.5F);
                }
                // Stop using the item
                player.clearItemInUse();
            }
        }
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List tooltipLines, boolean showAdvanced) {
        float baseDmg = projectileDamage;
        tooltipLines.add(EnumChatFormatting.GOLD + "基础伤害：" + (int) baseDmg);
        tooltipLines.add(EnumChatFormatting.AQUA + "炮弹命中产生范围爆炸，施加骨折");
        tooltipLines.add("");
        tooltipLines.add(EnumChatFormatting.YELLOW + "左键：发射硬化骨髓");
        tooltipLines.add(EnumChatFormatting.YELLOW + "长按右键：装填大炮");
        tooltipLines.add("");
        tooltipLines.add(EnumChatFormatting.GREEN + "装填大炮：");
        tooltipLines.add(EnumChatFormatting.GRAY + "  蓄力 " + (loadTicksRequired / 20.0f) + " 秒，消耗背包中的一个硬化骨髓完成装填");
        tooltipLines.add(EnumChatFormatting.GRAY + "  手持大炮时会赋予使用者缓慢效果");
        tooltipLines.add("");
        tooltipLines.add(EnumChatFormatting.LIGHT_PURPLE + "发射炮弹：");
        tooltipLines.add(EnumChatFormatting.GRAY + "  发射装填完毕的骨髓，造成直线打击");
        tooltipLines.add(EnumChatFormatting.GRAY + "  爆炸伤害：" + (int) baseDmg);
        tooltipLines.add(EnumChatFormatting.GRAY + "  爆炸范围：半径 5 格");
        tooltipLines.add(EnumChatFormatting.GRAY + "  附加效果：对所有爆炸范围内的敌人施加一层【骨折】");
    }
}
