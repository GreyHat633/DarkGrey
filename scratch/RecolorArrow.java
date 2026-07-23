import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class RecolorArrow {
    public static void main(String[] args) throws Exception {
        File file = new File("src/main/resources/assets/dark_grey/textures/entity/itanis_arrow.png");
        BufferedImage img = ImageIO.read(file);
        
        int width = img.getWidth();
        int height = img.getHeight();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = img.getRGB(x, y);
                int a = (argb >> 24) & 0xff;
                int r = (argb >> 16) & 0xff;
                int g = (argb >> 8) & 0xff;
                int b = argb & 0xff;
                
                if (a > 0) {
                    double lightness = (r + g + b) / (3.0 * 255.0);
                    
                    int newR, newG, newB;
                    if (lightness > 0.8) {
                        // Brilliant White/Gold Highlight
                        newR = 255; newG = 255; newB = 220;
                    } else if (lightness > 0.6) {
                        // Pure Gold
                        newR = 255; newG = 215; newB = 0;
                    } else if (lightness > 0.4) {
                        // Goldenrod
                        newR = 218; newG = 165; newB = 32;
                    } else {
                        // Dark Gold
                        newR = 184; newG = 134; newB = 11;
                    }
                    
                    int newArgb = (a << 24) | (newR << 16) | (newG << 8) | newB;
                    img.setRGB(x, y, newArgb);
                }
            }
        }
        
        ImageIO.write(img, "png", file);
        System.out.println("Recolored successfully to pure gold!");
    }
}
