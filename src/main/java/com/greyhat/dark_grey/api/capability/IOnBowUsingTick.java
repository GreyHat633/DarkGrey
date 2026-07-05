// 
// Decompiled by Procyon v0.6.0
// 

package com.greyhat.dark_grey.api.capability;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;

public interface IOnBowUsingTick
{
    void onBowUsingTick(final ItemStack p0, final World p1, final EntityPlayer p2, final int p3);
}
