package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.api.CooldownHelper;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.api.RPGItemDataManager;
import com.greyhat.dark_grey.api.UndergroundSunOrbManager;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnLeftClick;
import com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick;
import com.greyhat.dark_grey.entity.EntityUndergroundSunOrb;

public class ComponentUndergroundSun
    implements IRPGComponent, IOnRightClick, IOnWeaponUsingTick, IOnPlayerStoppedUsing, IOnLeftClick, IHasTooltip {

    private static final String LEGACY_LAUNCH_COOLDOWN_KEY = "DarkGreyUndergroundSunLastLaunch";
    private static final String LAUNCH_COOLDOWN_END_MILLIS_KEY = "DarkGreyUndergroundSunLaunchCooldownEndMillis";
    private static final String NO_ORB_MESSAGE_END_MILLIS_KEY = "DarkGreyUndergroundSunNoOrbMessageEndMillis";

    private int chargeTicks = 40;
    private int maxStoredOrbs = 3;
    private float damageMultiplier = 5.0F;
    private float explosionRadius = 20.0F;
    private float explosionHalfHeight = 10.0F;
    private float projectileSpeed = 1.8F;
    private int projectileLifetime = 100;
    private int launchCooldownTicks = 5;
    private boolean ignoreHurtResistance = true;
    private boolean respectWalls = false;
    private float orbitRadius = 1.25F;
    private float orbitHeight = -4.0F;
    private float orbitSpeed = 2.0F;

    @Override
    public String getComponentId() {
        return "地底太阳";
    }

    @Override
    public void configure(JsonObject params) {
        if (params == null) return;
        if (params.has("chargeTicks")) chargeTicks = Math.max(
            20,
            Math.min(
                72000,
                params.get("chargeTicks")
                    .getAsInt()));
        if (params.has("maxStoredOrbs")) maxStoredOrbs = Math.max(
            1,
            Math.min(
                10,
                params.get("maxStoredOrbs")
                    .getAsInt()));
        if (params.has("damageMultiplier")) damageMultiplier = Math.max(
            0f,
            Math.min(
                100f,
                params.get("damageMultiplier")
                    .getAsFloat()));
        if (params.has("explosionRadius")) explosionRadius = Math.max(
            1f,
            Math.min(
                64f,
                params.get("explosionRadius")
                    .getAsFloat()));
        if (params.has("explosionHalfHeight")) explosionHalfHeight = Math.max(
            1f,
            Math.min(
                64f,
                params.get("explosionHalfHeight")
                    .getAsFloat()));
        if (params.has("projectileSpeed")) projectileSpeed = Math.max(
            0.1f,
            Math.min(
                10f,
                params.get("projectileSpeed")
                    .getAsFloat()));
        if (params.has("projectileLifetime")) projectileLifetime = Math.max(
            20,
            Math.min(
                1200,
                params.get("projectileLifetime")
                    .getAsInt()));
        if (params.has("launchCooldownTicks")) launchCooldownTicks = Math.max(
            1,
            Math.min(
                100,
                params.get("launchCooldownTicks")
                    .getAsInt()));
        if (params.has("ignoreHurtResistance")) ignoreHurtResistance = params.get("ignoreHurtResistance")
            .getAsBoolean();
        if (params.has("respectWalls")) respectWalls = params.get("respectWalls")
            .getAsBoolean();
        if (params.has("orbitRadius")) orbitRadius = Math.max(
            0.3f,
            Math.min(
                5f,
                params.get("orbitRadius")
                    .getAsFloat()));
        if (params.has("orbitHeight")) orbitHeight = Math.max(
            -5.0f,
            Math.min(
                5f,
                params.get("orbitHeight")
                    .getAsFloat()));
        if (params.has("orbitSpeed")) orbitSpeed = Math.max(
            0f,
            Math.min(
                20f,
                params.get("orbitSpeed")
                    .getAsFloat()));
    }

    private float getBaseDamage(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof IRPGItemContainer) {
            String id = ((IRPGItemContainer) stack.getItem()).getRpgItemId();
            RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance()
                .getConfig(id);
            if (config != null) {
                return config.damage;
            }
        }
        return 90.0F;
    }

    private NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    private void clearChargeState(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return;
        NBTTagCompound nbt = stack.getTagCompound();
        nbt.removeTag("UndergroundSunCharging");
        nbt.removeTag("UndergroundSunChargeCompleted");
        nbt.removeTag("UndergroundSunChargeStartTime");
    }

    @Override
    public ItemStack onRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (player == null || stack == null || !player.isEntityAlive()) return stack;
        if (player.getCurrentEquippedItem() != stack) return stack;
        if (player.isUsingItem()) return stack;

        if (!world.isRemote) {
            int currentOrbs = UndergroundSunOrbManager.countFollowingOrbs(player);
            if (currentOrbs >= maxStoredOrbs) {
                NBTTagCompound nbt = getOrCreateTag(stack);
                nbt.removeTag("UndergroundSunCharging");
                nbt.removeTag("UndergroundSunChargeCompleted");
                nbt.removeTag("UndergroundSunChargeStartTime");
                player.addChatMessage(
                    new ChatComponentText(
                        EnumChatFormatting.RED + "地底太阳：光球数量已达到上限 " + currentOrbs + "/" + maxStoredOrbs));
                return stack;
            }
        }

        NBTTagCompound nbt = getOrCreateTag(stack);
        nbt.setBoolean("UndergroundSunCharging", true);
        nbt.setBoolean("UndergroundSunChargeCompleted", false);
        nbt.setLong("UndergroundSunChargeStartTime", world.getTotalWorldTime());

        player.setItemInUse(stack, stack.getMaxItemUseDuration());
        return stack;
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
        int usedTicks = stack.getMaxItemUseDuration() - count;
        float progress = Math.min(1.0F, usedTicks / (float) chargeTicks);
        World world = player.worldObj;

        if (world.isRemote) {
            if (world.getTotalWorldTime() % 4 == 0) {
                double angle = world.rand.nextDouble() * Math.PI * 2.0;
                double r = 1.0;
                double px = player.posX + Math.cos(angle) * r;
                double pz = player.posZ + Math.sin(angle) * r;
                world.spawnParticle("flame", px, player.boundingBox.minY + player.height * 0.7, pz, 0, 0.05, 0);
            }
            if (world.getTotalWorldTime() % 10 == 0) {
                world.playSound(player.posX, player.posY, player.posZ, "random.fizz", 0.4F, 0.8F + progress, false);
            }
        } else {
            NBTTagCompound nbt = getOrCreateTag(stack);
            boolean completed = nbt.getBoolean("UndergroundSunChargeCompleted");

            if (usedTicks >= chargeTicks && !completed) {
                int currentOrbs = UndergroundSunOrbManager.countFollowingOrbs(player);
                if (currentOrbs < maxStoredOrbs) {
                    nbt.setBoolean("UndergroundSunChargeCompleted", true);

                    int slot = UndergroundSunOrbManager.findFreeSlot(player);
                    float damage = getBaseDamage(stack) * damageMultiplier;

                    EntityUndergroundSunOrb orb = null;
                    try {
                        orb = new EntityUndergroundSunOrb(world, player);
                        orb.setPosition(player.posX, player.posY + player.getEyeHeight(), player.posZ);
                        orb.setExplosionDamage(damage);
                        orb.setExplosionRadius(explosionRadius);
                        orb.setExplosionHalfHeight(explosionHalfHeight);
                        orb.setProjectileSpeed(projectileSpeed);
                        orb.setProjectileLifetime(projectileLifetime);
                        orb.setIgnoreHurtResistance(ignoreHurtResistance);
                        orb.setRespectWalls(respectWalls);
                        orb.setOrbitRadius(orbitRadius);
                        orb.setOrbitHeight(orbitHeight);
                        orb.setOrbitSpeed(orbitSpeed);
                        orb.setFormationSlot(slot);

                        if (world.spawnEntityInWorld(orb)) {
                            world.playSoundEffect(player.posX, player.posY, player.posZ, "random.orb", 1.0F, 1.5F);
                        } else {
                            orb.setDead();
                            player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "地底太阳：光球生成失败，请稍后重试"));
                            DarkGrey.LOG.error(
                                "Underground Sun orb spawn returned false for player {} in dimension {}",
                                player.getCommandSenderName(),
                                world.provider.dimensionId);
                        }
                    } catch (RuntimeException e) {
                        if (orb != null) orb.setDead();
                        player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "地底太阳：光球生成异常，已安全中止"));
                        DarkGrey.LOG.error(
                            "Underground Sun orb spawn failed for player " + player.getCommandSenderName()
                                + " in dimension "
                                + world.provider.dimensionId,
                            e);
                    } catch (LinkageError e) {
                        if (orb != null) orb.setDead();
                        player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "地底太阳：光球生成不兼容，已安全中止"));
                        DarkGrey.LOG.error(
                            "Underground Sun orb spawn linkage failure for player " + player.getCommandSenderName()
                                + " in dimension "
                                + world.provider.dimensionId,
                            e);
                    }
                } else {
                    nbt.setBoolean("UndergroundSunChargeCompleted", true);
                    player.addChatMessage(
                        new ChatComponentText(
                            EnumChatFormatting.RED + "地底太阳：光球数量已达到上限 " + currentOrbs + "/" + maxStoredOrbs));
                }
                clearChargeState(stack);
                player.clearItemInUse();
            }
        }
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int timeLeft) {
        clearChargeState(stack);
    }

    @Override
    public boolean onLeftClick(ItemStack stack, EntityPlayer player) {
        World world = player.worldObj;
        if (world.isRemote) return true;

        if (player.getCurrentEquippedItem() != stack || !player.isEntityAlive()) {
            return true;
        }

        NBTTagCompound nbt = getOrCreateTag(stack);
        if (nbt.getBoolean("UndergroundSunCharging") && player.isUsingItem()) {
            player.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "地底太阳正在蓄力"));
            return true;
        }

        NBTTagCompound playerData = player.getEntityData();
        long launchCooldownMillis = CooldownHelper.ticksToMillis(launchCooldownTicks);
        if (!CooldownHelper
            .isReady(playerData, LAUNCH_COOLDOWN_END_MILLIS_KEY, launchCooldownMillis, LEGACY_LAUNCH_COOLDOWN_KEY)) {
            return true;
        }

        EntityUndergroundSunOrb oldestOrb = UndergroundSunOrbManager.findOldestFollowingOrb(player);
        if (oldestOrb == null) {
            long messageCooldownMillis = CooldownHelper.ticksToMillis(20L);
            if (CooldownHelper.isReady(playerData, NO_ORB_MESSAGE_END_MILLIS_KEY, messageCooldownMillis)) {
                player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "地底太阳：当前没有可发射的光球"));
                CooldownHelper.start(playerData, NO_ORB_MESSAGE_END_MILLIS_KEY, messageCooldownMillis);
            }
            return true;
        }

        Vec3 look = player.getLookVec();
        if (look != null) {
            look = look.normalize();
            oldestOrb.launch(look);
            CooldownHelper
                .start(playerData, LAUNCH_COOLDOWN_END_MILLIS_KEY, launchCooldownMillis, LEGACY_LAUNCH_COOLDOWN_KEY);
        }
        return true;
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List tooltipLines, boolean showAdvanced) {
        float baseDmg = getBaseDamage(stack);
        tooltipLines.add(EnumChatFormatting.GOLD + "基础伤害：" + (int) baseDmg);
        tooltipLines.add(EnumChatFormatting.AQUA + "光球命中产生范围爆炸，不会破坏方块");
        tooltipLines.add("");
        tooltipLines.add(EnumChatFormatting.YELLOW + "左键：发射光球");
        tooltipLines.add(EnumChatFormatting.YELLOW + "长按右键：生成光球");
        tooltipLines.add("");
        tooltipLines.add(EnumChatFormatting.GREEN + "生成光球：");
        tooltipLines.add(EnumChatFormatting.GRAY + "  蓄力 " + (chargeTicks / 20) + " 秒生成一个地底太阳光球");
        tooltipLines.add(EnumChatFormatting.GRAY + "  光球环绕自身提供照明和保护");
        tooltipLines.add(EnumChatFormatting.GRAY + "  最多可同时储存 " + maxStoredOrbs + " 个光球");
        tooltipLines.add("");
        tooltipLines.add(EnumChatFormatting.LIGHT_PURPLE + "发射光球：");
        tooltipLines.add(EnumChatFormatting.GRAY + "  消耗最早生成的光球进行直线发射");
        tooltipLines.add(EnumChatFormatting.GRAY + "  爆炸伤害：" + (int) (baseDmg * damageMultiplier));
        tooltipLines.add(EnumChatFormatting.GRAY + "  爆炸范围：半径 " + (int) explosionRadius + " 格");
        if (ignoreHurtResistance) {
            tooltipLines.add(EnumChatFormatting.GRAY + "  伤害无视怪物的受伤冷却机制");
        }
    }
}
