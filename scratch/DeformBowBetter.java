package scratch;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class DeformBowBetter {
    
    // Bilinear interpolation for smoother edges
    private static int getInterpolatedPixel(BufferedImage img, double x, double y) {
        int w = img.getWidth();
        int h = img.getHeight();
        
        int x1 = (int) Math.floor(x);
        int y1 = (int) Math.floor(y);
        int x2 = x1 + 1;
        int y2 = y1 + 1;
        
        if (x1 < 0 || x2 >= w || y1 < 0 || y2 >= h) return 0; // transparent
        
        double xDiff = x - x1;
        double yDiff = y - y1;
        
        int p00 = img.getRGB(x1, y1);
        int p10 = img.getRGB(x2, y1);
        int p01 = img.getRGB(x1, y2);
        int p11 = img.getRGB(x2, y2);
        
        double a00 = (p00 >> 24) & 0xff;
        double r00 = (p00 >> 16) & 0xff;
        double g00 = (p00 >> 8) & 0xff;
        double b00 = (p00) & 0xff;
        
        double a10 = (p10 >> 24) & 0xff;
        double r10 = (p10 >> 16) & 0xff;
        double g10 = (p10 >> 8) & 0xff;
        double b10 = (p10) & 0xff;
        
        double a01 = (p01 >> 24) & 0xff;
        double r01 = (p01 >> 16) & 0xff;
        double g01 = (p01 >> 8) & 0xff;
        double b01 = (p01) & 0xff;
        
        double a11 = (p11 >> 24) & 0xff;
        double r11 = (p11 >> 16) & 0xff;
        double g11 = (p11 >> 8) & 0xff;
        double b11 = (p11) & 0xff;
        
        double a = a00*(1-xDiff)*(1-yDiff) + a10*xDiff*(1-yDiff) + a01*(1-xDiff)*yDiff + a11*xDiff*yDiff;
        double r = r00*(1-xDiff)*(1-yDiff) + r10*xDiff*(1-yDiff) + r01*(1-xDiff)*yDiff + r11*xDiff*yDiff;
        double g = g00*(1-xDiff)*(1-yDiff) + g10*xDiff*(1-yDiff) + g01*(1-xDiff)*yDiff + g11*xDiff*yDiff;
        double b = b00*(1-xDiff)*(1-yDiff) + b10*xDiff*(1-yDiff) + b01*(1-xDiff)*yDiff + b11*xDiff*yDiff;
        
        return (((int)a) << 24) | (((int)r) << 16) | (((int)g) << 8) | ((int)b);
    }

    public static void main(String[] args) throws Exception {
        File inputFile = new File("src/main/resources/assets/dark_grey/textures/items/itanis.png");
        File arrowFile = new File("src/main/resources/assets/dark_grey/textures/entity/itanis_arrow.png");
        if (!inputFile.exists() || !arrowFile.exists()) {
            System.out.println("Image not found");
            return;
        }
        BufferedImage img = ImageIO.read(inputFile);
        BufferedImage arrowImg = ImageIO.read(arrowFile);
        int w = img.getWidth();
        int h = img.getHeight();
        
        for (int frame = 0; frame < 3; frame++) {
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            double pullFactor = (frame == 0) ? 0.3 : (frame == 1) ? 0.6 : 0.9;
            double maxShift = w * 0.15 * pullFactor; 
            
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    // We want to pull the string towards bottom-right (w, h)
                    // The string is around the line x + y = w
                    // Center of the string is (w/2, h/2)
                    // The bow body is around x + y = w/2 (closer to top-left)
                    // We need a deformation field.
                    
                    double projDiagonal = (x + y) / Math.sqrt(2); 
                    double projString = (x - y) / Math.sqrt(2);   
                    
                    // String pulling: maximum at the center of the string
                    double stringCenterDiag = w / Math.sqrt(2);
                    double distDiag = Math.abs(projDiagonal - stringCenterDiag);
                    double distStr = Math.abs(projString);
                    
                    // Weight for the string center
                    double stringWeight = Math.exp(-(distDiag * distDiag) / (w * w * 0.05)) * 
                                          Math.exp(-(distStr * distStr) / (w * w * 0.1));
                    
                    // Shift the string
                    double shiftX = maxShift * stringWeight;
                    double shiftY = maxShift * stringWeight;
                    
                    // Bow limbs bending: tips are at (w, 0) and (0, h)
                    // They should bend towards the bottom-right as well
                    // Tip weight
                    double tipWeight = Math.exp(-(projDiagonal * projDiagonal) / (w * w * 0.15));
                    double tipBending = w * 0.05 * pullFactor * tipWeight;
                    shiftX += tipBending;
                    shiftY += tipBending;
                    
                    // Ensure the handle (top-left) doesn't move
                    double handleWeight = Math.exp(-((x - w/4.0)*(x - w/4.0) + (y - h/4.0)*(y - h/4.0)) / (w * w * 0.05));
                    shiftX *= (1 - handleWeight);
                    shiftY *= (1 - handleWeight);
                    
                    double srcX = x - shiftX;
                    double srcY = y - shiftY;

                    int pixel = getInterpolatedPixel(img, srcX, srcY);
                    if ((pixel >>> 24) > 10) { // threshold alpha
                        out.setRGB(x, y, pixel);
                    }
                }
            }
            
            // Overlay arrow!
            int arrowShift = (int)((maxShift + w * 0.05) * 1.5);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int sx = x - arrowShift;
                    int sy = y - arrowShift;
                    if (sx >= 0 && sx < arrowImg.getWidth() && sy >= 0 && sy < arrowImg.getHeight()) {
                        int arrowPixel = arrowImg.getRGB(sx, sy);
                        int aa = (arrowPixel >> 24) & 0xff;
                        if (aa > 128) {
                            out.setRGB(x, y, arrowPixel);
                        }
                    }
                }
            }
            
            File outFile = new File("src/main/resources/assets/dark_grey/textures/items/itanis_pulling_" + frame + ".png");
            ImageIO.write(out, "png", outFile);
            System.out.println("Saved " + outFile.getName());
        }
    }
}
