import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class MakeTexture {
    public static void main(String[] args) throws Exception {
        File inputFile = new File("E:/Java/MinecraftMod/DarkGrey/src/main/resources/assets/dark_grey/textures/items/judgment_dice.png");
        BufferedImage img = ImageIO.read(inputFile);
        
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgba = img.getRGB(x, y);
                int a = (rgba >> 24) & 0xff;
                if (a > 0) {
                    int r = (rgba >> 16) & 0xff;
                    int g = (rgba >> 8) & 0xff;
                    int b = rgba & 0xff;
                    
                    int lum = (int)(0.299 * r + 0.587 * g + 0.114 * b);
                    int newR = Math.min(255, (int)(lum * 1.8));
                    int newG = (int)(lum * 0.2);
                    int newB = (int)(lum * 0.2);
                    
                    img.setRGB(x, y, (a << 24) | (newR << 16) | (newG << 8) | newB);
                }
            }
        }
        
        File outputFile = new File("E:/Java/MinecraftMod/DarkGrey/src/main/resources/assets/dark_grey/textures/items/death_judgment_dice.png");
        ImageIO.write(img, "png", outputFile);
        System.out.println("Success");
    }
}
