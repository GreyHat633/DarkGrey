package com.greyhat.dark_grey.api;

import java.util.Iterator;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.Vec3;
import net.minecraft.world.Explosion;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * A vanilla explosion whose affected-entity list can be filtered through
 * {@link CombatTargeting} before vanilla applies damage and knockback.
 */
public final class FriendlyFireExplosion extends Explosion {

    private final EntityLivingBase owner;

    private FriendlyFireExplosion(WorldServer world, EntityLivingBase owner, double x, double y, double z, float size) {
        super(world, owner, x, y, z, size);
        this.owner = owner;
        this.isFlaming = false;
        this.isSmoking = false;
    }

    public EntityLivingBase getOwner() {
        return this.owner;
    }

    /**
     * Mirrors {@link WorldServer#newExplosion} so clients receive the same
     * explosion packet, sound, particles, and allowed-target knockback.
     */
    public static void create(WorldServer world, EntityLivingBase owner, double x, double y, double z, float size) {
        FriendlyFireExplosion explosion = new FriendlyFireExplosion(world, owner, x, y, z, size);
        if (ForgeEventFactory.onExplosionStart(world, explosion)) {
            return;
        }

        explosion.doExplosionA();
        explosion.doExplosionB(false);
        explosion.affectedBlockPositions.clear();

        @SuppressWarnings("unchecked")
        Iterator<EntityPlayer> players = world.playerEntities.iterator();
        while (players.hasNext()) {
            EntityPlayer player = players.next();
            if (player.getDistanceSq(x, y, z) >= 4096.0D) {
                continue;
            }
            Vec3 knockback = (Vec3) explosion.func_77277_b()
                .get(player);
            ((EntityPlayerMP) player).playerNetServerHandler
                .sendPacket(new S27PacketExplosion(x, y, z, size, explosion.affectedBlockPositions, knockback));
        }
    }
}
