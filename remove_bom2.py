import os

file_path = 'src/main/java/com/greyhat/dark_grey/network/MeteorExplosionMessage.java'
with open(file_path, 'rb') as f:
    content = f.read()
if content.startswith(b'\xef\xbb\xbf'):
    content = content[3:]
    with open(file_path, 'wb') as f:
        f.write(content)
    print("Removed BOM.")
else:
    print("No BOM found.")
