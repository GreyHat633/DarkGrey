import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;

/**
 * V3: Complete fix for pulling animation
 * - Completely remove the original checkered bowstring  
 * - Draw clean new bowstring with V-shape pull
 * - Make arrow visible and properly positioned
 */
public class GenerateItanisPullingV3 {

    public static void main(String[] args) throws Exception {
        BufferedImage original = ImageIO.read(
            new File("src/main/resources/assets/dark_grey/textures/items/itanis.png"));
        BufferedImage arrowTex = ImageIO.read(
            new File("src/main/resources/assets/dark_grey/textures/entity/itanis_arrow.png"));
        
        int W = original.getWidth();
        int H = original.getHeight();
        
        // Step 1: Identify and classify all pixels
        // The bowstring is cyan/blue checkered pattern going from upper-right to lower-left
        // The bow body is golden/dark/white
        // The blue GEM is a large solid blue area near the center of the grip
        
        // Build a mask of which pixels are bowstring vs body
        boolean[][] isString = new boolean[H][W];
        
        // First pass: find all cyan-ish pixels
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int argb = original.getRGB(x, y);
                int a = (argb >> 24) & 0xff;
                if (a < 10) continue;
                int r = (argb >> 16) & 0xff;
                int g = (argb >> 8) & 0xff;
                int b = argb & 0xff;
                if (isCyanish(r, g, b)) {
                    isString[y][x] = true;
                }
            }
        }
        
        // Second pass: identify thick clusters (these are the gem, not string)
        // For each row, if a horizontal run of cyan pixels is wider than threshold, it's gem
        int maxStringRunWidth = 12; // at 720px scale, bowstring should be thin
        
        for (int y = 0; y < H; y++) {
            int runStart = -1;
            for (int x = 0; x <= W; x++) {
                boolean cyan = (x < W) && isString[y][x];
                if (cyan && runStart == -1) {
                    runStart = x;
                } else if (!cyan && runStart != -1) {
                    int runLen = x - runStart;
                    if (runLen > maxStringRunWidth) {
                        // This is gem, not bowstring - mark as NOT string
                        for (int sx = runStart; sx < x; sx++) {
                            isString[y][sx] = false;
                        }
                    }
                    runStart = -1;
                }
            }
        }
        
        // Also do vertical check
        for (int x = 0; x < W; x++) {
            int runStart = -1;
            for (int y = 0; y <= H; y++) {
                boolean cyan = (y < H) && isString[y][x];
                if (cyan && runStart == -1) {
                    runStart = y;
                } else if (!cyan && runStart != -1) {
                    int runLen = y - runStart;
                    if (runLen > maxStringRunWidth) {
                        for (int sy = runStart; sy < y; sy++) {
                            isString[sy][x] = false;
                        }
                    }
                    runStart = -1;
                }
            }
        }
        
        // Find bowstring endpoints
        int stringTopX = -1, stringTopY = H;
        int stringBotX = -1, stringBotY = 0;
        int stringCount = 0;
        
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                if (isString[y][x]) {
                    stringCount++;
                    if (y < stringTopY) { stringTopY = y; stringTopX = x; }
                    if (y > stringBotY) { stringBotY = y; stringBotX = x; }
                }
            }
        }
        System.out.println("Bowstring pixels identified: " + stringCount);
        System.out.println("Top: (" + stringTopX + "," + stringTopY + "), Bot: (" + stringBotX + "," + stringBotY + ")");
        
        // Create bow body without bowstring
        BufferedImage bowBody = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                if (!isString[y][x]) {
                    bowBody.setRGB(x, y, original.getRGB(x, y));
                }
            }
        }
        
        // Scale arrow to be visible 
        double arrowScale = (W * 0.40) / Math.sqrt(arrowTex.getWidth() * arrowTex.getWidth() + arrowTex.getHeight() * arrowTex.getHeight());
        int newAW = Math.max(1, (int)(arrowTex.getWidth() * arrowScale));
        int newAH = Math.max(1, (int)(arrowTex.getHeight() * arrowScale));
        BufferedImage scaledArrow = new BufferedImage(newAW, newAH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gA = scaledArrow.createGraphics();
        gA.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        gA.drawImage(arrowTex, 0, 0, newAW, newAH, null);
        gA.dispose();
        System.out.println("Arrow scaled to: " + newAW + "x" + newAH);
        
        // Generate frames
        for (int frame = 0; frame < 3; frame++) {
            double pullFactor = (frame + 1) / 3.0;
            
            BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            // 1. Draw bow body (stays in place)
            g.drawImage(bowBody, 0, 0, null);
            
            // 2. Calculate pulled midpoint
            // String goes from (stringTopX, stringTopY) to (stringBotX, stringBotY)
            // Mid point pulled toward lower-right (inside the bow's concave side)
            double midX = (stringTopX + stringBotX) / 2.0;
            double midY = (stringTopY + stringBotY) / 2.0;
            
            // Direction perpendicular to string, pointing toward lower-right
            double dx = stringBotX - stringTopX;
            double dy = stringBotY - stringTopY;
            double len = Math.sqrt(dx * dx + dy * dy);
            double perpX = dy / len;   
            double perpY = -dx / len;  
            if (perpX + perpY < 0) { perpX = -perpX; perpY = -perpY; }
            
            double pullDist = W * 0.11 * pullFactor;
            double pulledMidX = midX + perpX * pullDist;
            double pulledMidY = midY + perpY * pullDist;
            
            // 3. Draw pulled bowstring as V-shape
            float baseStroke = W / 180.0f; // ~4px
            
            // Outer glow
            g.setColor(new Color(60, 180, 255, 40));
            g.setStroke(new BasicStroke(baseStroke * 4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(stringTopX, stringTopY, (int)pulledMidX, (int)pulledMidY);
            g.drawLine((int)pulledMidX, (int)pulledMidY, stringBotX, stringBotY);
            
            // Main line
            g.setColor(new Color(80, 210, 255, 200));
            g.setStroke(new BasicStroke(baseStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(stringTopX, stringTopY, (int)pulledMidX, (int)pulledMidY);
            g.drawLine((int)pulledMidX, (int)pulledMidY, stringBotX, stringBotY);
            
            // Inner bright core
            g.setColor(new Color(200, 245, 255, 160));
            g.setStroke(new BasicStroke(baseStroke * 0.35f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(stringTopX, stringTopY, (int)pulledMidX, (int)pulledMidY);
            g.drawLine((int)pulledMidX, (int)pulledMidY, stringBotX, stringBotY);
            
            // 4. Draw arrow
            // The arrow's lower-right corner (tail/nock) should be at the pulled midpoint
            int arrowX = (int)pulledMidX - newAW + newAW / 8;
            int arrowY = (int)pulledMidY - newAH + newAH / 8;
            g.drawImage(scaledArrow, arrowX, arrowY, null);
            
            g.dispose();
            
            String outPath = "src/main/resources/assets/dark_grey/textures/items/itanis_pulling_" + frame + ".png";
            ImageIO.write(out, "png", new File(outPath));
            System.out.println("Frame " + frame + ": pull=" + (int)pullDist + "px, mid=(" + (int)pulledMidX + "," + (int)pulledMidY + ")");
        }
        
        System.out.println("All frames generated!");
    }
    
    static boolean isCyanish(int r, int g, int b) {
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float hue = hsb[0] * 360;
        float sat = hsb[1];
        float bri = hsb[2];
        // Broad cyan detection: hue 160-225, sat > 0.2, bri > 0.4
        // This catches both the bright cyan string and the semi-transparent checkered parts
        if (hue >= 160 && hue <= 225 && sat > 0.20 && bri > 0.35) {
            return true;
        }
        // Also catch light blue / white-blue
        if (b > 180 && g > 180 && r < 150 && sat > 0.1) {
            return true;
        }
        return false;
    }
}
