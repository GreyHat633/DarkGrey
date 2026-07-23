import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;

/**
 * Generate Itanis bow pulling animation frames.
 * 
 * Based on analysis of rainbow_bow and law_of_cycles pulling animations:
 * - The bow body shifts toward lower-right in each frame (simulating the player pulling it back)
 * - The bowstring mid-section gets pulled toward lower-right (V-shape)
 * - An arrow appears, nocked on the bowstring, also shifting with the pull
 * 
 * Itanis bow layout (720x720):
 * - Bow body: golden/dark arc from upper-left to lower-right  
 * - Bowstring: cyan/blue line from upper-right area to lower-left area
 * - The bowstring is currently too thick - we'll thin it during generation
 *
 * In MC 32x32, each pull frame shifts ~2px = 6.25% of the texture.
 * For 720px, that's ~45px per frame.
 */
public class GenerateItanisPulling {

    public static void main(String[] args) throws Exception {
        BufferedImage original = ImageIO.read(
            new File("src/main/resources/assets/dark_grey/textures/items/itanis.png"));
        BufferedImage arrowTex = ImageIO.read(
            new File("src/main/resources/assets/dark_grey/textures/entity/itanis_arrow.png"));
        
        int W = original.getWidth();  // 720
        int H = original.getHeight(); // 720
        
        System.out.println("Original: " + W + "x" + H);
        System.out.println("Arrow: " + arrowTex.getWidth() + "x" + arrowTex.getHeight());
        
        // Step 1: Separate bow body from bowstring
        BufferedImage bowBody = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        BufferedImage bowString = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int argb = original.getRGB(x, y);
                int a = (argb >> 24) & 0xff;
                if (a < 10) continue;
                
                int r = (argb >> 16) & 0xff;
                int g = (argb >> 8) & 0xff;
                int b = argb & 0xff;
                
                if (isBowstringColor(r, g, b)) {
                    bowString.setRGB(x, y, argb);
                } else {
                    bowBody.setRGB(x, y, argb);
                }
            }
        }
        
        // Step 2: Analyze bowstring geometry
        // Find the two endpoints (top and bottom) of the bowstring
        // The bowstring in itanis goes roughly from upper-right to lower-left
        int stringMinY = H, stringMaxY = 0;
        int stringTopX = 0, stringBotX = 0;
        
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int a = (bowString.getRGB(x, y) >> 24) & 0xff;
                if (a > 10) {
                    if (y < stringMinY) { stringMinY = y; stringTopX = x; }
                    if (y > stringMaxY) { stringMaxY = y; stringBotX = x; }
                }
            }
        }
        System.out.println("Bowstring top: (" + stringTopX + "," + stringMinY + ")");
        System.out.println("Bowstring bottom: (" + stringBotX + "," + stringMaxY + ")");
        
        // The bowstring midpoint
        double stringMidX = (stringTopX + stringBotX) / 2.0;
        double stringMidY = (stringMinY + stringMaxY) / 2.0;
        System.out.println("Bowstring midpoint: (" + stringMidX + "," + stringMidY + ")");
        
        // Step 3: Create a thinned version of the bowstring
        // The user says the string is too thick. We'll create a 1-pixel-wide center line
        // and redraw with a consistent thin width
        BufferedImage thinString = createThinBowstring(bowString, W, H, 
            stringTopX, stringMinY, stringBotX, stringMaxY);
        
        // Step 4: Prepare the arrow image  
        // Scale the arrow to fit on the bow (roughly 40% of bow height for the shaft)
        // The arrow should point from lower-right to upper-left (same diagonal as the bow)
        int arrowLength = (int)(W * 0.35); // arrow length
        BufferedImage scaledArrow = scaleAndRotateArrow(arrowTex, arrowLength);
        
        // Step 5: Generate each pulling frame
        // In vanilla MC (32x32), each frame shifts ~2px toward lower-right
        // That's 2/32 = 6.25% of texture size
        // For 720px: shift = 45px per frame
        double shiftPerFrame = W * 0.0625;
        
        for (int frame = 0; frame < 3; frame++) {
            double pullFactor = (frame + 1) / 3.0; // 0.33, 0.67, 1.0
            int shiftX = (int)(shiftPerFrame * (frame + 1)); // shift toward +X (right)
            int shiftY = (int)(shiftPerFrame * (frame + 1)); // shift toward +Y (down)
            
            System.out.println("Frame " + frame + ": shift=(" + shiftX + "," + shiftY + "), pullFactor=" + pullFactor);
            
            BufferedImage frameImg = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = frameImg.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw bow body shifted toward lower-right
            // But only a small portion of the shift - bow body moves less than arrow
            int bodyShiftX = (int)(shiftX * 0.15);
            int bodyShiftY = (int)(shiftY * 0.15);
            g.drawImage(bowBody, bodyShiftX, bodyShiftY, null);
            
            // Draw the pulled bowstring
            // The string endpoints stay at the bow tips (shifted with the body)
            // But the midpoint gets pulled toward lower-right proportional to pullFactor
            drawPulledBowstring(g, 
                stringTopX + bodyShiftX, stringMinY + bodyShiftY,  // top endpoint
                stringBotX + bodyShiftX, stringMaxY + bodyShiftY,  // bottom endpoint
                pullFactor, W);
            
            // Draw arrow on the bowstring
            // Arrow nock point is at the pulled midpoint of the bowstring
            double pulledMidX = stringMidX + bodyShiftX + shiftX * 0.7 * pullFactor;
            double pulledMidY = stringMidY + bodyShiftY + shiftY * 0.7 * pullFactor;
            drawArrow(g, scaledArrow, (int)pulledMidX, (int)pulledMidY, 
                stringTopX + bodyShiftX, stringMinY + bodyShiftY);
            
            g.dispose();
            
            String outPath = "src/main/resources/assets/dark_grey/textures/items/itanis_pulling_" + frame + ".png";
            ImageIO.write(frameImg, "png", new File(outPath));
            System.out.println("Saved: " + outPath);
        }
        
        System.out.println("Done!");
    }
    
    static boolean isBowstringColor(int r, int g, int b) {
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float hue = hsb[0] * 360;
        float sat = hsb[1];
        float bri = hsb[2];
        // Cyan range: hue ~180-220, or bright blue-white
        return (hue >= 160 && hue <= 220 && sat > 0.15 && bri > 0.4)
            || (b > 150 && g > 150 && r < g * 0.8 && bri > 0.6);
    }
    
    /**
     * Create a thin bowstring by finding the center line and drawing a thin stroke
     */
    static BufferedImage createThinBowstring(BufferedImage thick, int W, int H,
            int topX, int topY, int botX, int botY) {
        BufferedImage thin = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = thin.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw a thin line from top to bottom with a cyan glow
        // Main color from the original bowstring
        g.setColor(new Color(100, 220, 255, 200)); // bright cyan
        g.setStroke(new BasicStroke(Math.max(2, W / 180.0f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(topX, topY, botX, botY);
        
        // Add a subtle glow
        g.setColor(new Color(150, 240, 255, 80));
        g.setStroke(new BasicStroke(Math.max(4, W / 90.0f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(topX, topY, botX, botY);
        
        g.dispose();
        return thin;
    }
    
    /**
     * Draw a pulled bowstring - straight from each endpoint to a pulled midpoint,
     * forming a V shape.
     */
    static void drawPulledBowstring(Graphics2D g, 
            int topX, int topY, int botX, int botY,
            double pullFactor, int W) {
        
        // Calculate the pulled midpoint
        // The midpoint gets pulled toward the lower-right (toward the player)
        // In the itanis bow, that means the string bends "inward" toward the bow's concave side
        double midX = (topX + botX) / 2.0;
        double midY = (topY + botY) / 2.0;
        
        // Pull direction is perpendicular to the string, toward lower-right
        // String goes from upper-right to lower-left, so perpendicular toward lower-right is (+1, +1) normalized
        double pullDist = W * 0.12 * pullFactor; // maximum pull distance
        double pulledMidX = midX + pullDist * 0.707; // 45-degree pull toward lower-right
        double pulledMidY = midY + pullDist * 0.707;
        
        // Draw the V-shaped string
        // Main bright cyan line
        g.setColor(new Color(100, 220, 255, 220));
        float strokeWidth = Math.max(2, W / 240.0f);
        g.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(topX, topY, (int)pulledMidX, (int)pulledMidY);
        g.drawLine((int)pulledMidX, (int)pulledMidY, botX, botY);
        
        // Subtle glow
        g.setColor(new Color(150, 240, 255, 60));
        g.setStroke(new BasicStroke(strokeWidth * 2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(topX, topY, (int)pulledMidX, (int)pulledMidY);
        g.drawLine((int)pulledMidX, (int)pulledMidY, botX, botY);
    }
    
    /**
     * Scale the arrow texture and rotate it to point from lower-right to upper-left
     * (matching the bow's diagonal orientation).
     */
    static BufferedImage scaleAndRotateArrow(BufferedImage arrowTex, int targetLength) {
        int aw = arrowTex.getWidth();
        int ah = arrowTex.getHeight();
        
        // The arrow texture is a diagonal line from lower-left to upper-right
        // We need to scale it to targetLength
        double diag = Math.sqrt(aw * aw + ah * ah);
        double scale = targetLength / diag;
        
        int newW = (int)(aw * scale);
        int newH = (int)(ah * scale);
        
        BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(arrowTex, 0, 0, newW, newH, null);
        g.dispose();
        
        return scaled;
    }
    
    /**
     * Draw the arrow on the frame with its nock at the pulled midpoint
     */
    static void drawArrow(Graphics2D g, BufferedImage arrow, int nockX, int nockY,
            int tipDirX, int tipDirY) {
        // The arrow should point from the nock toward the upper-left (where it will fly)
        // Place the arrow such that its lower-right corner is at the nock point
        int drawX = nockX - arrow.getWidth();
        int drawY = nockY - arrow.getHeight();
        
        g.drawImage(arrow, drawX, drawY, null);
    }
}
