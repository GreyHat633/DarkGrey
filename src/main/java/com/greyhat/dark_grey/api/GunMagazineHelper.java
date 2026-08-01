package com.greyhat.dark_grey.api;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class GunMagazineHelper {

    private static final String NBT_KEY = "DarkGreyGunLoadedAmmo";

    public static int getLoadedAmmo(ItemStack stack) {
        if (stack == null || stack.getTagCompound() == null) {
            return 0;
        }
        return stack.getTagCompound()
            .getInteger(NBT_KEY);
    }

    public static void setLoadedAmmo(ItemStack stack, int ammo) {
        if (stack == null) return;
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }
        nbt.setInteger(NBT_KEY, Math.max(0, ammo));
    }

    public static void addAmmo(ItemStack stack, int amount, int capacity) {
        int current = getLoadedAmmo(stack);
        int next = Math.min(capacity, current + amount);
        setLoadedAmmo(stack, next);
    }

    public static boolean consumeAmmo(ItemStack stack, int amount) {
        int current = getLoadedAmmo(stack);
        if (current >= amount) {
            setLoadedAmmo(stack, current - amount);
            return true;
        }
        return false;
    }

    public static void clampAmmo(ItemStack stack, int capacity) {
        int current = getLoadedAmmo(stack);
        if (current > capacity) {
            setLoadedAmmo(stack, capacity);
        }
    }
}
