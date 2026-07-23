package scratch;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GenerateVanillaOrientedBows {

    public static void main(String[] args) throws Exception {
        File previewDir = new File("scratch/previews");
        previewDir.mkdirs();

        generateOption1(previewDir);
        generateOption2(previewDir);
        generateOption3(previewDir);
        generateOption4(previewDir);
        generateOption5(previewDir);

        System.out.println("Successfully generated 5 Vanilla-Oriented 32x32 Bow Options!");
    }

    private static final int GOLD = 0xFFFFD700;
    private static final int LIGHT_GOLD = 0xFFFFF096;
    private static final int DARK_GOLD = 0xFFB8860B;
    private static final int CYAN_CORE = 0xFF00E6FF;
    private static final int BRIGHT_BLUE = 0xFF80F0FF;
    private static final int DEEP_BLUE = 0xFF0044AA;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int STRING_COLOR = 0xDDD0F8FF;
    private static final int AURA_GOLD = 0xAAFFC800;

    private static void setPixel(BufferedImage img, int x, int y, int color) {
        if (x >= 0 && x < img.getWidth() && y >= 0 && y < img.getHeight()) {
            img.setRGB(x, y, color);
        }
    }

    private static void drawLine(BufferedImage img, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            setPixel(img, x0, y0, color);
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

    // Option 1: 天界晶羽反曲弓 (Celestial Crystal Recurve)
    private static void generateOption1(File dir) throws Exception {
        for (int p = 0; p <= 3; p++) {
            BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            
            // Top tip: (26, 4), Bottom tip: (4, 26)
            // Limb curves OUTWARDS towards TOP-LEFT (0,0)
            int[][] limbOuter = {
                {4, 26}, {3, 23}, {2, 20}, {2, 17}, {3, 14}, {5, 11}, {8, 8}, {11, 5}, {14, 3}, {17, 2}, {20, 2}, {23, 3}, {26, 4}
            };
            int[][] limbInner = {
                {5, 25}, {4, 22}, {3, 19}, {3, 16}, {4, 13}, {6, 10}, {9, 9}, {10, 6}, {13, 4}, {16, 3}, {19, 3}, {22, 4}, {25, 5}
            };

            // Draw Limb
            for (int i = 0; i < limbOuter.length; i++) {
                setPixel(img, limbOuter[i][0], limbOuter[i][1], DEEP_BLUE);
                setPixel(img, limbInner[i][0], limbInner[i][1], CYAN_CORE);
            }

            // Gold Wing Accents on Outer edge
            setPixel(img, 2, 17, GOLD);
            setPixel(img, 17, 2, GOLD);
            setPixel(img, 1, 17, LIGHT_GOLD);
            setPixel(img, 17, 1, LIGHT_GOLD);

            // Horn Tips
            setPixel(img, 26, 4, GOLD);
            setPixel(img, 27, 4, LIGHT_GOLD);
            setPixel(img, 4, 26, GOLD);
            setPixel(img, 4, 27, LIGHT_GOLD);

            // Center Grip Gem
            setPixel(img, 8, 8, WHITE);
            setPixel(img, 7, 8, CYAN_CORE);
            setPixel(img, 8, 7, CYAN_CORE);
            setPixel(img, 9, 8, GOLD);
            setPixel(img, 8, 9, GOLD);

            // Bowstring & Pulling (Pulled towards BOTTOM-RIGHT (31,31))
            int topHornX = 26, topHornY = 4;
            int botHornX = 4, botHornY = 26;

            if (p == 0) {
                drawLine(img, topHornX, topHornY, botHornX, botHornY, STRING_COLOR);
            } else {
                int pullX = 14 + p * 3;
                int pullY = 14 + p * 3;
                drawLine(img, topHornX, topHornY, pullX, pullY, STRING_COLOR);
                drawLine(img, botHornX, botHornY, pullX, pullY, STRING_COLOR);

                // Energy Arrow pointing TOP-LEFT
                int arrowTipX = pullX - 12;
                int arrowTipY = pullY - 12;
                drawLine(img, pullX, pullY, arrowTipX, arrowTipY, CYAN_CORE);
                setPixel(img, arrowTipX, arrowTipY, WHITE);
                setPixel(img, arrowTipX + 1, arrowTipY, LIGHT_GOLD);
                setPixel(img, arrowTipX, arrowTipY + 1, LIGHT_GOLD);
            }

            String name = p == 0 ? "v1_standby.png" : "v1_pull_" + (p - 1) + ".png";
            ImageIO.write(img, "png", new File(dir, name));
        }
    }

    // Option 2: 炽天使大天使羽翼弓 (Archangel Wing Bow)
    private static void generateOption2(File dir) throws Exception {
        for (int p = 0; p <= 3; p++) {
            BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            
            // White feather wings curving to top-left
            for (int i = 0; i < 22; i++) {
                int x = 4 + i;
                int y = 26 - i;
                // Feather wing outer curve
                setPixel(img, x - 2, y - 2, WHITE);
                setPixel(img, x - 1, y - 1, BRIGHT_BLUE);
                setPixel(img, x, y, GOLD);
            }

            // Golden Armor Plates
            setPixel(img, 5, 23, GOLD);
            setPixel(img, 23, 5, GOLD);
            setPixel(img, 13, 13, WHITE); // Core

            // Bowstring
            int topHornX = 25, topHornY = 5;
            int botHornX = 5, botHornY = 25;

            if (p == 0) {
                drawLine(img, topHornX, topHornY, botHornX, botHornY, STRING_COLOR);
            } else {
                int pullX = 13 + p * 3;
                int pullY = 13 + p * 3;
                drawLine(img, topHornX, topHornY, pullX, pullY, STRING_COLOR);
                drawLine(img, botHornX, botHornY, pullX, pullY, STRING_COLOR);

                int arrowTipX = pullX - 10;
                int arrowTipY = pullY - 10;
                drawLine(img, pullX, pullY, arrowTipX, arrowTipY, CYAN_CORE);
                setPixel(img, arrowTipX, arrowTipY, WHITE);
            }

            String name = p == 0 ? "v2_standby.png" : "v2_pull_" + (p - 1) + ".png";
            ImageIO.write(img, "png", new File(dir, name));
        }
    }

    // Option 3: 月华星界弯月长弓 (Astral Crescent Bow)
    private static void generateOption3(File dir) throws Exception {
        for (int p = 0; p <= 3; p++) {
            BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);

            // Crescent moon arc
            for (int i = 0; i < 20; i++) {
                int x = 4 + i;
                int y = 24 - i;
                setPixel(img, x - 1, y - 1, CYAN_CORE);
                setPixel(img, x, y, DEEP_BLUE);
            }

            // Star Gem Orbs
            setPixel(img, 4, 24, GOLD);
            setPixel(img, 24, 4, GOLD);
            setPixel(img, 14, 14, WHITE);

            int topHornX = 24, topHornY = 4;
            int botHornX = 4, botHornY = 24;

            if (p == 0) {
                drawLine(img, topHornX, topHornY, botHornX, botHornY, STRING_COLOR);
            } else {
                int pullX = 14 + p * 3;
                int pullY = 14 + p * 3;
                drawLine(img, topHornX, topHornY, pullX, pullY, STRING_COLOR);
                drawLine(img, botHornX, botHornY, pullX, pullY, STRING_COLOR);
            }

            String name = p == 0 ? "v3_standby.png" : "v3_pull_" + (p - 1) + ".png";
            ImageIO.write(img, "png", new File(dir, name));
        }
    }

    // Option 4: 龙脊神骨魔弓 (Dragon Bone Rune Bow)
    private static void generateOption4(File dir) throws Exception {
        for (int p = 0; p <= 3; p++) {
            BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);

            for (int i = 0; i < 22; i++) {
                int x = 3 + i;
                int y = 25 - i;
                setPixel(img, x - 1, y - 2, CYAN_CORE);
                setPixel(img, x, y, BRIGHT_BLUE);
            }

            setPixel(img, 3, 25, GOLD);
            setPixel(img, 25, 3, GOLD);

            int topHornX = 25, topHornY = 3;
            int botHornX = 3, botHornY = 25;

            if (p == 0) {
                drawLine(img, topHornX, topHornY, botHornX, botHornY, STRING_COLOR);
            } else {
                int pullX = 14 + p * 3;
                int pullY = 14 + p * 3;
                drawLine(img, topHornX, topHornY, pullX, pullY, STRING_COLOR);
                drawLine(img, botHornX, botHornY, pullX, pullY, STRING_COLOR);
            }

            String name = p == 0 ? "v4_standby.png" : "v4_pull_" + (p - 1) + ".png";
            ImageIO.write(img, "png", new File(dir, name));
        }
    }

    // Option 5: 离子光刃科技重弓 (Plasma Blade Heavy Bow)
    private static void generateOption5(File dir) throws Exception {
        for (int p = 0; p <= 3; p++) {
            BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);

            for (int i = 0; i < 22; i++) {
                int x = 4 + i;
                int y = 26 - i;
                setPixel(img, x - 1, y - 1, BRIGHT_BLUE);
                setPixel(img, x, y, GOLD);
            }

            setPixel(img, 15, 15, WHITE);

            int topHornX = 26, topHornY = 4;
            int botHornX = 4, botHornY = 26;

            if (p == 0) {
                drawLine(img, topHornX, topHornY, botHornX, botHornY, STRING_COLOR);
            } else {
                int pullX = 15 + p * 3;
                int pullY = 15 + p * 3;
                drawLine(img, topHornX, topHornY, pullX, pullY, STRING_COLOR);
                drawLine(img, botHornX, botHornY, pullX, pullY, STRING_COLOR);
            }

            String name = p == 0 ? "v5_standby.png" : "v5_pull_" + (p - 1) + ".png";
            ImageIO.write(img, "png", new File(dir, name));
        }
    }
}
