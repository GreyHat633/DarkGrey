package com.greyhat.dark_grey.event;

import java.util.Iterator;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.world.ExplosionEvent;

import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.api.FriendlyFireExplosion;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Applies DarkGrey's shared PVP/team rules to opt-in explosions. */
public final class CombatExplosionHandler {

    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.explosion instanceof FriendlyFireExplosion)) {
            return;
        }

        FriendlyFireExplosion explosion = (FriendlyFireExplosion) event.explosion;
        EntityLivingBase owner = explosion.getOwner();
        Iterator<Entity> entities = event.getAffectedEntities()
            .iterator();
        while (entities.hasNext()) {
            Entity entity = entities.next();
            if (entity instanceof EntityLivingBase
                && (owner == null || !CombatTargeting.canDamage(owner, (EntityLivingBase) entity, false))) {
                entities.remove();
            }
        }
    }
}
