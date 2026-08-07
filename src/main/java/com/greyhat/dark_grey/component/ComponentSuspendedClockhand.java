package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.RPGDamageSources;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnHeldTick;
import com.greyhat.dark_grey.api.capability.IOnHit;
import com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick;
import com.greyhat.dark_grey.api.death.AbsoluteDeathService;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ComponentSuspendedClockhand implements IRPGComponent, IOnHeldTick, IOnHit, IOnRightClick,
    IOnPlayerStoppedUsing, IHasTooltip, IOnWeaponUsingTick {

    public static final int MAX_SOUL_VALUE = 2048;
    private static final String SOUL_TAG = "SoulValue";

    @Override
    public String getComponentId() {
        return "倒悬";
    }

    @Override
    public void onHeldTick(ItemStack itemStack, World world, EntityPlayer player) {
        if (!itemStack.hasTagCompound()) {
            itemStack.setTagCompound(new NBTTagCompound());
            itemStack.getTagCompound()
                .setInteger(SOUL_TAG, 10);
        } else if (!itemStack.getTagCompound()
            .hasKey(SOUL_TAG)) {
                itemStack.getTagCompound()
                    .setInteger(SOUL_TAG, 10);
            }
    }

    @Override
    public void onHit(ItemStack weaponStack, EntityLivingBase attacker, EntityLivingBase target, float damage) {
        int soul = getSoulValue(weaponStack);
        if (soul <= 0) {
            weaponStack.stackSize = 0;
            if (attacker instanceof EntityPlayer) {
                ((EntityPlayer) attacker).inventory.markDirty();
            }
            attacker.worldObj.playSoundEffect(attacker.posX, attacker.posY, attacker.posZ, "random.glass", 1.0F, 1.0F);
            return;
        }

        soul -= 1;

        if (target.getHealth() <= 0.0F || target.isDead) {
            soul += 2;
        }

        if (soul < 0) {
            weaponStack.stackSize = 0;
            if (attacker instanceof EntityPlayer) {
                ((EntityPlayer) attacker).inventory.markDirty();
            }
            attacker.worldObj.playSoundEffect(attacker.posX, attacker.posY, attacker.posZ, "random.glass", 1.0F, 1.0F);
        } else {
            setSoulValue(weaponStack, soul);
        }
    }

    @Override
    public ItemStack onRightClick(ItemStack weaponStack, World world, EntityPlayer player) {
        // 彻底移除右键聊天框提示，只保留蓄力逻辑
        return weaponStack;
    }

    @Override
    public void onUsingTick(ItemStack weaponStack, EntityPlayer player, int count) {
        int charge = weaponStack.getItem()
            .getMaxItemUseDuration(weaponStack) - count;
        if (getSoulValue(weaponStack) >= MAX_SOUL_VALUE) {

            // 蓄力期间的粒子效果 (超量增加粒子密度)
            if (player.worldObj.isRemote) {
                double midY = player.boundingBox.minY + player.height / 2.0;

                // 初期：紫色传送门粒子环绕交织 (翻倍)
                for (int i = 0; i < 6; i++) {
                    double angle1 = (charge * 0.3) + (i * Math.PI * 2 / 6);
                    double px1 = player.posX + Math.cos(angle1) * 1.5;
                    double pz1 = player.posZ + Math.sin(angle1) * 1.5;
                    com.greyhat.dark_grey.DarkGrey.proxy.spawnParticle(
                        player.worldObj,
                        "portal",
                        px1,
                        midY + Math.sin(charge * 0.15) * 0.8,
                        pz1,
                        0,
                        0,
                        0);

                    double angle2 = -(charge * 0.3) + (i * Math.PI * 2 / 6);
                    double px2 = player.posX + Math.cos(angle2) * 2.0;
                    double pz2 = player.posZ + Math.sin(angle2) * 2.0;
                    com.greyhat.dark_grey.DarkGrey.proxy.spawnParticle(
                        player.worldObj,
                        "portal",
                        px2,
                        midY + Math.cos(charge * 0.15) * 0.5,
                        pz2,
                        0,
                        0,
                        0);
                }

                // 蓄力结束后的白色粒子 (附魔台符文)，仅在蓄力满后出现，数量翻1.5倍
                if (charge >= 100) {
                    for (int i = 0; i < 8; i++) {
                        double rx = player.posX + (player.worldObj.rand.nextDouble() - 0.5) * 4.0;
                        double ry = midY + (player.worldObj.rand.nextDouble() - 0.5) * 4.0;
                        double rz = player.posZ + (player.worldObj.rand.nextDouble() - 0.5) * 4.0;
                        com.greyhat.dark_grey.DarkGrey.proxy
                            .spawnParticle(player.worldObj, "enchantmenttable", rx, ry, rz, 0, 0.1, 0);
                    }
                }
            }

            if (charge == 100 && !player.worldObj.isRemote) { // 从 40 增加到 100 刻 (5秒)
                // 仅音效提示，不再用文字刷屏
                player.worldObj.playSoundEffect(player.posX, player.posY, player.posZ, "random.levelup", 1.0F, 2.0F);
            }
        }
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int timeLeft) {
        // Assume default max use duration is 72000 from ItemRPGWeapon
        int charge = 72000 - timeLeft;

        if (charge >= 100 && getSoulValue(stack) >= MAX_SOUL_VALUE) { // 必须满5秒
            if (!world.isRemote) {
                player
                    .addChatComponentMessage(new ChatComponentText(EnumChatFormatting.DARK_RED + "“对真理的求知即是与造物主的角力”"));

                List<Entity> entities = world
                    .getEntitiesWithinAABBExcludingEntity(player, player.boundingBox.expand(8.0D, 8.0D, 8.0D));
                DamageSource damageSource = RPGDamageSources.causeFoolsHangingDamage(player);
                for (Entity entity : entities) {
                    if (entity instanceof EntityLivingBase) {
                        EntityLivingBase target = (EntityLivingBase) entity;
                        if (CombatTargeting.canDamage(player, target, true)) {
                            if (target instanceof net.minecraft.entity.player.EntityPlayerMP) {
                                AbsoluteDeathService.requestAbsoluteDeath(
                                    (net.minecraft.entity.player.EntityPlayerMP) target,
                                    com.greyhat.dark_grey.api.death.AbsoluteDeathReason.FOOLS_HANGING);
                            } else {
                                target.attackEntityFrom(
                                    new com.greyhat.dark_grey.api.death.AbsoluteDeathSource(
                                        com.greyhat.dark_grey.api.death.AbsoluteDeathReason.FOOLS_HANGING,
                                        player),
                                    Float.MAX_VALUE);
                            }
                        }
                    }
                }

                setSoulValue(stack, 1);
            }

            world.playSoundEffect(player.posX, player.posY, player.posZ, "mob.wither.death", 1.0F, 1.0F);
            world.playSoundEffect(player.posX, player.posY, player.posZ, "mob.enderdragon.end", 1.0F, 0.5F);

            // 释放时的华丽粒子
            if (world.isRemote) {
                double midY = player.boundingBox.minY + player.height / 2.0; // 调整到玩家腰部位置
                for (int i = 0; i < 40; i++) {
                    double d0 = world.rand.nextGaussian() * 0.1D;
                    double d1 = world.rand.nextGaussian() * 0.1D;
                    double d2 = world.rand.nextGaussian() * 0.1D;
                    com.greyhat.dark_grey.DarkGrey.proxy.spawnParticle(
                        world,
                        "hugeexplosion",
                        player.posX + (world.rand.nextFloat() * 2.0F - 1.0F) * 4.0F,
                        midY + (world.rand.nextFloat() * 2.0F - 1.0F) * 2.0F,
                        player.posZ + (world.rand.nextFloat() * 2.0F - 1.0F) * 4.0F,
                        d0,
                        d1,
                        d2);
                }
                for (int i = 0; i < 100; i++) {
                    double d0 = world.rand.nextGaussian() * 0.5D;
                    double d1 = world.rand.nextGaussian() * 0.5D;
                    double d2 = world.rand.nextGaussian() * 0.5D;
                    com.greyhat.dark_grey.DarkGrey.proxy.spawnParticle(
                        world,
                        "flame",
                        player.posX + (world.rand.nextFloat() * 2.0F - 1.0F) * 3.0F,
                        midY + (world.rand.nextFloat() * 2.0F - 1.0F) * 3.0F,
                        player.posZ + (world.rand.nextFloat() * 2.0F - 1.0F) * 3.0F,
                        d0,
                        d1,
                        d2);
                    com.greyhat.dark_grey.DarkGrey.proxy.spawnParticle(
                        world,
                        "largeexplode",
                        player.posX + (world.rand.nextFloat() * 2.0F - 1.0F) * 3.0F,
                        midY + (world.rand.nextFloat() * 2.0F - 1.0F) * 2.0F,
                        player.posZ + (world.rand.nextFloat() * 2.0F - 1.0F) * 3.0F,
                        0,
                        0,
                        0);
                }
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void addTooltipLines(ItemStack itemStack, EntityPlayer player, List tooltipLines, boolean showAdvanced) {
        int soulValue = getSoulValue(itemStack);
        float currentBonusDamage = soulValue / 4.0F;

        tooltipLines.add(EnumChatFormatting.AQUA + "灵魂即倒悬时针的生命与锋芒");
        tooltipLines.add("");
        tooltipLines.add(EnumChatFormatting.YELLOW + "攻击：消耗 1 点灵魂");
        tooltipLines.add(EnumChatFormatting.YELLOW + "长按右键：蓄力愚者的倒悬");
        tooltipLines.add("");
        tooltipLines.add(EnumChatFormatting.GREEN + "灵魂刻度：");
        tooltipLines.add(EnumChatFormatting.GRAY + "  每 4 点灵魂转化为 1 点额外伤害");
        tooltipLines.add(EnumChatFormatting.GRAY + "  击杀目标恢复 2 点灵魂，最高 2048 点");
        tooltipLines.add(EnumChatFormatting.GRAY + "  灵魂归零后再次攻击，倒悬时针必定破碎");
        tooltipLines.add("");
        tooltipLines.add(EnumChatFormatting.LIGHT_PURPLE + "愚者的倒悬");
        tooltipLines.add(EnumChatFormatting.GRAY + "  长按 2048 灵魂时持续 5 秒释放");
        tooltipLines.add(EnumChatFormatting.GRAY + "  对周围 8 格内合法目标赋予绝对死亡");
        tooltipLines.add(EnumChatFormatting.GRAY + "  释放后灵魂降至 1 点");
        tooltipLines.add(EnumChatFormatting.DARK_RED + "  下一击若未能夺取灵魂，武器将濒临破碎");
        tooltipLines.add("");
        tooltipLines.add(
            EnumChatFormatting.WHITE + "当前灵魂："
                + EnumChatFormatting.AQUA
                + soulValue
                + EnumChatFormatting.GRAY
                + " / "
                + MAX_SOUL_VALUE);
        tooltipLines.add(
            EnumChatFormatting.WHITE + "当前额外伤害："
                + EnumChatFormatting.RED
                + "+"
                + String.format("%.1f", currentBonusDamage));
    }

    private int getSoulValue(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound()
            .hasKey(SOUL_TAG)) {
            return stack.getTagCompound()
                .getInteger(SOUL_TAG);
        }
        return 10;
    }

    private void setSoulValue(ItemStack stack, int value) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound()
            .setInteger(SOUL_TAG, value);
    }
}
