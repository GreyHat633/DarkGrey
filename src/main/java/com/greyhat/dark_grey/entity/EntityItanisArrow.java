package com.greyhat.dark_grey.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.api.RPGDamageSources;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class EntityItanisArrow extends EntityThrowable {

    public enum ArrowState {

        HOVERING(0),
        LAUNCHED(1),
        PIERCING(2);

        private final int id;

        ArrowState(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static ArrowState fromId(int id) {
            for (ArrowState state : values()) {
                if (state.id == id) return state;
            }
            return LAUNCHED;
        }
    }

    private static final int DW_STATE = 20;
    private static final int DW_SLOT = 21;
    private static final int DW_TARGET = 22;
    private static final int DW_OWNER = 23;

    private float customDamage = 300.0F;
    private int formationSlot = 0;
    private int formationTotal = 1;
    private long formationId = 0L;
    private int hoverRemainTicks = 100; // Retention time after stopping bow use
    private boolean isOwnerUsingBow = true;
    private final Set<Integer> hitEntityIds = new HashSet<>();
    private int launchedTicks = 0;

    public EntityItanisArrow(World world) {
        super(world);
        this.setSize(0.5F, 0.5F);
    }

    public EntityItanisArrow(World world, EntityLivingBase shooter, float damage, ArrowState state) {
        super(world, shooter);
        this.setSize(0.5F, 0.5F);
        this.customDamage = damage;
        this.setState(state);
        this.dataWatcher.updateObject(DW_OWNER, shooter.getEntityId());
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataWatcher.addObject(DW_STATE, (byte) ArrowState.LAUNCHED.getId());
        this.dataWatcher.addObject(DW_SLOT, (byte) 0);
        this.dataWatcher.addObject(DW_TARGET, 0);
        this.dataWatcher.addObject(DW_OWNER, 0);
    }

    public ArrowState getArrowState() {
        return ArrowState.fromId(this.dataWatcher.getWatchableObjectByte(DW_STATE));
    }

    public void setState(ArrowState state) {
        this.dataWatcher.updateObject(DW_STATE, (byte) state.getId());
    }

    public int getFormationSlot() {
        return this.dataWatcher.getWatchableObjectByte(DW_SLOT);
    }

    public void setFormationSlot(int slot) {
        this.formationSlot = slot;
        this.dataWatcher.updateObject(DW_SLOT, (byte) slot);
    }

    @Override
    public EntityLivingBase getThrower() {
        EntityLivingBase thrower = super.getThrower();
        if (thrower == null && this.worldObj.isRemote) {
            int ownerId = this.dataWatcher.getWatchableObjectInt(DW_OWNER);
            if (ownerId != 0) {
                Entity entity = this.worldObj.getEntityByID(ownerId);
                if (entity instanceof EntityLivingBase) {
                    return (EntityLivingBase) entity;
                }
            }
        }
        return thrower;
    }

    public int getTargetEntityId() {
        return this.dataWatcher.getWatchableObjectInt(DW_TARGET);
    }

    public void setTargetEntityId(int targetId) {
        this.dataWatcher.updateObject(DW_TARGET, targetId);
    }

    public float getCustomDamage() {
        return customDamage;
    }

    public void setCustomDamage(float customDamage) {
        this.customDamage = customDamage;
    }

    public void setFormationTotal(int total) {
        this.formationTotal = Math.max(1, total);
    }

    public void setFormationId(long formationId) {
        this.formationId = formationId;
    }

    public long getFormationId() {
        return formationId;
    }

    public void setOwnerUsingBow(boolean using) {
        this.isOwnerUsingBow = using;
    }

    @Override
    protected float getGravityVelocity() {
        return 0.0F; // No gravity for Itanis magic arrows
    }

    @Override
    public void onUpdate() {
        ArrowState state = getArrowState();

        if (!this.worldObj.isRemote) {
            if (state == ArrowState.HOVERING) {
                // Hovering arrows live until hoverRemainTicks depletes or owner disconnects. Failsafe 1200 ticks.
                if (this.ticksExisted > 1200) {
                    this.setDead();
                    return;
                }
            } else {
                // Launched arrows live for 200 ticks max
                this.launchedTicks++;
                if (this.launchedTicks > 200) {
                    this.setDead();
                    return;
                }
            }
        }

        if (state == ArrowState.HOVERING) {
            updateHoveringState();
        } else if (state == ArrowState.LAUNCHED || state == ArrowState.PIERCING) {
            updateHomingState(state);
            super.onUpdate();
        }

        if (this.worldObj.isRemote) {
            spawnVisualParticles(state);
        }
    }

    private void updateHoveringState() {
        EntityLivingBase owner = getThrower();
        if (owner == null || owner.isDead || owner.worldObj != this.worldObj) {
            if (!this.worldObj.isRemote) {
                this.setDead();
            }
            return;
        }

        if (!this.worldObj.isRemote) {
            boolean isOwnerCharging = false;
            if (owner instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) owner;
                ItemStack held = player.getCurrentEquippedItem();
                if (player.isUsingItem() && held != null && held.getItem() instanceof IRPGItemContainer) {
                    IRPGItemContainer container = (IRPGItemContainer) held.getItem();
                    if ("itanis".equals(container.getRpgItemId())) {
                        isOwnerCharging = true;
                    }
                }
            }

            // If player is NOT charging, decrement retention time and search for targets
            if (!isOwnerCharging) {
                this.hoverRemainTicks--;
                if (this.hoverRemainTicks <= 0) {
                    this.setDead();
                    return;
                }

                // Target search every 4 ticks
                if (this.ticksExisted % 4 == 0) {
                    EntityLivingBase target = searchTarget(owner, 32.0D);
                    if (target != null) {
                        setTargetEntityId(target.getEntityId());
                        setState(ArrowState.LAUNCHED);
                        this.worldObj.playSoundEffect(this.posX, this.posY, this.posZ, "random.bow", 0.8F, 1.5F);
                        return;
                    }
                }
            }

            // Calculate formation slot position relative to owner
            Vec3 look = owner.getLookVec();
            Vec3 horizLook = Vec3.createVectorHelper(look.xCoord, 0, look.zCoord);
            if (horizLook.lengthVector() < 0.01D) {
                horizLook = Vec3.createVectorHelper(0, 0, 1);
            } else {
                horizLook = horizLook.normalize();
            }

            Vec3 up = Vec3.createVectorHelper(0, 1, 0);
            Vec3 right = horizLook.crossProduct(up)
                .normalize();

            // Arc arrangement behind/above player's head/shoulders
            int total = Math.max(1, formationTotal);
            double arcStep = total > 1 ? 2.4D / (total - 1) : 0;
            double offsetR = -1.2D + formationSlot * arcStep; // Left to right arc [-1.2, +1.2]
            double offsetUp = 1.2D + Math.sin((formationSlot / (double) total) * Math.PI) * 0.4D; // Slight vertical curve
            double offsetBack = -0.4D;

            double targetX = owner.posX + right.xCoord * offsetR + horizLook.xCoord * offsetBack;
            double targetY = owner.posY + owner.getEyeHeight() + offsetUp;
            double targetZ = owner.posZ + right.zCoord * offsetR + horizLook.zCoord * offsetBack;

            // Smooth interpolation to target position
            this.posX += (targetX - this.posX) * 0.3D;
            this.posY += (targetY - this.posY) * 0.3D;
            this.posZ += (targetZ - this.posZ) * 0.3D;
            this.setPosition(this.posX, this.posY, this.posZ);
        }

        // Always sync rotation to match owner on BOTH client and server for hovering arrows
        // This prevents visual flipping when network syncs vanilla atan2 rotation
        this.rotationYaw = owner.rotationYaw;
        this.rotationPitch = owner.rotationPitch;
        this.prevRotationYaw = owner.prevRotationYaw;
        this.prevRotationPitch = owner.prevRotationPitch;
    }

    private void updateHomingState(ArrowState state) {
        if (this.worldObj.isRemote) return;

        EntityLivingBase target = null;
        int targetId = getTargetEntityId();
        if (targetId > 0) {
            Entity e = this.worldObj.getEntityByID(targetId);
            if (e instanceof EntityLivingBase && e.isEntityAlive()) {
                target = (EntityLivingBase) e;
            }
        }

        EntityLivingBase owner = getThrower();
        if (target == null && this.ticksExisted % 4 == 0 && owner != null) {
            target = searchTarget(owner, 32.0D);
            if (target != null) {
                setTargetEntityId(target.getEntityId());
            }
        }

        if (target != null) {
            double targetX = target.posX;
            double targetY = target.posY + target.height * 0.5D;
            double targetZ = target.posZ;

            Vec3 desiredDir = Vec3.createVectorHelper(targetX - this.posX, targetY - this.posY, targetZ - this.posZ);
            if (desiredDir.lengthVector() > 0.01D) {
                desiredDir = desiredDir.normalize();
                Vec3 currentDir = Vec3.createVectorHelper(this.motionX, this.motionY, this.motionZ);
                if (currentDir.lengthVector() < 0.01D) {
                    currentDir = desiredDir;
                } else {
                    currentDir = currentDir.normalize();
                }

                float turnRate = state == ArrowState.PIERCING ? 0.05F : 0.45F;
                double newX = currentDir.xCoord * (1.0F - turnRate) + desiredDir.xCoord * turnRate;
                double newY = currentDir.yCoord * (1.0F - turnRate) + desiredDir.yCoord * turnRate;
                double newZ = currentDir.zCoord * (1.0F - turnRate) + desiredDir.zCoord * turnRate;
                Vec3 newDir = Vec3.createVectorHelper(newX, newY, newZ)
                    .normalize();

                double speed = state == ArrowState.PIERCING ? 3.5D : 3.0D;
                this.motionX = newDir.xCoord * speed;
                this.motionY = newDir.yCoord * speed;
                this.motionZ = newDir.zCoord * speed;
            }
        }
    }

    private EntityLivingBase searchTarget(EntityLivingBase owner, double range) {
        AxisAlignedBB box = this.boundingBox.expand(range, range, range);
        @SuppressWarnings("unchecked")
        List<Entity> list = this.worldObj.getEntitiesWithinAABBExcludingEntity(owner, box);

        java.util.List<EntityLivingBase> validTargets = new java.util.ArrayList<EntityLivingBase>();

        for (Entity entity : list) {
            if (!(entity instanceof EntityLivingBase) || !entity.isEntityAlive()) continue;
            EntityLivingBase living = (EntityLivingBase) entity;

            if (!CombatTargeting.canDamage(owner, living, false)) continue;
            if (!owner.canEntityBeSeen(living)) continue;

            validTargets.add(living);
        }

        if (validTargets.isEmpty()) {
            return null;
        }

        java.util.Collections.sort(validTargets, new java.util.Comparator<EntityLivingBase>() {
            @Override
            public int compare(EntityLivingBase e1, EntityLivingBase e2) {
                double d1 = EntityItanisArrow.this.getDistanceSqToEntity(e1);
                double d2 = EntityItanisArrow.this.getDistanceSqToEntity(e2);
                return Double.compare(d1, d2);
            }
        });

        double minDistSq = this.getDistanceSqToEntity(validTargets.get(0));
        double minDist = Math.sqrt(minDistSq);
        double maxAllowedDist = minDist + 8.0D;
        double maxAllowedDistSq = maxAllowedDist * maxAllowedDist;

        java.util.List<EntityLivingBase> cluster = new java.util.ArrayList<EntityLivingBase>();
        for (EntityLivingBase living : validTargets) {
            if (this.getDistanceSqToEntity(living) <= maxAllowedDistSq) {
                cluster.add(living);
            } else {
                break;
            }
        }

        int index = Math.abs(this.getEntityId()) % cluster.size();
        return cluster.get(index);
    }

    @Override
    protected void onImpact(MovingObjectPosition mop) {
        if (this.worldObj.isRemote) return;

        ArrowState state = getArrowState();
        if (state == ArrowState.HOVERING) return;

        EntityLivingBase owner = getThrower();

        if (state == ArrowState.PIERCING) {
            if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                Entity hit = mop.entityHit;
                if (hit instanceof EntityLivingBase && !hitEntityIds.contains(hit.getEntityId())) {
                    EntityLivingBase living = (EntityLivingBase) hit;
                    if (owner == null || CombatTargeting.canDamage(owner, living, false)) {
                        hitEntityIds.add(hit.getEntityId());
                        DamageSource source = owner != null ? RPGDamageSources.causeArrowDamage(this, owner)
                            : DamageSource.causeThrownDamage(this, owner);
                        living.attackEntityFrom(source, customDamage);

                        this.worldObj.playSoundEffect(
                            living.posX,
                            living.posY,
                            living.posZ,
                            "random.successful_hit",
                            1.0F,
                            1.2F);
                        if (this.worldObj instanceof net.minecraft.world.WorldServer) {
                            ((net.minecraft.world.WorldServer) this.worldObj).func_147487_a(
                                "crit",
                                living.posX,
                                living.posY + living.height * 0.5D,
                                living.posZ,
                                12,
                                0.3,
                                0.3,
                                0.3,
                                0.2);
                        }
                    }
                }
                // Piercing arrow does NOT die on hitting entities
                return;
            } else if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                // Piercing arrow stops when hitting blocks
                this.worldObj.playSoundEffect(this.posX, this.posY, this.posZ, "random.break", 1.0F, 1.5F);
                this.setDead();
            }
        } else {
            // LAUNCHED state (Normal main & extra arrows, launch floating arrows)
            if (mop.entityHit != null && mop.entityHit instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) mop.entityHit;
                if (owner == null || CombatTargeting.canDamage(owner, living, false)) {
                    DamageSource source = owner != null ? RPGDamageSources.causeArrowDamage(this, owner)
                        : DamageSource.causeThrownDamage(this, owner);
                    living.attackEntityFrom(source, customDamage);
                }
            }
            this.worldObj.playSoundEffect(this.posX, this.posY, this.posZ, "random.pop", 0.8F, 1.4F);
            this.setDead();
        }
    }

    @SideOnly(Side.CLIENT)
    private void spawnVisualParticles(ArrowState state) {
        if (state == ArrowState.HOVERING) {
            // Gold reddust particle
            double px = this.posX + (this.rand.nextDouble() - 0.5D) * this.width * 2.0D;
            double py = this.posY + this.rand.nextDouble() * this.height - 0.25D;
            double pz = this.posZ + (this.rand.nextDouble() - 0.5D) * this.width * 2.0D;
            this.worldObj.spawnParticle("reddust", px, py, pz, 1.0D, 0.8D, 0.1D);
        } else if (state == ArrowState.LAUNCHED) {
            for (int i = 0; i < 2; i++) {
                double pX = this.lastTickPosX + (this.posX - this.lastTickPosX) * (i / 2.0D);
                double pY = this.lastTickPosY + (this.posY - this.lastTickPosY) * (i / 2.0D);
                double pZ = this.lastTickPosZ + (this.posZ - this.lastTickPosZ) * (i / 2.0D);
                this.worldObj.spawnParticle("reddust", pX, pY, pZ, 1.0D, 0.8D, 0.1D);
            }
        } else if (state == ArrowState.PIERCING) {
            for (int i = 0; i < 5; i++) {
                double fraction = i / 5.0D;
                double pX = this.lastTickPosX + (this.posX - this.lastTickPosX) * fraction
                    + (this.rand.nextDouble() - 0.5D) * 0.4D;
                double pY = this.lastTickPosY + (this.posY - this.lastTickPosY) * fraction
                    + (this.rand.nextDouble() - 0.5D) * 0.4D;
                double pZ = this.lastTickPosZ + (this.posZ - this.lastTickPosZ) * fraction
                    + (this.rand.nextDouble() - 0.5D) * 0.4D;
                this.worldObj.spawnParticle("reddust", pX, pY, pZ, 1.0D, 0.85D, 0.3D); // Golden radiant energy
            }
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setInteger("ArrowState", getArrowState().getId());
        nbt.setFloat("CustomDamage", customDamage);
        nbt.setInteger("FormationSlot", formationSlot);
        nbt.setInteger("FormationTotal", formationTotal);
        nbt.setLong("FormationId", formationId);
        nbt.setInteger("HoverRemainTicks", hoverRemainTicks);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        setState(ArrowState.fromId(nbt.getInteger("ArrowState")));
        this.customDamage = nbt.getFloat("CustomDamage");
        this.formationSlot = nbt.getInteger("FormationSlot");
        this.formationTotal = nbt.getInteger("FormationTotal");
        this.formationId = nbt.getLong("FormationId");
        this.hoverRemainTicks = nbt.getInteger("HoverRemainTicks");
    }
}
