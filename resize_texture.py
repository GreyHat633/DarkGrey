import sys
sys.path.insert(0, r"E:\Java\VibeCoding\ATTACH")
from PIL import Image

try:
    img = Image.open('E:\\Java\\MinecraftMod\\RPGItem\\图片2.png')
    img = img.resize((32, 32), Image.Resampling.NEAREST)
    img.save('E:\\Java\\MinecraftMod\\DarkGrey\\src\\main\\resources\\assets\\dark_grey\\textures\\items\\meteor.png')
    print("Texture resized successfully.")
except Exception as e:
    print(f"Error: {e}")
