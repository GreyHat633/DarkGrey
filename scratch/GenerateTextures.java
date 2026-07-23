package scratch;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GenerateTextures {

    public static void main(String[] args) throws Exception {
        File itemsDir = new File("src/main/resources/assets/dark_grey/textures/items");
        File entityDir = new File("src/main/resources/assets/dark_grey/textures/entity");
        itemsDir.mkdirs();
        entityDir.mkdirs();

        generateItanisBow(new File(itemsDir, "itanis.png"), 0);
        generateItanisBow(new File(itemsDir, "itanis_pulling_0.png"), 1);
        generateItanisBow(new File(itemsDir, "itanis_pulling_1.png"), 2);
        generateItanisBow(new File(itemsDir, "itanis_pulling_2.png"), 3);
        generateItanisArrow(new File(entityDir, "itanis_arrow.png"));

        System.out.println("Successfully generated Itanis 32x32 Pixel Art Bow textures!");
    }

    private static final int GOLD = 0xFFFFD700;
    private static final int LIGHT_GOLD = 0xFFFFF096;
    private static final int DARK_GOLD = 0xFFB8860B;
    private static final int CYAN_CORE = 0xFF00E6FF;
    private static final int BRIGHT_BLUE = 0xFF80F0FF;
    private static final int DEEP_BLUE = 0xFF0055AA;
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

    private static void generateItanisBow(File file, int pullStage) throws Exception {
        int width = 32;
        int height = 32;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // 1. Draw Bow Limbs (Recurve Bow Shape)
        int[][] upperSpine = {
            {16, 14}, {17, 13}, {18, 12}, {19, 11}, {20, 10}, {21, 9}, {22, 8}, {23, 7}, {24, 6}, {25, 5}, {26, 4}, {27, 3}
        };
        int[][] lowerSpine = {
            {14, 16}, {13, 17}, {12, 18}, {11, 19}, {10, 20}, {9, 21}, {8, 22}, {7, 23}, {6, 24}, {5, 25}, {4, 26}, {3, 27}
        };

        // Draw Limb Body (Cyan Core + Deep Blue outline + Gold Wing Accents)
        for (int i = 0; i < upperSpine.length; i++) {
            int x = upperSpine[i][0];
            int y = upperSpine[i][1];
            
            // Core
            setPixel(img, x, y, CYAN_CORE);
            
            // Outer armor
            setPixel(img, x - 1, y, DEEP_BLUE);
            setPixel(img, x, y - 1, DEEP_BLUE);
            setPixel(img, x - 1, y - 1, BRIGHT_BLUE);

            // Inner gold trim
            if (i % 2 == 0) {
                setPixel(img, x + 1, y + 1, GOLD);
            } else {
                setPixel(img, x + 1, y + 1, DARK_GOLD);
            }
        }

        for (int i = 0; i < lowerSpine.length; i++) {
            int x = lowerSpine[i][0];
            int y = lowerSpine[i][1];

            setPixel(img, x, y, CYAN_CORE);

            setPixel(img, x + 1, y, DEEP_BLUE);
            setPixel(img, x, y + 1, DEEP_BLUE);
            setPixel(img, x + 1, y + 1, BRIGHT_BLUE);

            if (i % 2 == 0) {
                setPixel(img, x - 1, y - 1, GOLD);
            } else {
                setPixel(img, x - 1, y - 1, DARK_GOLD);
            }
        }

        // Horn Tips (Golden Wings)
        setPixel(img, 27, 3, GOLD);
        setPixel(img, 28, 2, LIGHT_GOLD);
        setPixel(img, 28, 3, GOLD);
        setPixel(img, 27, 2, WHITE);

        setPixel(img, 3, 27, GOLD);
        setPixel(img, 2, 28, LIGHT_GOLD);
        setPixel(img, 3, 28, GOLD);
        setPixel(img, 2, 27, WHITE);

        // Center Grip & Royal Gem
        setPixel(img, 15, 15, WHITE);
        setPixel(img, 14, 15, CYAN_CORE);
        setPixel(img, 16, 15, CYAN_CORE);
        setPixel(img, 15, 14, CYAN_CORE);
        setPixel(img, 15, 16, CYAN_CORE);

        setPixel(img, 14, 14, GOLD);
        setPixel(img, 16, 14, GOLD);
        setPixel(img, 14, 16, GOLD);
        setPixel(img, 16, 16, GOLD);

        setPixel(img, 13, 15, DARK_GOLD);
        setPixel(img, 17, 15, DARK_GOLD);
        setPixel(img, 15, 13, DARK_GOLD);
        setPixel(img, 15, 17, DARK_GOLD);

        // 2. String & Pulling Mechanics
        int topHornX = 27, topHornY = 3;
        int botHornX = 3, botHornY = 27;

        int pullX, pullY;
        if (pullStage == 0) {
            pullX = 14;
            pullY = 14;
            drawLine(img, topHornX, topHornY, botHornX, botHornY, STRING_COLOR);
        } else {
            pullX = 14 - pullStage * 3;
            pullY = 14 + pullStage * 3;

            drawLine(img, topHornX, topHornY, pullX, pullY, STRING_COLOR);
            drawLine(img, botHornX, botHornY, pullX, pullY, STRING_COLOR);

            // Draw Magic Energy Arrow being loaded
            int tipX = pullX + 12;
            int tipY = pullY - 12;

            drawLine(img, pullX, pullY, tipX, tipY, CYAN_CORE);

            // Arrowhead
            setPixel(img, tipX, tipY, WHITE);
            setPixel(img, tipX - 1, tipY, LIGHT_GOLD);
            setPixel(img, tipX, tipY + 1, LIGHT_GOLD);

            // Energy aura at nock
            setPixel(img, pullX, pullY, AURA_GOLD);
            setPixel(img, pullX + 1, pullY - 1, BRIGHT_BLUE);
            if (pullStage == 3) {
                // Full charge radiant aura
                setPixel(img, pullX - 1, pullY + 1, AURA_GOLD);
                setPixel(img, pullX + 1, pullY, AURA_GOLD);
                setPixel(img, pullX, pullY - 1, AURA_GOLD);
            }
        }

        ImageIO.write(img, "png", file);
    }

    private static void generateItanisArrow(File file) throws Exception {
        int width = 64;
        int height = 64;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int i = 0; i < 40; i++) {
            int x = 12 + i;
            int y = 52 - i;
            setPixel(img, x, y, DEEP_BLUE);
            setPixel(img, x + 1, y, CYAN_CORE);
        }

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                setPixel(img, 52 + dx, 12 + dy, CYAN_CORE);
            }
        }
        setPixel(img, 53, 11, WHITE);
        setPixel(img, 54, 10, GOLD);

        ImageIO.write(img, "png", file);
    }
}
