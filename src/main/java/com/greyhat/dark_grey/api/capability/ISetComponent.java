//
// Decompiled by Procyon v0.6.0
//

package com.greyhat.dark_grey.api.capability;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import com.greyhat.dark_grey.api.IRPGComponent;

public interface ISetComponent extends IRPGComponent {

    String getSetId();

    default String getComponentId() {
        return this.getSetId();
    }

    default float onSetHit(final EntityPlayer attacker, final EntityLivingBase target, final float rawDamage,
        final int pieceCount) {
        return rawDamage;
    }

    default void onSetTick(final EntityPlayer player, final int pieceCount) {}

    default void onSetKill(final EntityPlayer killer, final EntityLivingBase victim, final int pieceCount) {}
}
