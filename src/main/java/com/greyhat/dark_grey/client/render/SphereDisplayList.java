package com.greyhat.dark_grey.client.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Sphere;

/**
 * Reuses the exact 32x32 GLU sphere mesh instead of rebuilding its vertices for
 * every planet, projectile, layer, and rendered frame.
 */
public final class SphereDisplayList {

    private static int displayList;
    private static Sphere fallbackSphere;

    private SphereDisplayList() {}

    public static void draw(float radius) {
        ensureDisplayList();
        GL11.glPushMatrix();
        GL11.glScalef(radius, radius, radius);
        if (displayList != 0) {
            GL11.glCallList(displayList);
        } else {
            fallbackSphere.draw(1.0F, 32, 32);
        }
        GL11.glPopMatrix();
    }

    private static void ensureDisplayList() {
        if (displayList != 0 || fallbackSphere != null) {
            return;
        }
        fallbackSphere = new Sphere();
        fallbackSphere.setDrawStyle(GLU.GLU_FILL);
        fallbackSphere.setNormals(GLU.GLU_SMOOTH);
        fallbackSphere.setTextureFlag(true);

        int generated = GL11.glGenLists(1);
        if (generated == 0) {
            return;
        }
        GL11.glNewList(generated, GL11.GL_COMPILE);
        fallbackSphere.draw(1.0F, 32, 32);
        GL11.glEndList();
        displayList = generated;
    }
}
