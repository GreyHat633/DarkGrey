package com.greyhat.dark_grey.event;

import java.util.WeakHashMap;

import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;

public class ArrowTracker {

    private static final WeakHashMap<EntityArrow, ItemStack> ARROW_BOWS = new WeakHashMap<>();

    public static synchronized void registerArrow(EntityArrow arrow, ItemStack bow) {
        ARROW_BOWS.put(arrow, bow);
    }

    public static synchronized ItemStack getBow(EntityArrow arrow) {
        return ARROW_BOWS.get(arrow);
    }
}
