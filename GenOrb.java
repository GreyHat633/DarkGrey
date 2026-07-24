import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GenOrb {
    public static void main(String[] args) throws Exception {
        createOrb(32, 14, new Color(255, 255, 255), new Color(255, 150, 0), "E:/Java/MinecraftMod/DarkGrey/src/main/resources/assets/dark_grey/textures/items/underground_sun.png");
        createOrb(64, 28, new Color(255, 255, 255), new Color(255, 100, 0), "E:/Java/MinecraftMod/DarkGrey/src/main/resources/assets/dark_grey/textures/entity/underground_sun_orb.png");
        System.out.println("Images generated");
    }

    private static void createOrb(int size, int glowRadius, Color centerColor, Color edgeColor, String filepath) throws Exception {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        int center = size / 2;

        // Draw glow
        for (int i = glowRadius; i > 0; i--) {
            double ratio = (double) i / glowRadius;
            int alpha = (int) (255 * Math.pow(1 - ratio, 2));
            if (alpha > 0) {
                int color = (alpha << 24) | (edgeColor.getRed() << 16) | (edgeColor.getGreen() << 8) | edgeColor.getBlue();
                fillCircle(img, center, center, i, color);
            }
        }

        // Draw core
        int coreRadius = (int) (glowRadius * 0.4);
        for (int i = coreRadius; i > 0; i--) {
            double ratio = (double) i / coreRadius;
            int alpha = (int) (255 * (1 - ratio));
            int r = (int) (centerColor.getRed() * (alpha / 255.0) + edgeColor.getRed() * ((255 - alpha) / 255.0));
            int g = (int) (centerColor.getGreen() * (alpha / 255.0) + edgeColor.getGreen() * ((255 - alpha) / 255.0));
            int b = (int) (centerColor.getBlue() * (alpha / 255.0) + edgeColor.getBlue() * ((255 - alpha) / 255.0));
            int color = (255 << 24) | (r << 16) | (g << 8) | b;
            fillCircle(img, center, center, i, color);
        }

        ImageIO.write(img, "png", new File(filepath));
    }

    private static void fillCircle(BufferedImage img, int cx, int cy, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                if (x * x + y * y <= radius * radius) {
                    if (cx + x >= 0 && cx + x < img.getWidth() && cy + y >= 0 && cy + y < img.getHeight()) {
                        img.setRGB(cx + x, cy + y, blend(img.getRGB(cx + x, cy + y), color));
                    }
                }
            }
        }
    }
    
    private static int blend(int bg, int fg) {
        int a1 = (bg >> 24) & 0xFF;
        int r1 = (bg >> 16) & 0xFF;
        int g1 = (bg >> 8) & 0xFF;
        int b1 = bg & 0xFF;
        
        int a2 = (fg >> 24) & 0xFF;
        int r2 = (fg >> 16) & 0xFF;
        int g2 = (fg >> 8) & 0xFF;
        int b2 = fg & 0xFF;
        
        float alpha1 = a1 / 255f;
        float alpha2 = a2 / 255f;
        float alpha = alpha2 + alpha1 * (1 - alpha2);
        
        if (alpha == 0) return 0;
        
        int r = (int) ((r2 * alpha2 + r1 * alpha1 * (1 - alpha2)) / alpha);
        int g = (int) ((g2 * alpha2 + g1 * alpha1 * (1 - alpha2)) / alpha);
        int b = (int) ((b2 * alpha2 + b1 * alpha1 * (1 - alpha2)) / alpha);
        int a = (int) (alpha * 255);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
