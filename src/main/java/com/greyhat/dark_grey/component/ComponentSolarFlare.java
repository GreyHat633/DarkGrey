package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick;
import com.greyhat.dark_grey.entity.EntityPhantomStrike;

public class ComponentSolarFlare
    implements IRPGComponent, IOnWeaponUsingTick, IOnPlayerStoppedUsing, IOnRightClick, IHasTooltip {

    private static final double CONTACT_OVERLAP = 0.002D;

    @Override
    public String getComponentId() {
        return "耀斑";
    }

    @Override
    public void configure(JsonObject params) {}

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List<String> tooltipLines, boolean advanced) {
        tooltipLines.add("§6[主动技能: 耀斑冲锋]");
        tooltipLines.add("§7长按右键进行冲锋突进，跨越地形障碍");
        tooltipLines.add("§7撞击敌人时自动触发残影贯穿，造成额外 §c600% §7伤害");
        tooltipLines.add("§7并为目标施加持续5秒的 [焦灼印记]");
        tooltipLines.add("§8(撞击墙壁会中断冲锋)");
        tooltipLines.add("");
        tooltipLines.add("§e[连击: 耀斑引爆]");
        tooltipLines.add("§7普通攻击命中带有印记的敌人时，将引爆印记");
        tooltipLines.add("§7造成周围范围性的毁灭伤害，并引燃目标");
    }

    @Override
    public ItemStack onRightClick(ItemStack weaponStack, World world, EntityPlayer player) {
        player.getEntityData()
            .setBoolean("SolarDashHasHit", false);
        return weaponStack;
    }

    @Override
    public void onUsingTick(ItemStack weaponStack, EntityPlayer player, int count) {
        // If we have already hit something, do not apply dash velocity, just let the player hold the item and rebound.
        if (player.getEntityData()
            .getBoolean("SolarDashHasHit")) {
            return;
        }

        int dashTick = weaponStack.getItem()
            .getMaxItemUseDuration(weaponStack) - count;

        player.stepHeight = 1.0F;
        player.moveForward = 0.0F;
        player.moveStrafing = 0.0F;

        Vec3 look = player.getLookVec();
        double progress = Math.min(1.0, dashTick / 100.0);

        // To prevent speed resetting to 0, we take the MAX of their current speed and the charge speed
        double currentHorizontalSpeed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
        double chargeSpeed = Math.max(currentHorizontalSpeed, 1.5 * progress);

        player.motionX = look.xCoord * chargeSpeed;
        player.motionZ = look.zCoord * chargeSpeed;

        // Calculate the actual horizontal velocity applied to the player.
        // This accounts for the player's pitch (looking down = low horizontal speed).
        double actualHorizontalSpeed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
        double speedThreshold = 0.6;

        World world = player.worldObj;

        // Spawn particles if speed threshold is met
        if (world.isRemote && actualHorizontalSpeed >= speedThreshold) {
            for (int i = 0; i < 6; i++) {
                // Offset particles FORWARD so they don't just trail behind due to player's high speed.
                // This makes them wrap around the player's vision in 1st person!
                double forwardOffset = world.rand.nextDouble() * 1.5; // [0.0, 1.5] blocks ahead
                double px = player.posX + (look.xCoord * forwardOffset) + (world.rand.nextDouble() - 0.5) * 1.5;
                // True waist/chest level in bounding box coordinates: 0.8 to 1.3
                double py = player.boundingBox.minY + 0.8 + (world.rand.nextDouble() * 0.5);
                double pz = player.posZ + (look.zCoord * forwardOffset) + (world.rand.nextDouble() - 0.5) * 1.5;
                world.spawnParticle("flame", px, py, pz, 0.0, 0.05, 0.0);
                if (world.rand.nextInt(3) == 0) {
                    world.spawnParticle("lava", px, py, pz, 0.0, 0.0, 0.0);
                }
            }
        }

        // Only actual body contact may trigger the impact. A wider swept box is used
        // only to clamp this tick's travel so high dash speed cannot tunnel through
        // the target; it must never fire damage, recoil or sounds by itself.
        if (actualHorizontalSpeed >= speedThreshold && !player.getEntityData()
            .getBoolean("SolarDashHasHit")) {
            AxisAlignedBB contactBox = player.boundingBox.expand(1.0E-4, 0.01, 1.0E-4);
            EntityLivingBase target = findNearestDashTarget(world, player, contactBox, look);

            if (target != null) {
                if (world.isRemote) {
                    // The previous implementation cleared horizontal velocity
                    // here until the server packet arrived. On a real network
                    // that created a visible pause between touching the target
                    // and the impact. Keep the dash visually continuous, while
                    // still leaving recoil, sounds and damage server-authoritative
                    // so protected targets cannot produce a client-only fake hit.
                    return;
                }

                // Capture exact movement direction BEFORE recoil
                Vec3 dashDir = Vec3.createVectorHelper(player.motionX, player.motionY, player.motionZ)
                    .normalize();

                // Get raw damage
                float rawDamage = 1.0F;
                if (player.getEntityAttribute(SharedMonsterAttributes.attackDamage) != null) {
                    rawDamage = (float) player.getEntityAttribute(SharedMonsterAttributes.attackDamage)
                        .getAttributeValue();
                }

                // Deal 600% damage
                float totalDamage = rawDamage * 6.0F;
                DamageSource source = DamageSource.causePlayerDamage(player)
                    .setMagicDamage();
                if (!target.attackEntityFrom(source, totalDamage)) {
                    // A protection rule or invulnerability rejected the hit.
                    // Keep moving and do not fabricate recoil, sounds or marks.
                    return;
                }

                player.getEntityData()
                    .setBoolean("SolarDashHasHit", true);

                // Recoil (bounce back)
                player.motionX = -look.xCoord * 0.8;
                player.motionY = 0.4;
                player.motionZ = -look.zCoord * 0.8;
                player.stepHeight = 0.5F;
                player.velocityChanged = true;

                // Broadcast from the stationary impact target. Binding the sound to
                // the recoiling player made the source jump away before clients
                // received it, which could make a valid entity hit sound silent.
                world.playSoundAtEntity(target, "random.explode", 1.2F, 0.9F);
                world.playSoundAtEntity(target, "mob.wither.shoot", 1.0F, 0.5F);

                if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                    com.greyhat.dark_grey.DarkGrey.NETWORK.sendTo(
                        new com.greyhat.dark_grey.network.SolarFlareImpactMessage(
                            player.motionX,
                            player.motionY,
                            player.motionZ),
                        (net.minecraft.entity.player.EntityPlayerMP) player);
                }

                // Apply Scorched Mark only after the hit was accepted.
                com.greyhat.dark_grey.event.ScorchedMarkTracker.mark(target, 100);

                // Knockback target
                target.addVelocity(look.xCoord * 1.5, 0.5, look.zCoord * 1.5);

                // Spawn Phantom Strike on server to actually deal damage
                EntityPhantomStrike phantom = new EntityPhantomStrike(world, player, totalDamage, dashDir);
                // The collision target already received the immediate 600% impact above.
                // Keep the phantom's piercing damage for enemies behind it without double-hitting this target.
                phantom.excludeInitialTarget(target);
                world.spawnEntityInWorld(phantom);
                return;
            }

            AxisAlignedBB sweptBox = player.boundingBox.addCoord(player.motionX, 0.0, player.motionZ)
                .expand(0.05, 0.10, 0.05);
            clampTravelToNearestContact(world, player, sweptBox);
        }

        // isCollidedHorizontally can remain true from a previous movement packet,
        // or be set by a side/step collision. Require a short dash grace period and
        // confirm that the player's body box is actually blocked in the current
        // forward direction before consuming the skill and playing rebound effects.
        if (dashTick >= 3 && player.isCollidedHorizontally
            && actualHorizontalSpeed >= speedThreshold
            && hasUnsteppableForwardObstacle(world, player, look)) {
            player.stepHeight = 0.5F;
            player.getEntityData()
                .setBoolean("SolarDashHasHit", true);

            player.motionX = -look.xCoord * 0.8;
            player.motionY = 0.4;
            player.motionZ = -look.zCoord * 0.8;

            player.playSound("random.anvil_land", 1.0F, 0.8F);
            player.playSound("mob.wither.shoot", 1.0F, 0.5F);
        }
    }

    private boolean hasUnsteppableForwardObstacle(World world, EntityPlayer player, Vec3 look) {
        double horizontalLength = Math.sqrt(look.xCoord * look.xCoord + look.zCoord * look.zCoord);
        if (horizontalLength < 1.0E-4) {
            return false;
        }

        double probeDistance = 0.45;
        double probeX = look.xCoord / horizontalLength * probeDistance;
        double probeZ = look.zCoord / horizontalLength * probeDistance;
        AxisAlignedBB probeBox = player.boundingBox.addCoord(probeX, 0.0, probeZ)
            .contract(0.05, 0.05, 0.05);
        if (world.func_147461_a(probeBox)
            .isEmpty()) {
            return false;
        }

        // A one-block obstacle is intentionally traversable during the dash. Only
        // treat it as a wall when the same forward body box is still blocked after
        // lifting it by the configured one-block step height. Blocks without a
        // collision box (vanilla tall grass, flowers, etc.) fail the first check.
        double stepClearance = Math.max(1.0D, player.stepHeight) + 0.05D;
        AxisAlignedBB steppedProbeBox = AxisAlignedBB.getBoundingBox(
            probeBox.minX,
            probeBox.minY + stepClearance,
            probeBox.minZ,
            probeBox.maxX,
            probeBox.maxY + stepClearance,
            probeBox.maxZ);
        return !world.func_147461_a(steppedProbeBox)
            .isEmpty();
    }

    private EntityLivingBase findNearestDashTarget(World world, EntityPlayer player, AxisAlignedBB searchBox,
        Vec3 look) {
        double horizontalLength = Math.sqrt(look.xCoord * look.xCoord + look.zCoord * look.zCoord);
        if (horizontalLength < 1.0E-4) {
            return null;
        }
        double directionX = look.xCoord / horizontalLength;
        double directionZ = look.zCoord / horizontalLength;

        @SuppressWarnings("unchecked")
        List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(player, searchBox);
        EntityLivingBase nearestTarget = null;
        double nearestProjection = Double.MAX_VALUE;
        for (Entity entity : entities) {
            if (!(entity instanceof EntityLivingBase) || !player.canEntityBeSeen(entity)) {
                continue;
            }
            EntityLivingBase candidate = (EntityLivingBase) entity;
            if (!com.greyhat.dark_grey.api.CombatTargeting.canDamage(player, candidate, false)) {
                continue;
            }

            double targetCenterX = (candidate.boundingBox.minX + candidate.boundingBox.maxX) * 0.5;
            double targetCenterZ = (candidate.boundingBox.minZ + candidate.boundingBox.maxZ) * 0.5;
            double projection = (targetCenterX - player.posX) * directionX + (targetCenterZ - player.posZ) * directionZ;
            if (projection < -0.10 || projection >= nearestProjection) {
                continue;
            }
            nearestTarget = candidate;
            nearestProjection = projection;
        }
        return nearestTarget;
    }

    private void clampTravelToNearestContact(World world, EntityPlayer player, AxisAlignedBB searchBox) {
        double horizontalSpeed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
        if (horizontalSpeed < 1.0E-4) {
            return;
        }

        double playerCenterX = (player.boundingBox.minX + player.boundingBox.maxX) * 0.5;
        double playerCenterY = (player.boundingBox.minY + player.boundingBox.maxY) * 0.5;
        double playerCenterZ = (player.boundingBox.minZ + player.boundingBox.maxZ) * 0.5;
        double playerHalfX = (player.boundingBox.maxX - player.boundingBox.minX) * 0.5;
        double playerHalfY = (player.boundingBox.maxY - player.boundingBox.minY) * 0.5;
        double playerHalfZ = (player.boundingBox.maxZ - player.boundingBox.minZ) * 0.5;
        Vec3 start = Vec3.createVectorHelper(playerCenterX, playerCenterY, playerCenterZ);
        Vec3 end = Vec3
            .createVectorHelper(playerCenterX + player.motionX, playerCenterY, playerCenterZ + player.motionZ);

        @SuppressWarnings("unchecked")
        List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(player, searchBox);
        double nearestTravel = horizontalSpeed;
        for (Entity entity : entities) {
            if (!(entity instanceof EntityLivingBase) || !player.canEntityBeSeen(entity)) {
                continue;
            }
            EntityLivingBase candidate = (EntityLivingBase) entity;
            if (!com.greyhat.dark_grey.api.CombatTargeting.canDamage(player, candidate, false)) {
                continue;
            }

            AxisAlignedBB expandedTarget = candidate.boundingBox.expand(playerHalfX, playerHalfY, playerHalfZ);
            net.minecraft.util.MovingObjectPosition intercept = expandedTarget.calculateIntercept(start, end);
            if (intercept == null || intercept.hitVec == null) {
                continue;
            }
            double travel = start.distanceTo(intercept.hitVec);
            if (travel >= 0.0 && travel < nearestTravel) {
                nearestTravel = travel;
            }
        }

        if (nearestTravel < horizontalSpeed) {
            // Move a couple of millimetres into the contact plane. The next
            // tick then observes a real AABB overlap instead of faking a hit
            // from the wider swept search box.
            double scale = Math.min(1.0D, (nearestTravel + CONTACT_OVERLAP) / horizontalSpeed);
            player.motionX *= scale;
            player.motionZ *= scale;
        }
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int timeLeft) {
        player.stepHeight = 0.5F;
        player.getEntityData()
            .setBoolean("SolarDashHasHit", false);
    }

}
