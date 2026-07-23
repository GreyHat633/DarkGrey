import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class InspectArrowDirection {
    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File("src/main/resources/assets/dark_grey/textures/entity/itanis_arrow.png"));
        int w = img.getWidth();
        int h = img.getHeight();
        
        long tl = 0, tr = 0, bl = 0, br = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = (img.getRGB(x, y) >> 24) & 0xff;
                if (a > 50) {
                    if (x < w/2 && y < h/2) tl++;
                    if (x >= w/2 && y < h/2) tr++;
                    if (x < w/2 && y >= h/2) bl++;
                    if (x >= w/2 && y >= h/2) br++;
                }
            }
        }
        System.out.println("Top-Left: " + tl);
        System.out.println("Top-Right: " + tr);
        System.out.println("Bottom-Left: " + bl);
        System.out.println("Bottom-Right: " + br);
    }
}
