package com.greyhat.dark_grey.mark;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.common.util.Constants;

import com.greyhat.dark_grey.DarkGrey;

public class MarkContainer implements IExtendedEntityProperties {

    public static final String PROP_NAME = "dark_grey:marks";

    private EntityLivingBase owner;
    private final Map<String, MarkInstance> activeMarks = new LinkedHashMap<>();

    public static MarkContainer get(EntityLivingBase entity) {
        if (entity == null) return null;
        return (MarkContainer) entity.getExtendedProperties(PROP_NAME);
    }

    public EntityLivingBase getOwner() {
        return owner;
    }

    public MarkInstance getMark(String markId) {
        if (markId == null) return null;
        return activeMarks.get(markId.toLowerCase());
    }

    public boolean hasMark(String markId) {
        if (markId == null) return false;
        return activeMarks.containsKey(markId.toLowerCase());
    }

    public Collection<MarkInstance> getAllMarks() {
        return activeMarks.values();
    }

    public boolean isEmpty() {
        return activeMarks.isEmpty();
    }

    public void put(MarkInstance instance) {
        if (instance != null && instance.getMarkId() != null) {
            activeMarks.put(
                instance.getMarkId()
                    .toLowerCase(),
                instance);
        }
    }

    public MarkInstance remove(String markId) {
        if (markId == null) return null;
        return activeMarks.remove(markId.toLowerCase());
    }

    public void clear() {
        activeMarks.clear();
    }

    public int size() {
        return activeMarks.size();
    }

    @Override
    public void saveNBTData(NBTTagCompound compound) {
        NBTTagCompound marksNbt = new NBTTagCompound();
        NBTTagList list = new NBTTagList();

        for (MarkInstance instance : activeMarks.values()) {
            if (instance.getStacks() <= 0) continue;

            try {
                NBTTagCompound instanceTag = new NBTTagCompound();
                instance.writeToNBT(instanceTag);
                list.appendTag(instanceTag);
            } catch (Exception e) {
                DarkGrey.LOG.error("Failed to save mark " + instance.getMarkId(), e);
            }
        }

        marksNbt.setTag("Marks", list);
        compound.setTag("DarkGreyMarks", marksNbt);
    }

    @Override
    public void loadNBTData(NBTTagCompound compound) {
        if (!compound.hasKey("DarkGreyMarks")) {
            return;
        }
        NBTTagCompound marksNbt = compound.getCompoundTag("DarkGreyMarks");
        if (marksNbt.hasKey("Marks", Constants.NBT.TAG_LIST)) {
            NBTTagList list = marksNbt.getTagList("Marks", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) {
                try {
                    NBTTagCompound instanceTag = list.getCompoundTagAt(i);
                    MarkInstance instance = MarkInstance.readFromNBT(instanceTag);
                    if (instance != null && instance.getStacks() > 0) {
                        if (MarkRegistry.contains(instance.getMarkId())) {
                            activeMarks.put(
                                instance.getMarkId()
                                    .toLowerCase(),
                                instance);
                        } else {
                            DarkGrey.LOG.warn("Skipping unknown mark id: " + instance.getMarkId());
                        }
                    }
                } catch (Exception e) {
                    DarkGrey.LOG.error("Failed to load a mark instance", e);
                }
            }
        }
    }

    @Override
    public void init(Entity entity, World world) {
        if (entity instanceof EntityLivingBase) {
            this.owner = (EntityLivingBase) entity;
        }
    }
}
