//
// Decompiled by Procyon v0.6.0
//

package com.greyhat.dark_grey.api.capability;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.greyhat.dark_grey.api.IRPGComponent;

public interface IOnBowShoot extends IRPGComponent {

    boolean onBowShoot(final ItemStack p0, final World p1, final EntityPlayer p2, final int p3);
}
