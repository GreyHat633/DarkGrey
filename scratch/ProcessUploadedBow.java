package scratch;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ProcessUploadedBow {

    public static void main(String[] args) throws Exception {
        File srcFile = new File("C:/Users/GreyHat/AppData/Local/Temp/21b6ef00-2c33-453c-95c3-189092370c31.png");
        if (!srcFile.exists()) {
            System.err.println("File not found: " + srcFile.getAbsolutePath());
            return;
        }

        BufferedImage screenshot = ImageIO.read(srcFile);
        int w = screenshot.getWidth();
        int h = screenshot.getHeight();

        // 1. Find bounding box of the bow pixels in screenshot
        // Background in MC is dark gray/black (R,G,B around < 40)
        int minX = w, maxX = 0, minY = h, maxY = 0;
        for (int y = 0; y < h * 0.75; y++) { // Exclude text area at bottom
            for (int x = 0; x < w; x++) {
                int rgb = screenshot.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Non-background pixel (has gold, cyan, white, silver, or dark outline)
                if (r > 35 || g > 35 || b > 35) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        System.out.println("Crop Bounding Box: [" + minX + ", " + minY + "] to [" + maxX + ", " + maxY + "]");

        int cropW = maxX - minX + 1;
        int cropH = maxY - minY + 1;
        BufferedImage cropped = new BufferedImage(cropW, cropH, BufferedImage.TYPE_INT_ARGB);

        // Copy cropped area with transparent background
        for (int y = 0; y < cropH; y++) {
            for (int x = 0; x < cropW; x++) {
                int srcX = minX + x;
                int srcY = minY + y;
                int rgb = screenshot.getRGB(srcX, srcY);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Threshold for background removal
                if (r < 30 && g < 35 && b < 30) {
                    cropped.setRGB(x, y, 0); // Transparent
                } else {
                    cropped.setRGB(x, y, rgb | 0xFF000000);
                }
            }
        }

        // 2. Horizontal Flip (Mirror Left-Right) so Bow Limb faces TOP-LEFT and string faces BOTTOM-RIGHT
        BufferedImage flipped = new BufferedImage(cropW, cropH, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < cropH; y++) {
            for (int x = 0; x < cropW; x++) {
                flipped.setRGB(cropW - 1 - x, y, cropped.getRGB(x, y));
            }
        }

        // 3. Scale and fit into 32x32 grid cleanly
        File itemsDir = new File("src/main/resources/assets/dark_grey/textures/items");
        itemsDir.mkdirs();

        generateFrames(flipped, itemsDir);

        System.out.println("Successfully processed uploaded bow texture!");
    }

    private static void generateFrames(BufferedImage flippedBow, File itemsDir) throws Exception {
        int targetSize = 32;

        for (int stage = 0; stage <= 3; stage++) {
            BufferedImage img = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();

            // Draw flipped bow scaled to fit 32x32
            int bw = flippedBow.getWidth();
            int bh = flippedBow.getHeight();
            double scale = Math.min((targetSize - 4.0) / bw, (targetSize - 4.0) / bh);
            int drawW = (int) (bw * scale);
            int drawH = (int) (bh * scale);
            int drawX = (targetSize - drawW) / 2;
            int drawY = (targetSize - drawH) / 2;

            g.drawImage(flippedBow, drawX, drawY, drawW, drawH, null);
            g.dispose();

            // If pulling (stage > 0), draw magic cyan dotted string pulling back towards bottom-right (31, 31)
            if (stage > 0) {
                // Top horn around (25, 5), Bottom horn around (5, 25)
                int pullX = 14 + stage * 3;
                int pullY = 14 + stage * 3;
                int stringColor = 0xFF00E6FF; // Bright Cyan

                // Draw string lines
                drawLine(img, 25, 5, pullX, pullY, stringColor);
                drawLine(img, 5, 25, pullX, pullY, stringColor);

                // Energy Arrow head pointing TOP-LEFT
                int arrowTipX = pullX - 10;
                int arrowTipY = pullY - 10;
                drawLine(img, pullX, pullY, arrowTipX, arrowTipY, 0xFF00E6FF);
                img.setRGB(arrowTipX, arrowTipY, 0xFFFFFFFF);
            }

            File destFile;
            if (stage == 0) {
                destFile = new File(itemsDir, "itanis.png");
            } else {
                destFile = new File(itemsDir, "itanis_pulling_" + (stage - 1) + ".png");
            }
            ImageIO.write(img, "png", destFile);
            System.out.println("Saved: " + destFile.getName());
        }
    }

    private static void drawLine(BufferedImage img, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            if (x0 >= 0 && x0 < img.getWidth() && y0 >= 0 && y0 < img.getHeight()) {
                img.setRGB(x0, y0, color);
            }
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }
}
