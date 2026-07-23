import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;

public class GenerateItanisPullingV6 {

    public static void main(String[] args) throws Exception {
        BufferedImage original = ImageIO.read(
            new File("src/main/resources/assets/dark_grey/textures/items/itanis.png"));
        
        int W = original.getWidth();
        int H = original.getHeight();
        
        // Find bowstring line
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
        
        int topX = -1, topY = H, botX = -1, botY = 0;
        for (int[] p : stringPixels) {
            if (p[1] < topY) { topY = p[1]; topX = p[0]; }
            if (p[1] > botY) { botY = p[1]; botX = p[0]; }
        }
        
        // Create clean bow body
        BufferedImage bowBody = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        double lineDx = botX - topX;
        double lineDy = botY - topY;
        double lineLen = Math.sqrt(lineDx * lineDx + lineDy * lineDy);
        double lineNx = -lineDy / lineLen; 
        double lineNy = lineDx / lineLen;
        
        double corridorHalfWidth = 30.0;
        
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int argb = original.getRGB(x, y);
                int a = (argb >> 24) & 0xff;
                if (a < 5) continue;
                
                double px = x - topX;
                double py = y - topY;
                double t = (px * lineDx + py * lineDy) / (lineLen * lineLen);
                double dist = Math.abs(px * lineNx + py * lineNy);
                
                double localWidth = corridorHalfWidth;
                if (t < 0.1 || t > 0.9) localWidth = corridorHalfWidth * 1.5; 
                
                boolean inCorridor = t >= -0.05 && t <= 1.05 && dist < localWidth;
                
                if (inCorridor) {
                    int r = (argb >> 16) & 0xff;
                    int g = (argb >> 8) & 0xff;
                    int b = argb & 0xff;
                    if (isCyanish(r, g, b) || isLightCyan(r, g, b, a)) {
                        continue;
                    }
                }
                
                bowBody.setRGB(x, y, argb);
            }
        }
        
        double perpX = lineDy / lineLen;
        double perpY = -lineDx / lineLen;
        if (perpX + perpY < 0) { perpX = -perpX; perpY = -perpY; }
        
        double maxPullDist = W * 0.28; 
        
        for (int frame = 0; frame < 3; frame++) {
            double pullFactor = (frame + 1) / 3.0;
            double pullDist = maxPullDist * pullFactor;
            
            double midX = (topX + botX) / 2.0;
            double midY = (topY + botY) / 2.0;
            double pulledMidX = midX + perpX * pullDist;
            double pulledMidY = midY + perpY * pullDist;
            
            BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            
            g.drawImage(bowBody, 0, 0, null);
            
            // Draw crisp bowstring
            float stringThickness = 6.0f;
            g.setColor(new Color(0, 230, 255, 255));
            g.setStroke(new BasicStroke(stringThickness, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
            g.drawLine(topX, topY, (int)pulledMidX, (int)pulledMidY);
            g.drawLine((int)pulledMidX, (int)pulledMidY, botX, botY);
            
            // Draw 2D crisp arrow pointing upper-left
            // The arrow starts at pulledMidX, pulledMidY and points to upper-left
            // Let's make it a diagonal line pointing (-1, -1)
            double arrowLen = W * 0.35;
            double dirX = -0.707; // 45 degrees up-left
            double dirY = -0.707;
            
            int tipX = (int)(pulledMidX + dirX * arrowLen);
            int tipY = (int)(pulledMidY + dirY * arrowLen);
            
            // Arrow shaft
            g.setColor(new Color(255, 255, 255, 255)); // Solid white core
            g.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
            g.drawLine((int)pulledMidX, (int)pulledMidY, tipX, tipY);
            
            // Arrow head
            int headSize = (int)(W * 0.04);
            g.drawLine(tipX, tipY, tipX + headSize, tipY);
            g.drawLine(tipX, tipY, tipX, tipY + headSize);
            
            g.dispose();
            
            String outPath = "src/main/resources/assets/dark_grey/textures/items/itanis_pulling_" + frame + ".png";
            ImageIO.write(out, "png", new File(outPath));
            System.out.println("Frame " + frame + " generated.");
        }
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
        if (a < 200 && b > 100 && g > 100 && b >= r) return true;
        if (b > 200 && g > 200 && r > 200 && b > r) return true; 
        return false;
    }
}
