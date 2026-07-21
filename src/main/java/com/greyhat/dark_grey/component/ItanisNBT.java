package com.greyhat.dark_grey.component;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ItanisNBT {

    private static final String MODE_TAG = "ItanisMode";

    public static ItanisMode getMode(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return ItanisMode.RAPID;
        }
        int mode = stack.getTagCompound()
            .getInteger(MODE_TAG);
        return ItanisMode.fromInt(mode);
    }

    public static void setMode(ItemStack stack, ItanisMode mode) {
        if (stack == null) return;
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound()
            .setInteger(MODE_TAG, mode.ordinal());
    }

    public static void toggleMode(ItemStack stack) {
        ItanisMode current = getMode(stack);
        setMode(stack, current == ItanisMode.RAPID ? ItanisMode.CHARGE : ItanisMode.RAPID);
    }
}
