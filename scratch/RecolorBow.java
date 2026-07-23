package scratch;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class RecolorBow {

    public static void main(String[] args) throws Exception {
        File itemsDir = new File("src/main/resources/assets/dark_grey/textures/items");

        recolorFile(
            new File(itemsDir, "law_of_cycles.png"),
            new File(itemsDir, "itanis.png")
        );
        recolorFile(
            new File(itemsDir, "law_of_cycles_pulling_0.png"),
            new File(itemsDir, "itanis_pulling_0.png")
        );
        recolorFile(
            new File(itemsDir, "law_of_cycles_pulling_1.png"),
            new File(itemsDir, "itanis_pulling_1.png")
        );
        recolorFile(
            new File(itemsDir, "law_of_cycles_pulling_2.png"),
            new File(itemsDir, "itanis_pulling_2.png")
        );

        System.out.println("Successfully recolored Law of Cycles bow texture into Itanis!");
    }

    private static void recolorFile(File srcFile, File destFile) throws Exception {
        if (!srcFile.exists()) {
            System.err.println("Source file does not exist: " + srcFile.getAbsolutePath());
            return;
        }

        BufferedImage src = ImageIO.read(srcFile);
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dest = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = src.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha < 10) {
                    dest.setRGB(x, y, 0);
                    continue;
                }

                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                float[] hsb = Color.RGBtoHSB(r, g, b, null);
                float hue = hsb[0];        // 0.0 to 1.0
                float sat = hsb[1];        // 0.0 to 1.0
                float bri = hsb[2];        // 0.0 to 1.0

                int newRGB;

                // Check for gold/yellow highlights or tip gems
                if (hue >= 0.08f && hue <= 0.18f && sat > 0.3f) {
                    // Keep gold/yellow accents bright gold
                    Color newColor = Color.getHSBColor(0.13f, 0.9f, Math.min(1.0f, bri * 1.1f));
                    newRGB = (alpha << 24) | (newColor.getRGB() & 0x00FFFFFF);
                } else if (bri > 0.85f && sat < 0.25f) {
                    // Bright white highlights -> Brilliant White
                    newRGB = (alpha << 24) | 0x00FFFFFF;
                } else if (bri < 0.35f) {
                    // Dark borders -> Deep Royal Blue
                    Color newColor = new Color((int)(10 * bri), (int)(40 * bri), (int)(140 * bri));
                    newRGB = (alpha << 24) | (newColor.getRGB() & 0x00FFFFFF);
                } else {
                    // Main pink/purple bow body -> Shift hue to Cyan / Light Blue (Hue ~ 0.52 to 0.56)
                    float newHue = 0.53f; // Cyan
                    float newSat = Math.min(1.0f, sat * 1.1f);
                    float newBri = Math.min(1.0f, bri * 1.1f);
                    Color newColor = Color.getHSBColor(newHue, newSat, newBri);
                    newRGB = (alpha << 24) | (newColor.getRGB() & 0x00FFFFFF);
                }

                dest.setRGB(x, y, newRGB);
            }
        }

        ImageIO.write(dest, "png", destFile);
        System.out.println("Generated: " + destFile.getName() + " (" + w + "x" + h + ")");
    }
}
