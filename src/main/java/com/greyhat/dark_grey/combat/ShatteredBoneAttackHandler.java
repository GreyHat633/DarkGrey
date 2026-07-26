package com.greyhat.dark_grey.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import com.greyhat.dark_grey.mark.MarkContainer;
import com.greyhat.dark_grey.mark.MarkInstance;
import com.greyhat.dark_grey.mark.type.ShatteredBoneMarkType;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class ShatteredBoneAttackHandler {

    private static class MovementData {

        double lastX, lastZ;
        long nextDamageTick;

        MovementData(double x, double z) {
            this.lastX = x;
            this.lastZ = z;
            this.nextDamageTick = 0;
        }
    }

    private final Map<UUID, MovementData> tracking = new HashMap<>();

    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent event) {
        EntityLivingBase entity = event.entityLiving;
        World world = entity.worldObj;
        if (world.isRemote || entity.isDead) return;

        MarkContainer container = MarkContainer.get(entity);
        if (container == null || container.isEmpty()) {
            this.tracking.remove(entity.getUniqueID());
            return;
        }

        MarkInstance instance = container.getMark(ShatteredBoneMarkType.ID);
        if (instance == null || instance.getStacks() < 1) {
            this.tracking.remove(entity.getUniqueID());
            return;
        }

        UUID uuid = entity.getUniqueID();
        MovementData data = this.tracking.get(uuid);

        if (data == null) {
            this.tracking.put(uuid, new MovementData(entity.posX, entity.posZ));
            return;
        }

        double dx = entity.posX - data.lastX;
        double dz = entity.posZ - data.lastZ;
        double distSq = dx * dx + dz * dz;

        if (distSq > 10.0 * 10.0) {
            data.lastX = entity.posX;
            data.lastZ = entity.posZ;
            return;
        }

        if (distSq >= 0.01) {
            long now = world.getTotalWorldTime();
            if (now >= data.nextDamageTick) {
                entity.attackEntityFrom(ShatteredBoneDamageSources.causeMovementDamage(), 5.0F);
                data.nextDamageTick = now + 5;
            }
        }

        data.lastX = entity.posX;
        data.lastZ = entity.posZ;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.isCanceled() || event.entityLiving.worldObj.isRemote) return;
        EntityLivingBase target = event.entityLiving;
        if (!com.greyhat.dark_grey.util.DirectAttackClassifier.isDirectAttack(event.source)) return;
        if (!(event.source.getEntity() instanceof EntityLivingBase)) return;
        EntityLivingBase attacker = (EntityLivingBase) event.source.getEntity();

        MarkContainer container = MarkContainer.get(target);
        if (container == null || container.isEmpty()) return;

        MarkInstance instance = container.getMark(ShatteredBoneMarkType.ID);
        if (instance == null || instance.getStacks() < 1) return;

        if (com.greyhat.dark_grey.util.SplashRecursionGuard.isProcessingSplash()) return;

        IAttributeInstance attackDamageAttr = attacker.getEntityAttribute(SharedMonsterAttributes.attackDamage);
        float baseDamage = attackDamageAttr != null ? (float) attackDamageAttr.getAttributeValue() : 1.0F;
        float splashDamage = baseDamage * 2.25F;

        com.greyhat.dark_grey.util.SplashRecursionGuard.setProcessingSplash(true);
        try {
            if (target.worldObj instanceof net.minecraft.world.WorldServer) {
                net.minecraft.world.WorldServer ws = (net.minecraft.world.WorldServer) target.worldObj;
                net.minecraft.util.Vec3 lookVec = attacker.getLookVec();
                java.util.Random rand = ws.rand;

                // Spawn particles throughout the entire 5-block cone
                for (int i = 0; i < 150; i++) {
                    // Generate a vector within the 45-degree cone
                    double vx = lookVec.xCoord + (rand.nextDouble() - 0.5) * 0.8;
                    double vy = lookVec.yCoord + (rand.nextDouble() - 0.5) * 0.4;
                    double vz = lookVec.zCoord + (rand.nextDouble() - 0.5) * 0.8;
                    double len = Math.sqrt(vx * vx + vy * vy + vz * vz);
                    vx /= len;
                    vy /= len;
                    vz /= len;

                    // Pick a random distance up to 5 blocks
                    double dist = rand.nextDouble() * 5.0;

                    double px = target.posX + vx * dist;
                    double py = target.posY + target.height / 2.0F + vy * dist;
                    double pz = target.posZ + vz * dist;

                    // Give them a slight outward velocity so they look dynamic
                    double speed = 0.05 + rand.nextDouble() * 0.1;

                    String particleType = (i % 3 == 0) ? "crit" : "blockcrack_155_0";
                    ws.func_147487_a(particleType, px, py, pz, 0, vx * speed, vy * speed, vz * speed, 1.0D);
                }
            }
            double radius = 5.0;
            java.util.List<EntityLivingBase> list = target.worldObj
                .getEntitiesWithinAABB(EntityLivingBase.class, target.boundingBox.expand(radius, radius, radius));
            for (EntityLivingBase splashTarget : list) {
                if (splashTarget == target || splashTarget == attacker) continue;
                if (!splashTarget.isEntityAlive()) continue;

                double dx = splashTarget.posX - attacker.posX;
                double dz = splashTarget.posZ - attacker.posZ;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > radius || dist < 0.01) continue;

                net.minecraft.util.Vec3 lookVec = attacker.getLookVec();
                double dot = (dx / dist) * lookVec.xCoord + (dz / dist) * lookVec.zCoord;
                if (dot >= 0.9238) {
                    if (com.greyhat.dark_grey.api.CombatTargeting.canDamage(attacker, splashTarget, false)) {
                        splashTarget
                            .attackEntityFrom(ShatteredBoneDamageSources.causeSplashDamage(attacker), splashDamage);

                        if (splashTarget.worldObj instanceof net.minecraft.world.WorldServer) {
                            ((net.minecraft.world.WorldServer) splashTarget.worldObj).func_147487_a(
                                "crit",
                                splashTarget.posX,
                                splashTarget.posY + splashTarget.height / 2.0F,
                                splashTarget.posZ,
                                15,
                                0.3D,
                                0.3D,
                                0.3D,
                                0.1D);
                        }

                        com.greyhat.dark_grey.mark.api.MarkApplyContext context = new com.greyhat.dark_grey.mark.api.MarkApplyContext.Builder()
                            .source(attacker)
                            .requestedStacks(1)
                            .worldTime(splashTarget.worldObj.getTotalWorldTime())
                            .applicationId("shattered_bone_splash")
                            .durationTicks(60)
                            .build();
                        com.greyhat.dark_grey.mark.MarkManager.apply(splashTarget, ShatteredBoneMarkType.ID, context);
                    }
                }
            }
        } finally {
            com.greyhat.dark_grey.util.SplashRecursionGuard.setProcessingSplash(false);
        }
    }
}
