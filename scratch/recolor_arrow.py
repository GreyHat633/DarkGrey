import sys
from PIL import Image

def recolor_arrow():
    img_path = r'e:\Java\MinecraftMod\DarkGrey\src\main\resources\assets\dark_grey\textures\entity\itanis_arrow.png'
    img = Image.open(img_path).convert('RGBA')
    width, height = img.size
    
    # We want: main body black, small amounts of gold and white.
    # Current arrow is mostly blue/cyan.
    # Light blues -> White or Gold
    # Mid blues -> Dark Grey / Black
    # Dark blues -> Black
    
    pixels = img.load()
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if a > 0:
                # Calculate lightness
                lightness = (r + g + b) / (3.0 * 255.0)
                
                # If it's a very light pixel (like the glowing edges or tip), make it Gold or White
                if lightness > 0.8:
                    # White
                    pixels[x, y] = (255, 255, 255, a)
                elif lightness > 0.6:
                    # Gold
                    pixels[x, y] = (255, 215, 0, a)
                elif lightness > 0.4:
                    # Dark Gold
                    pixels[x, y] = (184, 134, 11, a)
                else:
                    # Black / Very dark grey for the main body
                    val = int(lightness * 255) // 2
                    pixels[x, y] = (val, val, val, a)
                    
    img.save(img_path)
    print("Recolored successfully!")

if __name__ == "__main__":
    recolor_arrow()
