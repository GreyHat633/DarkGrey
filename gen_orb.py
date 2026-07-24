from PIL import Image, ImageDraw, ImageFilter
import math

def create_orb(size, glow_radius, center_color, edge_color, filepath):
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    center = size // 2

    # Draw glow
    for i in range(glow_radius, 0, -1):
        alpha = int(255 * (1 - i / glow_radius)**2)
        if alpha > 0:
            color = (edge_color[0], edge_color[1], edge_color[2], alpha)
            draw.ellipse(
                (center - i, center - i, center + i, center + i),
                fill=color
            )
    
    # Draw core
    core_radius = int(glow_radius * 0.4)
    for i in range(core_radius, 0, -1):
        alpha = int(255 * (1 - i / core_radius))
        r = int(center_color[0] * alpha/255 + edge_color[0] * (255-alpha)/255)
        g = int(center_color[1] * alpha/255 + edge_color[1] * (255-alpha)/255)
        b = int(center_color[2] * alpha/255 + edge_color[2] * (255-alpha)/255)
        draw.ellipse(
            (center - i, center - i, center + i, center + i),
            fill=(r, g, b, 255)
        )

    # Blur a bit to smooth
    img = img.filter(ImageFilter.GaussianBlur(1))
    img.save(filepath)

# 32x32 item texture
create_orb(32, 14, (255, 255, 255), (255, 150, 0), 'E:/Java/MinecraftMod/DarkGrey/src/main/resources/assets/dark_grey/textures/items/underground_sun.png')

# 64x64 entity texture
create_orb(64, 28, (255, 255, 255), (255, 100, 0), 'E:/Java/MinecraftMod/DarkGrey/src/main/resources/assets/dark_grey/textures/entity/underground_sun_orb.png')
print('Images generated.')
