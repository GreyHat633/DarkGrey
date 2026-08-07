import os

files = [
    'src/main/java/com/greyhat/dark_grey/component/ComponentMeteor.java',
    'src/main/java/com/greyhat/dark_grey/event/MeteorFlightTracker.java',
    'src/main/java/com/greyhat/dark_grey/network/MeteorExplosionMessage.java'
]

for file_path in files:
    with open(file_path, 'rb') as f:
        content = f.read()
    if content.startswith(b'\xef\xbb\xbf'):
        content = content[3:]
        with open(file_path, 'wb') as f:
            f.write(content)
        print(f"Removed BOM from {file_path}")
