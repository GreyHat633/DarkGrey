import os

path = 'src/main/java/com/greyhat/dark_grey/common/ClientProxy.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

search = '''double distSq = (px - message.x)*(px - message.x) + (py - message.y)*(py - message.y) + (pz - message.z)*(pz - message.z);'''

replace = '''if (message.type == 2) {
                        int num = 40;
                        for (int i=0; i<num; i++) {
                            double t = i / (double)num;
                            double px = message.x + (message.x2 - message.x)*t + (world.rand.nextDouble()-0.5)*1.5;
                            double py = message.y + (message.y2 - message.y)*t + (world.rand.nextDouble()*0.5);
                            double pz = message.z + (message.z2 - message.z)*t + (world.rand.nextDouble()-0.5)*1.5;
                            world.spawnParticle("flame", px, py, pz, 0, 0.05, 0);
                            world.spawnParticle("lava", px, py, pz, 0, 0, 0);
                            world.spawnParticle("crit", px, py, pz, 0, 0, 0);
                        }
                        return;
                    }
                    double r = message.radius;
                    for (int i = 0; i < 200; i++) {
                        double px = message.x + (world.rand.nextDouble() - 0.5) * r * 2.0;
                        double py = message.y + (world.rand.nextDouble() - 0.5) * r * 2.0;
                        double pz = message.z + (world.rand.nextDouble() - 0.5) * r * 2.0;
                        double distSq = (px - message.x)*(px - message.x) + (py - message.y)*(py - message.y) + (pz - message.z)*(pz - message.z);'''

search2 = 'double r = message.radius;\n                    for (int i = 0; i < 200; i++) {\n                        double px = message.x + (world.rand.nextDouble() - 0.5) * r * 2.0;\n                        double py = message.y + (world.rand.nextDouble() - 0.5) * r * 2.0;\n                        double pz = message.z + (world.rand.nextDouble() - 0.5) * r * 2.0;\n                        double distSq = (px - message.x)*(px - message.x) + (py - message.y)*(py - message.y) + (pz - message.z)*(pz - message.z);'

if search2 in content:
    content = content.replace(search2, replace)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
