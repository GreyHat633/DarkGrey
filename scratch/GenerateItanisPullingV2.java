import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;

/**
 * Generate Itanis bow pulling animation frames - V2 with fixes:
 * 1. Don't strip the blue gem from the bow body (it's not bowstring)
 * 2. Make the arrow bigger and more visible
 * 3. Adjust bowstring thickness to be moderate (not too thick, not too thin)
 * 4. The bowstring V-shape should be more pronounced
 */
public class GenerateItanisPullingV2 {

    public static void main(String[] args) throws Exception {
        BufferedImage original = ImageIO.read(
            new File("src/main/resources/assets/dark_grey/textures/items/itanis.png"));
        BufferedImage arrowTex = ImageIO.read(
            new File("src/main/resources/assets/dark_grey/textures/entity/itanis_arrow.png"));
        
        int W = original.getWidth();  // 720
        int H = original.getHeight(); // 720
        
        // Step 1: Separate bowstring from bow body
        // The bowstring is the cyan diagonal line. We need to be more careful:
        // - The blue GEM at the center of the bow handle should NOT be treated as bowstring
        // - The bowstring is thin and linear, running from upper-right to lower-left
        // Strategy: find the center of the bow body, and any cyan pixels near the center
        // are the gem, not the bowstring.
        
        // First, find the center of mass of the entire image
        long totalX = 0, totalY = 0, count = 0;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int a = (original.getRGB(x, y) >> 24) & 0xff;
                if (a > 10) { totalX += x; totalY += y; count++; }
            }
        }
        double centerX = totalX / (double)count;
        double centerY = totalY / (double)count;
        System.out.println("Image center of mass: (" + (int)centerX + "," + (int)centerY + ")");
        
        // The bowstring runs from the upper-right tip to the lower-left tip
        // We need to find these tips by looking at cyan pixels that are far from the center
        // and along the string diagonal
        
        // Collect all cyan-ish pixels
        java.util.List<int[]> cyanPixels = new java.util.ArrayList<>();
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int argb = original.getRGB(x, y);
                int a = (argb >> 24) & 0xff;
                if (a < 10) continue;
                int r = (argb >> 16) & 0xff;
                int g = (argb >> 8) & 0xff;
                int b = argb & 0xff;
                if (isCyanStringColor(r, g, b)) {
                    cyanPixels.add(new int[]{x, y, argb});
                }
            }
        }
        System.out.println("Total cyan pixels: " + cyanPixels.size());
        
        // The bowstring should be a thin line. Pixels that are part of a large blob 
        // near the center are likely the gem, not the string.
        // Strategy: The bowstring pixels form a narrow band. For each cyan pixel,
        // check if it's within a "thick" cluster. If so, it's the gem.
        
        // Create bow body = original with bowstring removed
        // Create bowstring mask
        BufferedImage bowBody = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        
        // Copy everything to bowBody first
        Graphics2D gBody = bowBody.createGraphics();
        gBody.drawImage(original, 0, 0, null);
        gBody.dispose();
        
        // Now identify true bowstring pixels and remove them from bowBody
        // True bowstring = cyan pixels that form a thin line (few neighboring cyan pixels per row)
        // Gem = cyan pixels in a thick cluster near the center
        
        // For each row, count consecutive cyan pixels
        // Bowstring: 1-8 pixels wide (at 720px scale)
        // Gem: much wider cluster
        int stringThreshold = 16; // max consecutive cyan pixels for bowstring
        
        // Track bowstring endpoints
        int stringTopX = -1, stringTopY = H;
        int stringBotX = -1, stringBotY = 0;
        
        boolean[][] isString = new boolean[H][W];
        
        for (int y = 0; y < H; y++) {
            // Find runs of cyan pixels in this row
            int runStart = -1;
            for (int x = 0; x <= W; x++) {
                boolean isCyan = false;
                if (x < W) {
                    int argb = original.getRGB(x, y);
                    int a = (argb >> 24) & 0xff;
                    if (a > 10) {
                        int r = (argb >> 16) & 0xff;
                        int g = (argb >> 8) & 0xff;
                        int b = argb & 0xff;
                        isCyan = isCyanStringColor(r, g, b);
                    }
                }
                
                if (isCyan && runStart == -1) {
                    runStart = x;
                } else if (!isCyan && runStart != -1) {
                    int runLen = x - runStart;
                    // If this run is thin enough, it's bowstring
                    if (runLen <= stringThreshold) {
                        for (int sx = runStart; sx < x; sx++) {
                            isString[y][sx] = true;
                            // Remove from bow body
                            bowBody.setRGB(sx, y, 0x00000000);
                            
                            // Track endpoints
                            if (y < stringTopY) { stringTopY = y; stringTopX = sx; }
                            if (y > stringBotY) { stringBotY = y; stringBotX = sx; }
                        }
                    }
                    // Thick runs (gem) stay in bowBody
                    runStart = -1;
                }
            }
        }
        
        System.out.println("Bowstring top endpoint: (" + stringTopX + "," + stringTopY + ")");
        System.out.println("Bowstring bottom endpoint: (" + stringBotX + "," + stringBotY + ")");
        
        // Step 2: Generate pulling frames
        // Arrow should be properly sized and visible
        int arrowLength = (int)(W * 0.30);
        BufferedImage scaledArrow = scaleArrow(arrowTex, arrowLength);
        System.out.println("Scaled arrow: " + scaledArrow.getWidth() + "x" + scaledArrow.getHeight());
        
        for (int frame = 0; frame < 3; frame++) {
            double pullFactor = (frame + 1) / 3.0;
            
            BufferedImage frameImg = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = frameImg.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            // Draw bow body (no shift - bow stays in place, only string moves)
            g.drawImage(bowBody, 0, 0, null);
            
            // Draw pulled bowstring with V-shape
            double pullDist = W * 0.10 * pullFactor;
            // String midpoint moves toward lower-right (perpendicular to the string line)
            // The string goes from upper-right (stringTopX, stringTopY) to lower-left (stringBotX, stringBotY)
            // Direction along string: (stringBotX - stringTopX, stringBotY - stringTopY) 
            // Perpendicular (toward inside of bow = toward lower-right): rotate 90° CW
            double dx = stringBotX - stringTopX;
            double dy = stringBotY - stringTopY;
            double len = Math.sqrt(dx*dx + dy*dy);
            // Perpendicular: (dy, -dx) normalized, but we want the one pointing toward lower-right
            double perpX = dy / len;  // points right-ward 
            double perpY = -dx / len; // points down-ward
            // Check: if perpendicular points toward upper-left, flip it
            if (perpX + perpY < 0) { perpX = -perpX; perpY = -perpY; }
            
            double pulledMidX = (stringTopX + stringBotX) / 2.0 + perpX * pullDist;
            double pulledMidY = (stringTopY + stringBotY) / 2.0 + perpY * pullDist;
            
            // Draw the V-shaped bowstring
            // Use moderate thickness - not too thin, not too thick
            float lineWidth = Math.max(3, W / 160.0f); // about 4.5px at 720
            
            // Glow layer
            g.setColor(new Color(80, 200, 255, 50));
            g.setStroke(new BasicStroke(lineWidth * 3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(stringTopX, stringTopY, (int)pulledMidX, (int)pulledMidY);
            g.drawLine((int)pulledMidX, (int)pulledMidY, stringBotX, stringBotY);
            
            // Main string
            g.setColor(new Color(100, 220, 255, 210));
            g.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(stringTopX, stringTopY, (int)pulledMidX, (int)pulledMidY);
            g.drawLine((int)pulledMidX, (int)pulledMidY, stringBotX, stringBotY);
            
            // Bright core
            g.setColor(new Color(180, 240, 255, 180));
            g.setStroke(new BasicStroke(lineWidth * 0.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(stringTopX, stringTopY, (int)pulledMidX, (int)pulledMidY);
            g.drawLine((int)pulledMidX, (int)pulledMidY, stringBotX, stringBotY);
            
            // Draw arrow nocked at the pulled midpoint
            // Arrow tip points upper-left, nock (tail) at the pulled midpoint
            int arrowDrawX = (int)pulledMidX - scaledArrow.getWidth() + scaledArrow.getWidth()/6;
            int arrowDrawY = (int)pulledMidY - scaledArrow.getHeight() + scaledArrow.getHeight()/6;
            g.drawImage(scaledArrow, arrowDrawX, arrowDrawY, null);
            
            g.dispose();
            
            String outPath = "src/main/resources/assets/dark_grey/textures/items/itanis_pulling_" + frame + ".png";
            ImageIO.write(frameImg, "png", new File(outPath));
            System.out.println("Frame " + frame + " saved: " + outPath + 
                " (pullDist=" + (int)pullDist + ", midpoint=" + (int)pulledMidX + "," + (int)pulledMidY + ")");
        }
        
        System.out.println("Done!");
    }
    
    static boolean isCyanStringColor(int r, int g, int b) {
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float hue = hsb[0] * 360;
        float sat = hsb[1];
        float bri = hsb[2];
        // Cyan: hue 170-210, moderate-to-high saturation and brightness
        // But NOT near-white or very dark
        return hue >= 170 && hue <= 215 && sat > 0.25 && bri > 0.5 && bri < 0.98;
    }
    
    static BufferedImage scaleArrow(BufferedImage arrowTex, int targetLength) {
        int aw = arrowTex.getWidth();
        int ah = arrowTex.getHeight();
        double diag = Math.sqrt(aw * aw + ah * ah);
        double scale = targetLength / diag;
        int newW = Math.max(1, (int)(aw * scale));
        int newH = Math.max(1, (int)(ah * scale));
        
        BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(arrowTex, 0, 0, newW, newH, null);
        g.dispose();
        return scaled;
    }
}
