package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.mark.MarkManager;
import com.greyhat.dark_grey.mark.MarkRegistry;
import com.greyhat.dark_grey.mark.api.MarkApplyContext;
import com.greyhat.dark_grey.mark.api.MarkApplyResult;

public class ComponentErebus implements IRPGComponent, IOnRightClick, IHasTooltip {

    private String markId = "poison";
    private int minRadius = 3;
    private int maxRadius = 7;
    private int radiusStep = 1;
    private float verticalHalfHeight = 3.0F;
    private boolean respectWalls = false;
    private int cooldownTicks = 20;
    private int rangeResetDelayTicks = 200;
    private int baseStacks = 1;
    private float twoStackChance = 0.50F;
    private float fiveStackChance = 0.25F;
    private boolean showRadiusMessage = true;
    private boolean showResetMessage = true;
    private boolean showAffectedTargetCount = true;

    @Override
    public String getComponentId() {
        return "厄瑞波斯";
    }

    @Override
    public void configure(JsonObject params) {
        if (params.has("markId")) markId = params.get("markId")
            .getAsString();
        if (params.has("minRadius")) minRadius = params.get("minRadius")
            .getAsInt();
        if (params.has("maxRadius")) maxRadius = params.get("maxRadius")
            .getAsInt();
        if (params.has("radiusStep")) radiusStep = params.get("radiusStep")
            .getAsInt();
        if (params.has("verticalHalfHeight")) verticalHalfHeight = params.get("verticalHalfHeight")
            .getAsFloat();
        if (params.has("respectWalls")) respectWalls = params.get("respectWalls")
            .getAsBoolean();
        if (params.has("cooldownTicks")) cooldownTicks = params.get("cooldownTicks")
            .getAsInt();
        if (params.has("rangeResetDelayTicks")) rangeResetDelayTicks = params.get("rangeResetDelayTicks")
            .getAsInt();
        if (params.has("baseStacks")) baseStacks = params.get("baseStacks")
            .getAsInt();
        if (params.has("twoStackChance")) twoStackChance = params.get("twoStackChance")
            .getAsFloat();
        if (params.has("fiveStackChance")) fiveStackChance = params.get("fiveStackChance")
            .getAsFloat();
        if (params.has("showRadiusMessage")) showRadiusMessage = params.get("showRadiusMessage")
            .getAsBoolean();
        if (params.has("showResetMessage")) showResetMessage = params.get("showResetMessage")
            .getAsBoolean();
        if (params.has("showAffectedTargetCount")) showAffectedTargetCount = params.get("showAffectedTargetCount")
            .getAsBoolean();

        if (twoStackChance + fiveStackChance > 1.0f) {
            DarkGrey.LOG.warn("ComponentErebus: twoStackChance + fiveStackChance > 1.0, resetting to defaults.");
            twoStackChance = 0.50f;
            fiveStackChance = 0.25f;
        }
        if (maxRadius < minRadius) {
            maxRadius = minRadius;
        }
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        float oneStackChance = 1.0f - twoStackChance - fiveStackChance;
        tooltip.add(net.minecraft.util.EnumChatFormatting.GOLD + "基础伤害：" + (int) 0);
        tooltip.add(net.minecraft.util.EnumChatFormatting.DARK_PURPLE + "高举法杖施放深渊诅咒，为周围一定范围内的目标施加剧毒印记");
        tooltip.add("");
        tooltip.add(net.minecraft.util.EnumChatFormatting.YELLOW + "右键：施放深渊诅咒");
        tooltip.add("");
        tooltip.add(net.minecraft.util.EnumChatFormatting.GREEN + "深渊诅咒：");
        tooltip.add(net.minecraft.util.EnumChatFormatting.GRAY + "  对周围的生物随机施加剧毒印记");
        tooltip.add(net.minecraft.util.EnumChatFormatting.GRAY + "  初始范围半径 " + minRadius + " 格");
        tooltip.add(
            net.minecraft.util.EnumChatFormatting.GRAY + "  连续施放扩大半径（每次 +" + radiusStep + " 格，最大 " + maxRadius + " 格）");
        tooltip.add(
            net.minecraft.util.EnumChatFormatting.GRAY + "  施放冷却："
                + String.format("%.1f", cooldownTicks / 20.0f)
                + " 秒");
        tooltip.add(
            net.minecraft.util.EnumChatFormatting.GRAY + "  "
                + String.format("%.1f", rangeResetDelayTicks / 20.0f)
                + " 秒不使用，半径重置为 "
                + minRadius
                + " 格");
        tooltip.add("");
        tooltip.add(net.minecraft.util.EnumChatFormatting.LIGHT_PURPLE + "诅咒层数：");
        tooltip.add(net.minecraft.util.EnumChatFormatting.GRAY + "  " + (int) (fiveStackChance * 100) + "% 概率施加 5 层");
        tooltip.add(net.minecraft.util.EnumChatFormatting.GRAY + "  " + (int) (twoStackChance * 100) + "% 概率施加 2 层");
        tooltip.add(net.minecraft.util.EnumChatFormatting.GRAY + "  " + (int) (oneStackChance * 100) + "% 概率施加 1 层");
    }

    @Override
    public ItemStack onRightClick(ItemStack weaponStack, World world, EntityPlayer player) {

        if (MarkRegistry.get(markId) == null) {
            if (!world.isRemote) {
                player.addChatMessage(new ChatComponentTranslation("message.erebus.mark_unavailable", markId));
            }
            return weaponStack;
        }

        NBTTagCompound entityData = player.getEntityData();
        long now = world.getTotalWorldTime();

        long cooldownEndTick = entityData.getLong("DarkGreyErebusCooldownEnd");
        if (now < cooldownEndTick) {
            long lastMsgTick = entityData.getLong("DarkGreyErebusLastMsgTick");
            if (now - lastMsgTick >= 20) {
                if (!world.isRemote) {
                    float remainingSeconds = (cooldownEndTick - now) / 20.0f;
                    player.addChatMessage(
                        new ChatComponentTranslation(
                            "message.erebus.cooldown",
                            String.format("%.1f", remainingSeconds)));
                }
                entityData.setLong("DarkGreyErebusLastMsgTick", now);
            }
            return weaponStack;
        }

        int currentRadius = entityData.hasKey("DarkGreyErebusNextRadius")
            ? entityData.getInteger("DarkGreyErebusNextRadius")
            : minRadius;

        // Random Stacks
        float roll = world.rand.nextFloat();
        int addedStacks = baseStacks;
        if (roll < fiveStackChance) {
            addedStacks = 5;
        } else if (roll < fiveStackChance + twoStackChance) {
            addedStacks = 2;
        }

        // Gather targets
        AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(
            player.posX - currentRadius,
            player.posY - verticalHalfHeight,
            player.posZ - currentRadius,
            player.posX + currentRadius,
            player.posY + verticalHalfHeight,
            player.posZ + currentRadius);

        @SuppressWarnings("unchecked")
        List<EntityLivingBase> candidates = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb);

        int affectedCount = 0;
        double playerCenterY = player.posY + player.height * 0.5D;
        java.util.List<Integer> hitEntities = new java.util.ArrayList<>();

        for (EntityLivingBase target : candidates) {
            if (target == player || target.isDead || target.getHealth() <= 0) continue;

            double dx = target.posX - player.posX;
            double dz = target.posZ - player.posZ;
            if (dx * dx + dz * dz > currentRadius * currentRadius) continue;

            double targetCenterY = target.posY + target.height * 0.5D;
            if (Math.abs(targetCenterY - playerCenterY) > verticalHalfHeight) continue;

            if (!CombatTargeting.canDamage(player, target, false)) continue;

            if (respectWalls) {
                Vec3 pVec = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
                Vec3 tVec = Vec3.createVectorHelper(target.posX, targetCenterY, target.posZ);
                MovingObjectPosition mop = world.rayTraceBlocks(pVec, tVec);
                if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    continue;
                }
            }

            if (!world.isRemote) {
                MarkApplyContext context = new MarkApplyContext.Builder().source(player)
                    .requestedStacks(addedStacks)
                    .worldTime(now)
                    .applicationId("erebus")
                    .refreshDuration(true)
                    .triggerImmediate(true)
                    .build();
                MarkApplyResult result = MarkManager.apply(target, markId, context);
                if (result != null && result.success) {
                    affectedCount++;
                    hitEntities.add(target.getEntityId());
                }
            }
        }

        if (!world.isRemote && !hitEntities.isEmpty()) {
            int[] ids = new int[hitEntities.size()];
            for (int i = 0; i < hitEntities.size(); i++) ids[i] = hitEntities.get(i);
            com.greyhat.dark_grey.DarkGrey.NETWORK.sendToAllAround(
                new com.greyhat.dark_grey.network.ErebusHitMessage(ids),
                new cpw.mods.fml.common.network.NetworkRegistry.TargetPoint(
                    player.dimension,
                    player.posX,
                    player.posY,
                    player.posZ,
                    64));
        }

        // Set state for next use
        entityData.setLong("DarkGreyErebusCooldownEnd", now + cooldownTicks);
        entityData.setLong("DarkGreyErebusResetAt", now + rangeResetDelayTicks);
        entityData.setBoolean("DarkGreyErebusRangeActive", true);

        int nextRadius = Math.min(maxRadius, currentRadius + radiusStep);
        entityData.setInteger("DarkGreyErebusNextRadius", nextRadius);

        // Messages
        if (!world.isRemote && showRadiusMessage) {
            player.addChatMessage(new ChatComponentTranslation("message.erebus.cast", currentRadius, addedStacks));
        }

        // Visual and Sound
        if (world.isRemote) {
            spawnRingParticles(world, player, currentRadius);
        }
        world.playSoundAtEntity(player, "ambient.cave.cave", 1.0F, 1.0F - (currentRadius / (float) maxRadius) * 0.4F);
        world.playSoundAtEntity(player, "random.fizz", 1.0F, 1.2F);

        return weaponStack;
    }

    private void spawnCurseParticle(World world, double x, double y, double z, int count) {
        for (int i = 0; i < count * 3; i++) {
            double offsetX = (world.rand.nextDouble() - 0.5);
            double offsetY = (world.rand.nextDouble() - 0.5);
            double offsetZ = (world.rand.nextDouble() - 0.5);
            // Dark purple
            world.spawnParticle(
                "mobSpell",
                x + offsetX,
                y + offsetY,
                z + offsetZ,
                0.5 + world.rand.nextDouble() * 0.4,
                0.0,
                0.7 + world.rand.nextDouble() * 0.3);
            world.spawnParticle("witchMagic", x + offsetX, y + offsetY, z + offsetZ, 0, 0, 0);
        }
        world.spawnParticle("largeexplode", x, y, z, 0, 0, 0);
    }

    private void spawnRingParticles(World world, EntityPlayer player, int radius) {
        int particleCount = Math.min(128, 40 + radius * 12);
        for (int i = 0; i < particleCount; i++) {
            double angle = 2.0D * Math.PI * i / particleCount;
            double px = player.posX + Math.cos(angle) * radius;
            double pz = player.posZ + Math.sin(angle) * radius;
            // Outer ring
            double py = player.posY - 0.8D;
            world.spawnParticle("mobSpell", px, py, pz, 0.6, 0.0, 0.9);
            world.spawnParticle("portal", px, py + 0.3, pz, 0, -1, 0);

            // Inner aura
            if (world.rand.nextFloat() < 0.5f) {
                double r2 = radius * world.rand.nextDouble();
                double px2 = player.posX + Math.cos(angle) * r2;
                double pz2 = player.posZ + Math.sin(angle) * r2;
                double py2 = player.posY - 0.5 + world.rand.nextDouble() * 0.4;
                world.spawnParticle("mobSpell", px2, py2, pz2, 0.2, 0.0, 0.4);
                world.spawnParticle("witchMagic", px2, py2, pz2, 0, 0, 0);
            }
        }
    }
}
