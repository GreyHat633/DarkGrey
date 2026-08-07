import os

path = 'src/main/java/com/greyhat/dark_grey/client/render/MeteorRenderHandler.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

search1 = '''        GL11.glPushMatrix();
        GL11.glTranslated(-renderPosX, -renderPosY, -renderPosZ);
        GL11.glDisable(GL11.GL_TEXTURE_2D);'''

replace1 = '''        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LINE_BIT);
        GL11.glTranslated(-renderPosX, -renderPosY, -renderPosZ);
        GL11.glDisable(GL11.GL_TEXTURE_2D);'''

search2 = '''        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();'''

replace2 = '''        GL11.glPopAttrib();
        GL11.glPopMatrix();'''

if search1 in content and search2 in content:
    content = content.replace(search1, replace1)
    content = content.replace(search2, replace2)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Fixed OpenGL state issues.")
else:
    print("Could not find patterns.")
