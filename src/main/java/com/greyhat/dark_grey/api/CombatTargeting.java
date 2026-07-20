package com.greyhat.dark_grey.api;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;

/** Shared multiplayer targeting rules for DarkGrey area attacks. */
public final class CombatTargeting {

    private CombatTargeting() {}

    public static boolean canDamage(EntityLivingBase attacker, EntityLivingBase target, boolean allowCreative) {
        if (target == null || target == attacker || target.isDead) {
            return false;
        }
        if (target instanceof EntityPlayer) {
            EntityPlayer targetPlayer = (EntityPlayer) target;
            if (targetPlayer.capabilities.isCreativeMode && !allowCreative) {
                return false;
            }
            if (attacker instanceof EntityPlayer) {
                MinecraftServer server = MinecraftServer.getServer();
                if (server != null && !server.isPVPEnabled()) {
                    return false;
                }
                Team attackerTeam = attacker.getTeam();
                if (attackerTeam != null && attackerTeam.isSameTeam(targetPlayer.getTeam())
                    && !attackerTeam.getAllowFriendlyFire()) {
                    return false;
                }
            }
        }
        return true;
    }
}
