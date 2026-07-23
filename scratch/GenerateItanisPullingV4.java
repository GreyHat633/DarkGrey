import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;

/**
 * V4: Final fix
 * - Erase bowstring with a wide corridor (not per-pixel detection)
 * - Draw clean new string
 * - Properly sized arrow
 */
public class GenerateItanisPullingV4 {

    public static void main(String[] args) throws Exception {
        BufferedImage original = ImageIO.read(
            new File("src/main/resources/assets/dark_grey/textures/items/itanis.png"));
        BufferedImage arrowTex = ImageIO.read(
            new File("src/main/resources/assets/dark_grey/textures/entity/itanis_arrow.png"));
        
        int W = original.getWidth();
        int H = original.getHeight();
        
        // Step 1: Find the bowstring line by detecting cyan pixels
        // Use thin-run detection to find the line, then get its equation
        boolean[][] cyanMask = new boolean[H][W];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int argb = original.getRGB(x, y);
                int a = (argb >> 24) & 0xff;
                if (a < 5) continue;
                int r = (argb >> 16) & 0xff;
                int g = (argb >> 8) & 0xff;
                int b = argb & 0xff;
                if (isCyanish(r, g, b)) {
                    cyanMask[y][x] = true;
                }
            }
        }
        
        // Find thin string pixels (not the gem)
        // For each row, find thin cyan runs
        java.util.List<int[]> stringPixels = new java.util.ArrayList<>();
        int maxRunWidth = 14;
        
        for (int y = 0; y < H; y++) {
            int runStart = -1;
            for (int x = 0; x <= W; x++) {
                boolean cyan = (x < W) && cyanMask[y][x];
                if (cyan && runStart == -1) {
                    runStart = x;
                } else if (!cyan && runStart != -1) {
                    int runLen = x - runStart;
                    if (runLen <= maxRunWidth) {
                        int midX = (runStart + x - 1) / 2;
                        stringPixels.add(new int[]{midX, y});
                    }
                    runStart = -1;
                }
            }
        }
        
        // Fit a line through the string center points
        // The string goes from upper-right to lower-left
        // Find endpoints
        int topX = -1, topY = H, botX = -1, botY = 0;
        for (int[] p : stringPixels) {
            if (p[1] < topY) { topY = p[1]; topX = p[0]; }
            if (p[1] > botY) { botY = p[1]; botX = p[0]; }
        }
        System.out.println("String line from (" + topX + "," + topY + ") to (" + botX + "," + botY + ")");
        
        // Step 2: Create bow body by erasing everything in a corridor around the string line
        // The corridor width should be wide enough to catch all checkered/semi-transparent string pixels
        BufferedImage bowBody = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        
        double lineDx = botX - topX;
        double lineDy = botY - topY;
        double lineLen = Math.sqrt(lineDx * lineDx + lineDy * lineDy);
        double lineNx = -lineDy / lineLen; // perpendicular direction
        double lineNy = lineDx / lineLen;
        
        double corridorHalfWidth = 30.0; // pixels on each side of center line to erase
        
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int argb = original.getRGB(x, y);
                int a = (argb >> 24) & 0xff;
                if (a < 5) continue;
                
                // Check distance to the string center line
                double px = x - topX;
                double py = y - topY;
                
                // Project onto the line direction
                double t = (px * lineDx + py * lineDy) / (lineLen * lineLen);
                
                // Distance perpendicular to line
                double dist = Math.abs(px * lineNx + py * lineNy);
                
                // Variable width: wider near endpoints (where string meets bow tips)
                double localWidth = corridorHalfWidth;
                if (t < 0.1 || t > 0.9) localWidth = corridorHalfWidth * 1.5; // wider near tips
                
                // If within corridor AND along the line (not beyond endpoints)
                // And the pixel is cyan-ish, erase it
                boolean inCorridor = t >= -0.05 && t <= 1.05 && dist < localWidth;
                
                if (inCorridor) {
                    // Check if pixel is cyan/blue/light-blue - only erase bowstring colors
                    int r = (argb >> 16) & 0xff;
                    int g = (argb >> 8) & 0xff;
                    int b = argb & 0xff;
                    if (isCyanish(r, g, b) || isLightCyan(r, g, b, a)) {
                        // Erase - don't copy to bowBody
                        continue;
                    }
                }
                
                bowBody.setRGB(x, y, argb);
            }
        }
        
        // Scale arrow  
        // The arrow texture is 64x64, we want it to be about 30% of 720 diagonal = ~216 in each axis
        int arrowSize = (int)(W * 0.30);
        BufferedImage scaledArrow = new BufferedImage(arrowSize, arrowSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gA = scaledArrow.createGraphics();
        gA.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        gA.drawImage(arrowTex, 0, 0, arrowSize, arrowSize, null);
        gA.dispose();
        System.out.println("Arrow: " + arrowSize + "x" + arrowSize);
        
        // Perpendicular direction for pull (toward lower-right, inside the bow curve)
        double perpX = lineDy / lineLen;
        double perpY = -lineDx / lineLen;
        if (perpX + perpY < 0) { perpX = -perpX; perpY = -perpY; }
        
        for (int frame = 0; frame < 3; frame++) {
            double pullFactor = (frame + 1) / 3.0;
            double pullDist = W * 0.11 * pullFactor;
            
            double midX = (topX + botX) / 2.0;
            double midY = (topY + botY) / 2.0;
            double pulledMidX = midX + perpX * pullDist;
            double pulledMidY = midY + perpY * pullDist;
            
            BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            // Draw bow body
            g.drawImage(bowBody, 0, 0, null);
            
            // Draw V-shaped bowstring
            float baseStroke = W / 160.0f;
            
            // Glow
            g.setColor(new Color(60, 190, 255, 35));
            g.setStroke(new BasicStroke(baseStroke * 5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(topX, topY, (int)pulledMidX, (int)pulledMidY);
            g.drawLine((int)pulledMidX, (int)pulledMidY, botX, botY);
            
            // Main string
            g.setColor(new Color(90, 215, 255, 200));
            g.setStroke(new BasicStroke(baseStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(topX, topY, (int)pulledMidX, (int)pulledMidY);
            g.drawLine((int)pulledMidX, (int)pulledMidY, botX, botY);
            
            // Core
            g.setColor(new Color(210, 248, 255, 150));
            g.setStroke(new BasicStroke(baseStroke * 0.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(topX, topY, (int)pulledMidX, (int)pulledMidY);
            g.drawLine((int)pulledMidX, (int)pulledMidY, botX, botY);
            
            // Draw arrow - nock at pulled midpoint
            // Arrow points from lower-right to upper-left in the texture
            // Place so the nock (lower-right corner area) is at pulledMidX, pulledMidY
            int ax = (int)pulledMidX - arrowSize + arrowSize / 5;
            int ay = (int)pulledMidY - arrowSize + arrowSize / 5;
            g.drawImage(scaledArrow, ax, ay, null);
            
            g.dispose();
            
            String outPath = "src/main/resources/assets/dark_grey/textures/items/itanis_pulling_" + frame + ".png";
            ImageIO.write(out, "png", new File(outPath));
            System.out.println("Frame " + frame + " saved (pull=" + (int)pullDist + "px)");
        }
        
        System.out.println("Done!");
    }
    
    static boolean isCyanish(int r, int g, int b) {
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float hue = hsb[0] * 360;
        float sat = hsb[1];
        float bri = hsb[2];
        if (hue >= 155 && hue <= 230 && sat > 0.15 && bri > 0.30) return true;
        if (b > 160 && g > 160 && r < 160 && sat > 0.08) return true;
        return false;
    }
    
    static boolean isLightCyan(int r, int g, int b, int a) {
        // Catch very light / semi-transparent cyan pixels that form the checkered pattern
        if (a < 200 && b > 100 && g > 100 && b >= r) return true;
        if (b > 200 && g > 200 && r > 200 && b > r) return true; // near-white with blue tint
        return false;
    }
}
