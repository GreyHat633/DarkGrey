import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;

public class GenerateItanis32x32 {
    public static void main(String[] args) throws Exception {
        BufferedImage original = ImageIO.read(
            new File("src/main/resources/assets/dark_grey/textures/items/itanis.png"));
        
        // 1. Scale original to 32x32
        BufferedImage scaled = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(original, 0, 0, 32, 32, null);
        g2.dispose();
        
        // 2. Clean up scaled image (remove faint alpha and cyan string)
        BufferedImage clean32 = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        int topX = -1, topY = 32, botX = -1, botY = -1;
        
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                int argb = scaled.getRGB(x, y);
                int a = (argb >> 24) & 0xff;
                int r = (argb >> 16) & 0xff;
                int g = (argb >> 8) & 0xff;
                int b = argb & 0xff;
                
                // Threshold alpha to make it crisp pixel art
                if (a < 100) continue; 
                
                // Remove the cyan string (approximate colors)
                float[] hsb = Color.RGBtoHSB(r, g, b, null);
                if (hsb[0] > 0.4f && hsb[0] < 0.6f && hsb[1] > 0.3f && hsb[2] > 0.4f) {
                    // It's cyan string, skip
                    continue;
                }
                
                clean32.setRGB(x, y, (255 << 24) | (r << 16) | (g << 8) | b);
                
                if (y < topY) { topY = y; topX = x; }
                if (y > botY) { botY = y; botX = x; }
            }
        }
        
        // Save the cleaned 32x32 base bow
        ImageIO.write(clean32, "png", new File("src/main/resources/assets/dark_grey/textures/items/itanis.png"));
        System.out.println("Saved 32x32 itanis.png. Top=(" + topX + "," + topY + ") Bot=(" + botX + "," + botY + ")");
        
        double lineDx = botX - topX;
        double lineDy = botY - topY;
        double lineLen = Math.sqrt(lineDx * lineDx + lineDy * lineDy);
        double perpX = lineDy / lineLen;
        double perpY = -lineDx / lineLen;
        if (perpX + perpY < 0) { perpX = -perpX; perpY = -perpY; }
        
        double maxPullDist = 32 * 0.35; // 11 pixels pull
        
        for (int frame = 0; frame < 3; frame++) {
            BufferedImage out = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            // CRISP pixel art drawing
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            
            g.drawImage(clean32, 0, 0, null);
            
            double pullFactor = (frame + 1) / 3.0;
            double pullDist = maxPullDist * pullFactor;
            
            double midX = (topX + botX) / 2.0;
            double midY = (topY + botY) / 2.0;
            double pulledMidX = midX + perpX * pullDist;
            double pulledMidY = midY + perpY * pullDist;
            
            // Draw string
            g.setColor(new Color(0, 255, 255, 255));
            g.setStroke(new BasicStroke(1.0f)); // EXACTLY 1 pixel thick!
            g.drawLine(topX, topY, (int)pulledMidX, (int)pulledMidY);
            g.drawLine((int)pulledMidX, (int)pulledMidY, botX, botY);
            
            // Draw arrow
            // For 32x32, arrow is just a line of pixels pointing upper-left
            int arrowLen = 14;
            int tipX = (int)pulledMidX - arrowLen;
            int tipY = (int)pulledMidY - arrowLen;
            
            // Shaft
            g.setColor(Color.WHITE);
            g.drawLine((int)pulledMidX, (int)pulledMidY, tipX, tipY);
            // Arrowhead (2 pixels)
            g.setColor(new Color(200, 200, 200));
            g.fillRect(tipX, tipY, 2, 2);
            // Fletching (blue/cyan)
            g.setColor(new Color(0, 150, 255));
            g.drawLine((int)pulledMidX, (int)pulledMidY, (int)pulledMidX + 1, (int)pulledMidY - 1);
            g.drawLine((int)pulledMidX, (int)pulledMidY, (int)pulledMidX - 1, (int)pulledMidY + 1);
            
            g.dispose();
            
            String outPath = "src/main/resources/assets/dark_grey/textures/items/itanis_pulling_" + frame + ".png";
            ImageIO.write(out, "png", new File(outPath));
            System.out.println("Frame " + frame + " generated.");
        }
    }
}
