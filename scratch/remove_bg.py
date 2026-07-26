import sys
sys.path.insert(0, r"E:\Java\VibeCoding\ATTACH")
try:
    from PIL import Image
except ImportError:
    print("Pillow not installed")
    sys.exit(1)
import os

def remove_white_bg(input_path, output_path):
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    img = Image.open(input_path).convert("RGBA")
    datas = img.getdata()

    new_data = []
    # threshold for white
    for item in datas:
        # change all white (also shades of whites)
        # to transparent
        if item[0] > 230 and item[1] > 230 and item[2] > 230:
            new_data.append((255, 255, 255, 0))
        else:
            new_data.append(item)

    img.putdata(new_data)
    
    # Resize for Minecraft
    img = img.resize((256, 256), Image.Resampling.LANCZOS)
    img.save(output_path, "PNG")

if __name__ == "__main__":
    remove_white_bg(sys.argv[1], sys.argv[2])
