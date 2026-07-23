package scratch;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class DeformBow {
    public static void main(String[] args) throws Exception {
        File inputFile = new File("src/main/resources/assets/dark_grey/textures/items/itanis.png");
        if (!inputFile.exists()) {
            System.out.println("Image not found at: " + inputFile.getAbsolutePath());
            return;
        }
        BufferedImage img = ImageIO.read(inputFile);
        int w = img.getWidth();
        int h = img.getHeight();
        
        System.out.println("Processing image size: " + w + "x" + h);
        
        for (int frame = 0; frame < 3; frame++) {
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            double pullAmount = (frame + 1) * 0.20; // 20%, 40%, 60% intensity
            
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    double cx = w / 2.0;
                    double cy = h / 2.0;
                    
                    double dx = x - cx;
                    double dy = y - cy;
                    double dist = Math.sqrt(dx*dx + dy*dy);
                    
                    // Gaussian weight centered on the middle of the image
                    double radius = w * 0.4;
                    double factor = Math.exp(- (dist * dist) / (radius * radius));
                    
                    // We pull towards bottom-right. For the bow facing Top-Right to Bottom-Left, 
                    // pulling the string means pulling the center towards bottom-right.
                    double shiftX = pullAmount * (w * 0.1) * factor;
                    double shiftY = pullAmount * (h * 0.1) * factor;
                    
                    int srcX = (int) Math.round(x - shiftX);
                    int srcY = (int) Math.round(y - shiftY);
                    
                    if (srcX >= 0 && srcX < w && srcY >= 0 && srcY < h) {
                        out.setRGB(x, y, img.getRGB(srcX, srcY));
                    }
                }
            }
            File outFile = new File("src/main/resources/assets/dark_grey/textures/items/itanis_pulling_" + frame + ".png");
            ImageIO.write(out, "png", outFile);
            System.out.println("Saved " + outFile.getName());
        }
    }
}
