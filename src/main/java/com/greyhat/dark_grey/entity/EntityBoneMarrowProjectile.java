package com.greyhat.dark_grey.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.api.RPGDamageSources;
import com.greyhat.dark_grey.mark.MarkManager;

public class EntityBoneMarrowProjectile extends EntityThrowable {

    private float damage = 23.0f;
    private int fractureStableDurationTicks = 100;
    private int lifetime = 200;

    public EntityBoneMarrowProjectile(World world) {
        super(world);
    }

    public EntityBoneMarrowProjectile(World world, EntityLivingBase thrower, float damage,
        int fractureStableDurationTicks) {
        super(world, thrower);
        this.damage = damage;
        this.fractureStableDurationTicks = fractureStableDurationTicks;
    }

    public EntityBoneMarrowProjectile(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    protected float getGravityVelocity() {
        return 0.01F; // Very low gravity, almost straight line
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (this.ticksExisted > lifetime) {
            this.setDead();
            return;
        }

        // Stunning particle trail on client side
        if (this.worldObj.isRemote) {
            for (int i = 0; i < 4; i++) {
                double offsetX = (this.rand.nextDouble() - 0.5) * 0.3;
                double offsetY = (this.rand.nextDouble() - 0.5) * 0.3;
                double offsetZ = (this.rand.nextDouble() - 0.5) * 0.3;

                // Bone dust (blockcrack using bone block / quartz block colors)
                this.worldObj.spawnParticle(
                    "iconcrack_352",
                    this.posX + offsetX,
                    this.posY + offsetY,
                    this.posZ + offsetZ,
                    -this.motionX * 0.1,
                    -this.motionY * 0.1,
                    -this.motionZ * 0.1);

                // Magic trail
                this.worldObj
                    .spawnParticle("crit", this.posX + offsetX, this.posY + offsetY, this.posZ + offsetZ, 0, 0, 0);
            }
        }
    }

    @Override
    protected void onImpact(MovingObjectPosition mop) {
        if (!this.worldObj.isRemote) {
            EntityLivingBase thrower = this.getThrower();

            // Explosion radius (5.0 blocks, which matches the visual particles spread)
            double radius = 5.0;
            java.util.List<EntityLivingBase> targets = this.worldObj
                .getEntitiesWithinAABB(EntityLivingBase.class, this.boundingBox.expand(radius, radius, radius));

            for (EntityLivingBase target : targets) {
                if (target == thrower || target.getDistanceSqToEntity(this) > radius * radius) {
                    continue;
                }

                if (thrower != null && CombatTargeting.canDamage(thrower, target, false)) {
                    DamageSource source = RPGDamageSources.causeArrowDamage(this, thrower);
                    boolean damaged = target.attackEntityFrom(source, this.damage);
                    if (damaged) {
                        // Apply fracture mark
                        com.greyhat.dark_grey.mark.api.MarkApplyContext ctx = new com.greyhat.dark_grey.mark.api.MarkApplyContext.Builder()
                            .source(thrower)
                            .requestedStacks(1)
                            .worldTime(this.worldObj.getTotalWorldTime())
                            .stableDurationTicks(fractureStableDurationTicks)
                            .build();
                        MarkManager.apply(target, "fracture", ctx);

                        // Impact sound for each target hit
                        this.worldObj
                            .playSoundAtEntity(target, "random.break", 1.0F, 0.8F + this.rand.nextFloat() * 0.4F);
                    }
                }
            }

            // Explode particles and sound on hit
            this.worldObj.playSoundEffect(
                this.posX,
                this.posY,
                this.posZ,
                "random.explode",
                2.0F,
                0.8F + this.rand.nextFloat() * 0.4F);

            if (this.worldObj instanceof net.minecraft.world.WorldServer) {
                net.minecraft.world.WorldServer ws = (net.minecraft.world.WorldServer) this.worldObj;

                // Huge explosion core
                ws.func_147487_a("hugeexplosion", this.posX, this.posY, this.posZ, 2, 0.5, 0.5, 0.5, 0.0);

                // Large explosions surrounding
                ws.func_147487_a("largeexplode", this.posX, this.posY, this.posZ, 15, 1.5, 1.5, 1.5, 0.0);

                // Flames
                ws.func_147487_a("flame", this.posX, this.posY, this.posZ, 50, 1.5, 1.5, 1.5, 0.1);

                // Bone fragments
                ws.func_147487_a("iconcrack_352", this.posX, this.posY, this.posZ, 30, 1.0, 1.0, 1.0, 0.2);
            }

            this.setDead();
        }
    }
}
