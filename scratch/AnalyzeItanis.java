import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;

/**
 * Analyze Itanis bow texture to understand its pixel structure:
 * - Find bounding box of non-transparent pixels
 * - Identify bowstring pixels (cyan/blue bright pixels)
 * - Identify bow body pixels (golden/dark pixels)
 * - Report dimensions and pixel coordinates of key features
 */
public class AnalyzeItanis {
    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File("src/main/resources/assets/dark_grey/textures/items/itanis.png"));
        int w = img.getWidth();
        int h = img.getHeight();
        System.out.println("Image size: " + w + "x" + h);

        // Find bounding box
        int minX = w, maxX = 0, minY = h, maxY = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = (img.getRGB(x, y) >> 24) & 0xff;
                if (a > 10) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        System.out.println("Bounding box: (" + minX + "," + minY + ") to (" + maxX + "," + maxY + ")");
        System.out.println("Content size: " + (maxX - minX + 1) + "x" + (maxY - minY + 1));

        // Identify bowstring pixels (high blue, high brightness, cyan-like)
        // The bowstring is the bright cyan/blue line
        int stringPixels = 0;
        int stringMinX = w, stringMaxX = 0, stringMinY = h, stringMaxY = 0;
        
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                int a = (argb >> 24) & 0xff;
                int r = (argb >> 16) & 0xff;
                int g = (argb >> 8) & 0xff;
                int b = argb & 0xff;
                
                if (a > 128 && isBowstringColor(r, g, b)) {
                    stringPixels++;
                    stringMinX = Math.min(stringMinX, x);
                    stringMaxX = Math.max(stringMaxX, x);
                    stringMinY = Math.min(stringMinY, y);
                    stringMaxY = Math.max(stringMaxY, y);
                }
            }
        }
        System.out.println("\nBowstring pixels: " + stringPixels);
        System.out.println("Bowstring bbox: (" + stringMinX + "," + stringMinY + ") to (" + stringMaxX + "," + stringMaxY + ")");
        
        // Sample bowstring pixels along diagonal to understand thickness
        System.out.println("\nBowstring cross-section samples:");
        // The string goes from upper-right area to lower-left area
        // Sample several rows to see thickness
        for (int y = stringMinY; y <= stringMaxY; y += (stringMaxY - stringMinY) / 10) {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            int firstX = -1, lastX = -1;
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                int a2 = (argb >> 24) & 0xff;
                int r2 = (argb >> 16) & 0xff;
                int g2 = (argb >> 8) & 0xff;
                int b2 = argb & 0xff;
                if (a2 > 128 && isBowstringColor(r2, g2, b2)) {
                    count++;
                    if (firstX == -1) firstX = x;
                    lastX = x;
                }
            }
            if (count > 0) {
                System.out.println("  Row " + y + ": string from x=" + firstX + " to x=" + lastX + " (width=" + (lastX - firstX + 1) + " pixels, count=" + count + ")");
            }
        }
        
        // Also analyze rainbow_bow for comparison
        System.out.println("\n=== Rainbow Bow Analysis ===");
        BufferedImage rb = ImageIO.read(new File("src/main/resources/assets/dark_grey/textures/items/rainbow_bow.png"));
        System.out.println("Rainbow bow size: " + rb.getWidth() + "x" + rb.getHeight());
        
        BufferedImage rbP0 = ImageIO.read(new File("src/main/resources/assets/dark_grey/textures/items/rainbow_bow_pulling_0.png"));
        BufferedImage rbP1 = ImageIO.read(new File("src/main/resources/assets/dark_grey/textures/items/rainbow_bow_pulling_1.png"));
        BufferedImage rbP2 = ImageIO.read(new File("src/main/resources/assets/dark_grey/textures/items/rainbow_bow_pulling_2.png"));
        
        System.out.println("\nComparing standby vs pulling frames (pixel diff analysis):");
        compareFrames("standby vs pull_0", rb, rbP0);
        compareFrames("standby vs pull_1", rb, rbP1);
        compareFrames("standby vs pull_2", rb, rbP2);
        compareFrames("pull_0 vs pull_1", rbP0, rbP1);
        compareFrames("pull_1 vs pull_2", rbP1, rbP2);
        
        // Also analyze law_of_cycles
        System.out.println("\n=== Law of Cycles Analysis ===");
        BufferedImage loc = ImageIO.read(new File("src/main/resources/assets/dark_grey/textures/items/law_of_cycles.png"));
        System.out.println("Law of Cycles size: " + loc.getWidth() + "x" + loc.getHeight());
        
        BufferedImage locP0 = ImageIO.read(new File("src/main/resources/assets/dark_grey/textures/items/law_of_cycles_pulling_0.png"));
        BufferedImage locP1 = ImageIO.read(new File("src/main/resources/assets/dark_grey/textures/items/law_of_cycles_pulling_1.png"));
        BufferedImage locP2 = ImageIO.read(new File("src/main/resources/assets/dark_grey/textures/items/law_of_cycles_pulling_2.png"));
        
        compareFrames("standby vs pull_0", loc, locP0);
        compareFrames("standby vs pull_1", loc, locP1);
        compareFrames("standby vs pull_2", loc, locP2);
        
        // Dump the actual pixel-by-pixel content of rainbow_bow standby and pulling frames
        System.out.println("\n=== Rainbow Bow Pixel Dump (non-transparent) ===");
        dumpPixels("rainbow_bow standby", rb);
        dumpPixels("rainbow_bow pull_0", rbP0);
        dumpPixels("rainbow_bow pull_1", rbP1);
        dumpPixels("rainbow_bow pull_2", rbP2);
    }
    
    static boolean isBowstringColor(int r, int g, int b) {
        // Cyan/bright blue: high blue, moderate-to-high green, relatively low red
        // or white-ish with blue tint
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float hue = hsb[0] * 360;
        float sat = hsb[1];
        float bri = hsb[2];
        // Cyan range: hue 160-220, saturation > 0.3, brightness > 0.4
        return (hue >= 160 && hue <= 220 && sat > 0.2 && bri > 0.4) 
            || (b > 150 && g > 150 && r < g && bri > 0.7); // bright cyan/white-blue
    }
    
    static void compareFrames(String label, BufferedImage a, BufferedImage b) {
        int w = Math.min(a.getWidth(), b.getWidth());
        int h = Math.min(a.getHeight(), b.getHeight());
        int diffCount = 0;
        int addedCount = 0;   // pixels in b but not a
        int removedCount = 0; // pixels in a but not b
        
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pa = a.getRGB(x, y);
                int pb = b.getRGB(x, y);
                int aa = (pa >> 24) & 0xff;
                int ab = (pb >> 24) & 0xff;
                
                if (pa != pb) {
                    diffCount++;
                    if (aa < 10 && ab > 10) addedCount++;
                    if (aa > 10 && ab < 10) removedCount++;
                }
            }
        }
        System.out.println("  " + label + ": " + diffCount + " different pixels, +" + addedCount + " added, -" + removedCount + " removed");
    }
    
    static void dumpPixels(String label, BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        System.out.println("--- " + label + " (" + w + "x" + h + ") ---");
        for (int y = 0; y < h; y++) {
            StringBuilder line = new StringBuilder();
            boolean hasContent = false;
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                int a = (argb >> 24) & 0xff;
                if (a > 10) {
                    hasContent = true;
                    int r = (argb >> 16) & 0xff;
                    int g = (argb >> 8) & 0xff;
                    int b = argb & 0xff;
                    line.append(String.format("(%d,%d)=#%02x%02x%02x ", x, y, r, g, b));
                }
            }
            if (hasContent) {
                System.out.println(line.toString().trim());
            }
        }
    }
}
