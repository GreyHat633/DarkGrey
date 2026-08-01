package com.greyhat.dark_grey.api;

import net.minecraft.entity.Entity;

public class BeyondStarOrbitMath {

    /**
     * @param center     The center entity (e.g. the player)
     * @param index      Which satellite this is (0 to totalCount - 1)
     * @param totalCount Total number of satellites
     * @param ticks      Ticks existed of the center entity
     * @param pt         Partial ticks
     * @return A double[] {x, y, z} relative to the center entity's feet, or absolute if you add center's pos.
     */
    public static double[] getOrbitPosition(Entity center, int index, int totalCount, float ticks, float pt,
        boolean absolute) {
        double radius = 0.8;
        double speed = 0.05;
        double angleOffset = (2 * Math.PI / totalCount) * index;
        double currentAngle = (ticks + pt) * speed + angleOffset;

        float yawDegrees = center.rotationYaw;
        if (center instanceof net.minecraft.entity.EntityLivingBase) {
            net.minecraft.entity.EntityLivingBase living = (net.minecraft.entity.EntityLivingBase) center;
            yawDegrees = living.prevRenderYawOffset + (living.renderYawOffset - living.prevRenderYawOffset) * pt;
        }

        double yaw = yawDegrees * Math.PI / 180.0;

        // Halo center behind head
        double haloDistance = 0.6;
        double haloCenterY = center.height * 0.85;

        double backX = Math.sin(yaw) * haloDistance;
        double backZ = -Math.cos(yaw) * haloDistance;

        // Vertical circle in local X-Y plane
        double localX = Math.cos(currentAngle) * radius;
        double localY = Math.sin(currentAngle) * radius;

        // Rotate localX to global X,Z based on body yaw
        double x = backX + localX * Math.cos(yaw);
        double y = haloCenterY + localY;
        double z = backZ + localX * Math.sin(yaw);

        if (absolute) {
            double cx = center.lastTickPosX + (center.posX - center.lastTickPosX) * pt;
            double cy = center.lastTickPosY + (center.posY - center.lastTickPosY) * pt;
            double cz = center.lastTickPosZ + (center.posZ - center.lastTickPosZ) * pt;

            // Normalize cy to feet level across client and server
            cy -= center.yOffset;

            return new double[] { cx + x, cy + y, cz + z };
        }

        return new double[] { x, y, z };
    }
}
