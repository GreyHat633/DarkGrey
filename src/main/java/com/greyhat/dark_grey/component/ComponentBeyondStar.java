package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.BeyondStarOrbitMath;
import com.greyhat.dark_grey.api.BeyondStarSatelliteManager;
import com.greyhat.dark_grey.api.CooldownHelper;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnHit;
import com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick;
import com.greyhat.dark_grey.entity.EntityBeyondStarSatellite;
import com.greyhat.dark_grey.mark.MarkManager;
import com.greyhat.dark_grey.mark.MarkRegistry;
import com.greyhat.dark_grey.mark.api.IMarkType;
import com.greyhat.dark_grey.mark.api.MarkApplyContext;
import com.greyhat.dark_grey.mark.type.ScorchMarkType;

public class ComponentBeyondStar
    implements IRPGComponent, IOnHit, IOnRightClick, IOnWeaponUsingTick, IOnPlayerStoppedUsing, IHasTooltip {

    private static final String LEGACY_COOLDOWN_END_TICK_KEY = "DarkGreyBeyondStarCooldownEnd";
    private static final String COOLDOWN_END_MILLIS_KEY = "DarkGreyBeyondStarCooldownEndMillis";

    private int chargeTicks = 60;
    private double detonationRadius = 10.0;
    private double verticalHalfHeight = 5.0;
    private int detonationCount = 2;
    private int cooldownTicks = 160;
    private int satellitesPerTarget = 2;
    private int maxSatellites = 8;
    private float satelliteDamageMultiplier = 0.25F;
    private float satelliteSpeedBonus = 0.20F;
    private float satelliteProjectileSpeed = 1.6F;
    private float satelliteTurnRate = 0.40F;
    private int satelliteLifetimeTicks = 160;

    @Override
    public String getComponentId() {
        return "彼方之星";
    }

    @Override
    public void configure(JsonObject params) {
        if (params.has("chargeTicks")) chargeTicks = params.get("chargeTicks")
            .getAsInt();
        if (params.has("detonationRadius")) detonationRadius = params.get("detonationRadius")
            .getAsDouble();
        if (params.has("verticalHalfHeight")) verticalHalfHeight = params.get("verticalHalfHeight")
            .getAsDouble();
        if (params.has("detonationCount")) detonationCount = params.get("detonationCount")
            .getAsInt();
        if (params.has("cooldownTicks")) cooldownTicks = params.get("cooldownTicks")
            .getAsInt();
        if (params.has("satellitesPerTarget")) satellitesPerTarget = params.get("satellitesPerTarget")
            .getAsInt();
        if (params.has("maxSatellites")) maxSatellites = params.get("maxSatellites")
            .getAsInt();
        if (params.has("satelliteDamageMultiplier")) satelliteDamageMultiplier = params.get("satelliteDamageMultiplier")
            .getAsFloat();
        if (params.has("satelliteSpeedBonus")) satelliteSpeedBonus = params.get("satelliteSpeedBonus")
            .getAsFloat();
        if (params.has("satelliteProjectileSpeed")) satelliteProjectileSpeed = params.get("satelliteProjectileSpeed")
            .getAsFloat();
        if (params.has("satelliteTurnRate")) satelliteTurnRate = params.get("satelliteTurnRate")
            .getAsFloat();
        if (params.has("satelliteLifetimeTicks")) satelliteLifetimeTicks = params.get("satelliteLifetimeTicks")
            .getAsInt();
    }

    @Override
    public void onHit(ItemStack weaponStack, EntityLivingBase attacker, EntityLivingBase target, float actualDamage) {
        if (!attacker.worldObj.isRemote) {
            if (attacker instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) attacker;
                int sats = BeyondStarSatelliteManager.getCount(player);
                if (sats > 0 && actualDamage > 0) {
                    BeyondStarSatelliteManager.consumeAll(player);
                    for (int i = 0; i < sats; i++) {
                        EntityBeyondStarSatellite sat = new EntityBeyondStarSatellite(
                            attacker.worldObj,
                            attacker,
                            target,
                            actualDamage * satelliteDamageMultiplier);
                        double[] pos = BeyondStarOrbitMath
                            .getOrbitPosition(attacker, i, sats, attacker.ticksExisted, 0, true);
                        sat.setPosition(pos[0], pos[1], pos[2]);

                        attacker.worldObj.spawnEntityInWorld(sat);
                    }
                }
            }

            if (actualDamage > 0) {
                MarkManager.apply(
                    target,
                    ScorchMarkType.ID,
                    new MarkApplyContext.Builder().source(attacker)
                        .requestedStacks(1)
                        .stableDurationTicks(200) // Default 10s
                        .worldTime(target.worldObj.getTotalWorldTime())
                        .build());
            }
        }
    }

    private long getRemainingCooldownMillis(EntityPlayer player) {
        return CooldownHelper.getRemainingMillis(
            player.getEntityData(),
            COOLDOWN_END_MILLIS_KEY,
            CooldownHelper.ticksToMillis(cooldownTicks),
            LEGACY_COOLDOWN_END_TICK_KEY);
    }

    private void startCooldown(EntityPlayer player) {
        CooldownHelper.start(
            player.getEntityData(),
            COOLDOWN_END_MILLIS_KEY,
            CooldownHelper.ticksToMillis(cooldownTicks),
            LEGACY_COOLDOWN_END_TICK_KEY);
    }

    @Override
    public ItemStack onRightClick(ItemStack itemStack, World world, EntityPlayer player) {
        if (!world.isRemote) {
            long remainingCooldownMillis = getRemainingCooldownMillis(player);
            if (remainingCooldownMillis > 0L) {
                double secs = remainingCooldownMillis / 1000.0D;
                player.addChatMessage(
                    new net.minecraft.util.ChatComponentText(
                        EnumChatFormatting.RED + String.format("技能冷却中：%.1f 秒", secs)));
                return itemStack;
            }
        }
        player.setItemInUse(itemStack, 72000);
        return itemStack;
    }

    @Override
    public void onUsingTick(ItemStack weaponStack, EntityPlayer player, int count) {
        int duration = 72000 - count;

        if (player.worldObj.isRemote) { // removed count % 2 == 0 to double the tick rate
            List<EntityLivingBase> targets = player.worldObj.getEntitiesWithinAABB(
                EntityLivingBase.class,
                player.boundingBox.expand(detonationRadius, verticalHalfHeight, detonationRadius));

            boolean spawned = false;
            for (EntityLivingBase target : targets) {
                if (target != player && MarkManager.has(target, ScorchMarkType.ID)) {
                    spawned = true;
                    for (int i = 0; i < 6; i++) { // Increased from 3 to 6
                        double tx = target.posX + (player.worldObj.rand.nextDouble() - 0.5) * target.width;
                        double tz = target.posZ + (player.worldObj.rand.nextDouble() - 0.5) * target.width;

                        // posY on client is eye level, so posY - 0.8 is roughly waist level
                        double targetY = player.posY - 0.8;
                        double dist = Math
                            .sqrt((tx - player.posX) * (tx - player.posX) + (tz - player.posZ) * (tz - player.posZ));

                        // Planar vortex height (slightly arched at the edges)
                        double vortexY = targetY + (dist * 0.15) + (player.worldObj.rand.nextDouble() - 0.5) * 0.2;

                        // Velocity towards player
                        double vx = player.posX - tx;
                        double vyVortex = targetY - vortexY;
                        double vz = player.posZ - tz;

                        // Vortex particles (flat plane) moving inwards with separate random offsets
                        double dxSmoke = (player.worldObj.rand.nextDouble() - 0.5) * 0.5;
                        double dzSmoke = (player.worldObj.rand.nextDouble() - 0.5) * 0.5;
                        player.worldObj.spawnParticle(
                            "largesmoke",
                            tx + dxSmoke,
                            vortexY,
                            tz + dzSmoke,
                            vx * 0.15,
                            vyVortex * 0.15,
                            vz * 0.15);

                        double dxFlame = (player.worldObj.rand.nextDouble() - 0.5) * 0.5;
                        double dzFlame = (player.worldObj.rand.nextDouble() - 0.5) * 0.5;
                        player.worldObj.spawnParticle(
                            "flame",
                            tx + dxFlame,
                            vortexY,
                            tz + dzFlame,
                            vx * 0.15,
                            vyVortex * 0.15,
                            vz * 0.15);

                        // Red/Black ambient spark dust
                        if (player.worldObj.rand.nextBoolean()) {
                            player.worldObj.spawnParticle("reddust", tx, vortexY, tz, 1.0D, 0.0D, 0.0D); // Red
                        } else {
                            player.worldObj.spawnParticle("reddust", tx, vortexY, tz, 0.001D, 0.001D, 0.001D); // Black
                        }

                        // Physical particles starting from the enemy's body and moving towards player's waist
                        double bodyY = target.posY + player.worldObj.rand.nextDouble() * target.height;
                        double vyBody = targetY - bodyY;

                        double dxSmokeBody = (player.worldObj.rand.nextDouble() - 0.5) * 0.3;
                        double dzSmokeBody = (player.worldObj.rand.nextDouble() - 0.5) * 0.3;
                        player.worldObj.spawnParticle(
                            "largesmoke",
                            tx + dxSmokeBody,
                            bodyY,
                            tz + dzSmokeBody,
                            vx * 0.1,
                            vyBody * 0.1,
                            vz * 0.1);

                        double dxFlameBody = (player.worldObj.rand.nextDouble() - 0.5) * 0.3;
                        double dzFlameBody = (player.worldObj.rand.nextDouble() - 0.5) * 0.3;
                        player.worldObj.spawnParticle(
                            "flame",
                            tx + dxFlameBody,
                            bodyY,
                            tz + dzFlameBody,
                            vx * 0.05,
                            vyBody * 0.05,
                            vz * 0.05);
                    }
                }
            }

            if (!spawned) {
                for (int i = 0; i < 6; i++) { // Increased ambient particles
                    double angle = player.worldObj.rand.nextDouble() * Math.PI * 2;
                    double radius = 1.0 + player.worldObj.rand.nextDouble() * 4.0;
                    double x = player.posX + Math.cos(angle) * radius;
                    double z = player.posZ + Math.sin(angle) * radius;

                    double targetY = player.posY - 0.8; // Waist level
                    double y = targetY + (radius * 0.15) + (player.worldObj.rand.nextDouble() - 0.5) * 0.2;

                    double vx = player.posX - x;
                    double vy = targetY - y;
                    double vz = player.posZ - z;

                    double dxSmoke = (player.worldObj.rand.nextDouble() - 0.5) * 0.5;
                    double dzSmoke = (player.worldObj.rand.nextDouble() - 0.5) * 0.5;
                    player.worldObj
                        .spawnParticle("largesmoke", x + dxSmoke, y, z + dzSmoke, vx * 0.15, vy * 0.15, vz * 0.15);

                    double dxFlame = (player.worldObj.rand.nextDouble() - 0.5) * 0.5;
                    double dzFlame = (player.worldObj.rand.nextDouble() - 0.5) * 0.5;
                    player.worldObj
                        .spawnParticle("flame", x + dxFlame, y, z + dzFlame, vx * 0.15, vy * 0.15, vz * 0.15);

                    if (player.worldObj.rand.nextBoolean()) {
                        player.worldObj.spawnParticle("reddust", x, y, z, 1.0D, 0.0D, 0.0D); // Red
                    } else {
                        player.worldObj.spawnParticle("reddust", x, y, z, 0.001D, 0.001D, 0.001D); // Black
                    }
                }
            }
        }

        if (duration == chargeTicks) {
            if (!player.worldObj.isRemote) {
                List<EntityLivingBase> targets = player.worldObj.getEntitiesWithinAABB(
                    EntityLivingBase.class,
                    player.boundingBox.expand(detonationRadius, verticalHalfHeight, detonationRadius));
                int totalDetonated = 0;

                IMarkType type = MarkRegistry.get(ScorchMarkType.ID);
                if (type instanceof ScorchMarkType) {
                    ScorchMarkType scorch = (ScorchMarkType) type;
                    for (EntityLivingBase target : targets) {
                        if (target != player && MarkManager.has(target, ScorchMarkType.ID)) {
                            totalDetonated++;
                            for (int i = 0; i < detonationCount; i++) {
                                scorch.detonate(target, player, true);
                            }
                            // Refresh duration as per plan
                            MarkManager
                                .consume(target, ScorchMarkType.ID, MarkManager.getStacks(target, ScorchMarkType.ID));
                            MarkManager.apply(
                                target,
                                ScorchMarkType.ID,
                                new MarkApplyContext.Builder().source(player)
                                    .requestedStacks(1)
                                    .stableDurationTicks(200)
                                    .worldTime(player.worldObj.getTotalWorldTime())
                                    .build());
                        }
                    }
                }

                if (totalDetonated > 0) {
                    int satsToAdd = totalDetonated * satellitesPerTarget;
                    BeyondStarSatelliteManager.addSatellites(player, satsToAdd);
                }

                player.worldObj.playSoundEffect(player.posX, player.posY, player.posZ, "mob.wither.shoot", 1.0F, 1.0F);
                startCooldown(player);
            }
            player.stopUsingItem();
        }
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int itemInUseCount) {
        // Nothing needed here, trigger is strictly on 3s mark.
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        tooltip.add(EnumChatFormatting.GOLD + "基础伤害：" + (int) 75);
        tooltip.add(EnumChatFormatting.AQUA + "所有攻击自动附带【灼痕】印记");
        tooltip.add("");

        tooltip.add(EnumChatFormatting.GREEN + "引爆技能（长按右键3秒）：");
        tooltip.add(EnumChatFormatting.GRAY + "  引爆半径 10 内所有目标的【灼痕】 2 次");
        tooltip.add(EnumChatFormatting.GRAY + "  引爆后自动重新施加【灼痕】");

        long remainingCooldownMillis = getRemainingCooldownMillis(player);
        if (remainingCooldownMillis > 0L) {
            double secs = remainingCooldownMillis / 1000.0D;
            tooltip.add(EnumChatFormatting.RED + String.format("  冷却时间：%.1f 秒", secs));
        } else {
            tooltip.add(EnumChatFormatting.DARK_GRAY + "  冷却时间：8 秒");
        }

        tooltip.add("");
        tooltip.add(EnumChatFormatting.LIGHT_PURPLE + "爆炸环绕卫星：");
        tooltip.add(EnumChatFormatting.GRAY + "  每次引爆时，每命中一个目标生成 2 颗卫星");
        tooltip.add(EnumChatFormatting.GRAY + "  每颗卫星伤害为武器攻击力的 25%");
        tooltip.add(EnumChatFormatting.GRAY + "  卫星存在期间，提升 20% 移动速度");
        tooltip.add(EnumChatFormatting.DARK_GRAY + "  最多存在 8 颗卫星");

        tooltip.add("");
        tooltip.add(EnumChatFormatting.YELLOW + "左键攻击：");
        tooltip.add(EnumChatFormatting.GRAY + "  将所有卫星一起发射出去追踪目标！");
    }
}
