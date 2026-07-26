import sys
from PIL import Image

input_path = "C:/Users/GreyHat/.gemini/antigravity/brain/dee0fb31-a186-4065-a0d9-1a998cb9bbc5/shattered_bone_1785071848409.jpg"
output_path = "e:/Java/MinecraftMod/DarkGrey/src/main/resources/assets/dark_grey/textures/gui/marks/shattered_bone.png"

img = Image.open(input_path).convert("RGBA")
img = img.resize((32, 32), Image.Resampling.NEAREST)

data = img.getdata()
new_data = []

for item in data:
    r, g, b, a = item
    if g > 150 and r < 100 and b < 100:
        new_data.append((255, 255, 255, 0))
    elif g > r * 1.5 and g > b * 1.5:
        new_data.append((255, 255, 255, 0))
    else:
        new_data.append(item)

img.putdata(new_data)
import os
os.makedirs(os.path.dirname(output_path), exist_ok=True)
img.save(output_path, "PNG")
print("Saved transparent shattered bone icon.")
