package com.greyhat.dark_grey.client.render;

import java.nio.FloatBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * Custom first-person and third-person renderer for the "? (GUN) weapon category.
 *
 * <p>
 * Uses the item's sprite icon for equipped rendering (same visual approach as
 * {@link RenderRPGBow}), but adds a subtle mechanical shake/vibration effect
 * when the player is in the "using" state (i.e. actively reloading the weapon).
 * </p>
 *
 * <p>
 * The shake effect is achieved by applying small random translations to the
 * GL modelview matrix each frame during the reload phase, simulating the
 * feeling of a heavy bone mechanism being engaged.
 * </p>
 */
public class RenderRPGGun implements IItemRenderer {

    private static final int EQUIPPED_TEXTURE_WIDTH = 32;
    private static final int EQUIPPED_TEXTURE_HEIGHT = 32;

    /** Optional equipped texture override; if null, uses the item sprite icon. */
    private final ResourceLocation equippedTex;

    /** If true, render from a standalone equipped PNG; otherwise use the atlas icon. */
    private final boolean useEquippedTexture;

    /**
     * Creates a gun renderer that uses the item's atlas sprite icon for rendering.
     */
    public RenderRPGGun() {
        this.equippedTex = null;
        this.useEquippedTexture = false;
    }

    /**
     * Creates a gun renderer with a specific equipped texture.
     *
     * @param equippedTextureName the texture name under {@code dark_grey:textures/items/}
     */
    public RenderRPGGun(String equippedTextureName) {
        this.equippedTex = new ResourceLocation("dark_grey", "textures/items/" + equippedTextureName + ".png");
        this.useEquippedTexture = true;
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return false;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        if (data.length < 2 || !(data[1] instanceof EntityLivingBase)) {
            return;
        }

        final EntityLivingBase entity = (EntityLivingBase) data[1];

        // Save any existing Forge transform matrix (same technique as RenderRPGBow)
        final boolean detachedFromForgeTransform = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH) > 1;
        FloatBuffer originalMatrix = null;
        if (detachedFromForgeTransform) {
            originalMatrix = BufferUtils.createFloatBuffer(16);
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, originalMatrix);
            GL11.glPopMatrix();
        }

        GL11.glPushMatrix();
        try {
            // Third-person adjustments
            if (type == ItemRenderType.EQUIPPED) {
                GL11.glRotatef(-20.0f, 0.0f, 0.0f, 1.0f);
                GL11.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
                GL11.glRotatef(-60.0f, 0.0f, 0.0f, 1.0f);
                GL11.glScalef(2.6666667f, 2.6666667f, 2.6666667f);
                GL11.glTranslatef(-0.25f, -0.1875f, 0.1875f);
                float f2 = 0.625f;
                GL11.glTranslatef(0.0f, 0.125f, 0.3125f);
                GL11.glRotatef(-20.0f, 0.0f, 1.0f, 0.0f);
                GL11.glScalef(f2, -f2, f2);
                GL11.glRotatef(-100.0f, 1.0f, 0.0f, 0.0f);
                GL11.glRotatef(45.0f, 0.0f, 1.0f, 0.0f);
            }

            if (type == ItemRenderType.EQUIPPED_FIRST_PERSON && entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                if (player.getItemInUse() != null && player.getItemInUse()
                    .getItem() == item.getItem()) {
                    // Cancel vanilla's doBlockTransform() for first-person so the gun doesn't block your face
                    GL11.glRotatef(-60.0F, 0.0F, 1.0F, 0.0F);
                    GL11.glRotatef(80.0F, 1.0F, 0.0F, 0.0F);
                    GL11.glRotatef(-30.0F, 0.0F, 1.0F, 0.0F);
                    GL11.glTranslatef(0.5F, -0.2F, 0.0F);
                }
            }

            // Base positioning (gun-like grip)
            GL11.glTranslatef(0.0f, -0.3f, 0.0f);
            GL11.glScalef(1.5f, 1.5f, 1.5f);
            GL11.glRotatef(50.0f, 0.0f, 1.0f, 0.0f);
            GL11.glRotatef(335.0f, 0.0f, 0.0f, 1.0f);
            GL11.glTranslatef(-0.9375f, -0.0625f, 0.0f);

            // Reload shake effect (first-person only)
            if (type == ItemRenderType.EQUIPPED_FIRST_PERSON && entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                if (player.getItemInUse() != null && player.getItemInUse()
                    .getItem() == item.getItem()) {
                    // Player is actively holding right-click (reloading).
                    // Apply a subtle, rapid mechanical vibration.
                    long tick = entity.worldObj.getTotalWorldTime();
                    float shakeIntensity = 0.012f;

                    // Use a combination of sine waves at different frequencies for organic shake
                    float shakeX = (float) (Math.sin(tick * 1.7) * shakeIntensity
                        + Math.sin(tick * 3.1) * shakeIntensity * 0.5f);
                    float shakeY = (float) (Math.sin(tick * 2.3 + 1.0) * shakeIntensity
                        + Math.cos(tick * 4.7) * shakeIntensity * 0.3f);
                    float shakeZ = (float) (Math.cos(tick * 1.9 + 2.0) * shakeIntensity * 0.4f);

                    GL11.glTranslatef(shakeX, shakeY, shakeZ);
                }
            }

            // Turn the extruded sprite around its true center so the muzzle points outward while preserving its top.
            if (type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
                GL11.glTranslatef(0.5f, 0.5f, -0.03125f);
                GL11.glRotatef(180.0f, 0.0f, 1.0f, 0.0f);
                GL11.glTranslatef(-0.5f, -0.5f, 0.03125f);
            }

            // Render the item sprite
            if (useEquippedTexture && equippedTex != null) {
                // Use standalone equipped texture
                Minecraft.getMinecraft()
                    .getTextureManager()
                    .bindTexture(equippedTex);
                GL11.glEnable(GL12.GL_RESCALE_NORMAL);
                Tessellator tessellator = Tessellator.instance;
                float thickness = 0.0625F;
                ItemRenderer.renderItemIn2D(
                    tessellator,
                    1.0F,
                    0.0F,
                    0.0F,
                    1.0F,
                    EQUIPPED_TEXTURE_WIDTH,
                    EQUIPPED_TEXTURE_HEIGHT,
                    thickness);
                GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            } else {
                // Use atlas icon (same approach as RenderRPGBow)
                IIcon icon = entity.getItemIcon(item, 0);
                if (icon != null) {
                    Minecraft.getMinecraft().renderEngine.bindTexture(
                        Minecraft.getMinecraft().renderEngine.getResourceLocation(item.getItemSpriteNumber()));
                    Tessellator tessellator = Tessellator.instance;
                    ItemRenderer.renderItemIn2D(
                        tessellator,
                        icon.getMaxU(),
                        icon.getMinV(),
                        icon.getMinU(),
                        icon.getMaxV(),
                        icon.getIconWidth(),
                        icon.getIconHeight(),
                        0.0625f);
                }
            }
        } finally {
            GL11.glPopMatrix();
            if (detachedFromForgeTransform) {
                GL11.glPushMatrix();
                originalMatrix.rewind();
                GL11.glLoadMatrix(originalMatrix);
            }
        }
    }
}
