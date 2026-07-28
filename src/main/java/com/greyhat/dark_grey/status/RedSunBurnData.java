package com.greyhat.dark_grey.status;

import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

import com.greyhat.dark_grey.api.WorldTimeRebaseHelper;

public class RedSunBurnData implements IExtendedEntityProperties {

    public static final String PROP_NAME = "dark_grey:red_sun_burn";

    private final EntityLivingBase entity;
    private boolean active;
    private long expireWorldTime;
    private UUID sourceUuid;
    private int lastSelectedHotbarSlot;
    private long lastSwitchDamageTick;
    private float switchDamage = 10.0F;
    private float incomingDamageMultiplier = 1.20F;
    private boolean ignoreSwitchDamageHurtResistance = true;

    public RedSunBurnData(EntityLivingBase entity) {
        this.entity = entity;
        this.lastSelectedHotbarSlot = -1;
    }

    public static void register(EntityLivingBase entity) {
        entity.registerExtendedProperties(PROP_NAME, new RedSunBurnData(entity));
    }

    public static RedSunBurnData get(EntityLivingBase entity) {
        return (RedSunBurnData) entity.getExtendedProperties(PROP_NAME);
    }

    public static void apply(EntityLivingBase target, EntityLivingBase source, int durationTicks) {
        apply(target, source, durationTicks, 10.0F, 1.20F, true);
    }

    public static void apply(EntityLivingBase target, EntityLivingBase source, int durationTicks, float switchDamage,
        float incomingDamageMultiplier, boolean ignoreSwitchDamageHurtResistance) {
        RedSunBurnData data = get(target);
        if (data != null) {
            data.active = true;
            data.expireWorldTime = target.worldObj.getTotalWorldTime() + durationTicks;
            if (source != null) {
                data.sourceUuid = source.getUniqueID();
            }
            if (target instanceof EntityPlayer) {
                data.lastSelectedHotbarSlot = ((EntityPlayer) target).inventory.currentItem;
            }
            data.switchDamage = switchDamage;
            data.incomingDamageMultiplier = incomingDamageMultiplier;
            data.ignoreSwitchDamageHurtResistance = ignoreSwitchDamageHurtResistance;
        }
    }

    public boolean isActive(World world) {
        if (!active) return false;
        if (world.getTotalWorldTime() >= expireWorldTime) {
            clear();
            return false;
        }
        return true;
    }

    public void clear() {
        this.active = false;
        this.sourceUuid = null;
        this.expireWorldTime = 0;
    }

    public void rebaseWorldTimes(long delta) {
        if (!active || delta == 0L) return;
        expireWorldTime = WorldTimeRebaseHelper.shiftPositiveTimestamp(expireWorldTime, delta);
        lastSwitchDamageTick = WorldTimeRebaseHelper.shiftPositiveTimestamp(lastSwitchDamageTick, delta);
    }

    public UUID getSourceUuid() {
        return sourceUuid;
    }

    public int getLastSelectedHotbarSlot() {
        return lastSelectedHotbarSlot;
    }

    public void setLastSelectedHotbarSlot(int slot) {
        this.lastSelectedHotbarSlot = slot;
    }

    public long getLastSwitchDamageTick() {
        return lastSwitchDamageTick;
    }

    public void setLastSwitchDamageTick(long tick) {
        this.lastSwitchDamageTick = tick;
    }

    public float getSwitchDamage() {
        return this.switchDamage;
    }

    public float getIncomingDamageMultiplier() {
        return this.incomingDamageMultiplier;
    }

    public boolean isIgnoringSwitchDamageHurtResistance() {
        return this.ignoreSwitchDamageHurtResistance;
    }

    @Override
    public void saveNBTData(NBTTagCompound compound) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("Active", active);
        nbt.setLong("ExpireWorldTime", expireWorldTime);
        if (sourceUuid != null) {
            nbt.setLong("SourceUUIDMost", sourceUuid.getMostSignificantBits());
            nbt.setLong("SourceUUIDLeast", sourceUuid.getLeastSignificantBits());
        }
        nbt.setInteger("LastSlot", lastSelectedHotbarSlot);
        nbt.setFloat("SwitchDamage", this.switchDamage);
        nbt.setFloat("IncomingDamageMultiplier", this.incomingDamageMultiplier);
        nbt.setBoolean("IgnoreSwitchDamageHurtResistance", this.ignoreSwitchDamageHurtResistance);
        compound.setTag(PROP_NAME, nbt);
    }

    @Override
    public void loadNBTData(NBTTagCompound compound) {
        if (compound.hasKey(PROP_NAME)) {
            NBTTagCompound nbt = compound.getCompoundTag(PROP_NAME);
            active = nbt.getBoolean("Active");
            expireWorldTime = nbt.getLong("ExpireWorldTime");
            if (nbt.hasKey("SourceUUIDMost") && nbt.hasKey("SourceUUIDLeast")) {
                sourceUuid = new UUID(nbt.getLong("SourceUUIDMost"), nbt.getLong("SourceUUIDLeast"));
            }
            lastSelectedHotbarSlot = nbt.getInteger("LastSlot");
            if (nbt.hasKey("SwitchDamage")) this.switchDamage = nbt.getFloat("SwitchDamage");
            if (nbt.hasKey("IncomingDamageMultiplier")) {
                this.incomingDamageMultiplier = nbt.getFloat("IncomingDamageMultiplier");
            }
            if (nbt.hasKey("IgnoreSwitchDamageHurtResistance")) {
                this.ignoreSwitchDamageHurtResistance = nbt.getBoolean("IgnoreSwitchDamageHurtResistance");
            }
        }
    }

    @Override
    public void init(Entity entity, World world) {}
}
