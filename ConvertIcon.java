import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ConvertIcon {
    public static void main(String[] args) {
        try {
            String inputPath = "C:/Users/GreyHat/.gemini/antigravity/brain/4deda908-1fff-43a6-86f4-e22f1d270c2a/underground_sun_icon_1784831731399.jpg";
            String outputPath = "E:/Java/MinecraftMod/DarkGrey/src/main/resources/assets/dark_grey/textures/items/underground_sun.png";
            
            File inputFile = new File(inputPath);
            if (!inputFile.exists()) {
                System.out.println("Input not found: " + inputPath);
                System.exit(1);
            }
            
            BufferedImage img = ImageIO.read(inputFile);
            if (img == null) {
                System.out.println("Failed to read image.");
                System.exit(1);
            }
            
            Image scaled = img.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
            BufferedImage resized = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2d = resized.createGraphics();
            g2d.drawImage(scaled, 0, 0, null);
            g2d.dispose();
            
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 32; x++) {
                    int argb = resized.getRGB(x, y);
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    
                    int lum = Math.max(r, Math.max(g, b));
                    
                    if (lum < 10) {
                        resized.setRGB(x, y, 0); 
                    } else {
                        int newArgb = (lum << 24) | (r << 16) | (g << 8) | b;
                        resized.setRGB(x, y, newArgb);
                    }
                }
            }
            
            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs();
            ImageIO.write(resized, "png", outputFile);
            System.out.println("Item texture generated.");
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
