import os

def replace_in_file(path, search, replace):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    if search in content:
        content = content.replace(search, replace)
        with open(path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Fixed {path}")
    else:
        print(f"Search string not found in {path}")

replace_in_file(
    'src/main/java/com/greyhat/dark_grey/entity/EntityMeteorFireTrail.java',
    'scorch.applyMark(target, owner);',
    'com.greyhat.dark_grey.mark.MarkManager.apply(target, com.greyhat.dark_grey.mark.type.ScorchMarkType.ID, 1, owner);'
)

replace_in_file(
    'src/main/java/com/greyhat/dark_grey/event/MeteorFlightTracker.java',
    'scorch.applyMark(target, player);',
    'com.greyhat.dark_grey.mark.MarkManager.apply(target, com.greyhat.dark_grey.mark.type.ScorchMarkType.ID, 1, player);'
)

comp_path = 'src/main/java/com/greyhat/dark_grey/component/ComponentMeteor.java'
with open(comp_path, 'r', encoding='utf-8') as f:
    comp_content = f.read()
comp_content = comp_content.replace('import com.greyhat.dark_grey.api.capability.IAfterHitEntity;', 'import com.greyhat.dark_grey.api.capability.IOnHit;')
comp_content = comp_content.replace('IAfterHitEntity', 'IOnHit')
comp_content = comp_content.replace('public void afterHitEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target, float damageAmount)', 'public void onHit(ItemStack stack, EntityLivingBase player, EntityLivingBase target, float damageAmount)')
comp_content = comp_content.replace('player.worldObj.isRemote', '((Entity)player).worldObj.isRemote')
with open(comp_path, 'w', encoding='utf-8') as f:
    f.write(comp_content)
print("Fixed ComponentMeteor.java")

rend_path = 'src/main/java/com/greyhat/dark_grey/client/render/MeteorRenderHandler.java'
rend_search = 'com.greyhat.dark_grey.api.IRPGComponent comp = RPGItemDataManager.getInstance().getComponent(stack);\n        if (!(comp instanceof ComponentMeteor)) return;'
rend_replace = '''if (!(stack.getItem() instanceof com.greyhat.dark_grey.api.IRPGItemContainer)) return;
        com.greyhat.dark_grey.api.IRPGItemContainer container = (com.greyhat.dark_grey.api.IRPGItemContainer) stack.getItem();
        boolean hasMeteor = false;
        for (com.greyhat.dark_grey.api.IRPGComponent component : container.getAllComponents()) {
            if (component instanceof ComponentMeteor) {
                hasMeteor = true;
                break;
            }
        }
        if (!hasMeteor) return;'''
replace_in_file(rend_path, rend_search, rend_replace)
