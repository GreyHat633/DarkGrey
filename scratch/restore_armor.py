import re

content = """1: package com.greyhat.dark_grey.item;
2: 
3: import com.greyhat.dark_grey.api.IRPGComponent;
4: import com.greyhat.dark_grey.api.IRPGItemContainer;
5: import com.greyhat.dark_grey.api.RPGItemDataManager;
6: import com.greyhat.dark_grey.api.capability.IAttributeModifier;
7: import com.greyhat.dark_grey.api.capability.IHasTooltip;
8: import com.greyhat.dark_grey.api.capability.IOnEquip;
9: import com.greyhat.dark_grey.api.capability.IOnHit;
10: import com.greyhat.dark_grey.api.capability.IOnHurt;
11: import com.greyhat.dark_grey.api.capability.IOnPlayerDeath;
12: import com.greyhat.dark_grey.api.capability.IOnUnequip;
13: import com.greyhat.dark_grey.api.capability.IOnWornTick;
14: 
15: import com.google.common.collect.Multimap;
16: import cpw.mods.fml.common.FMLLog;
17: import cpw.mods.fml.relauncher.Side;
18: import cpw.mods.fml.relauncher.SideOnly;
19: import net.minecraft.creativetab.CreativeTabs;
20: import net.minecraft.enchantment.Enchantment;
21: import net.minecraft.entity.ai.attributes.AttributeModifier;
22: import net.minecraft.entity.player.EntityPlayer;
23: import net.minecraft.item.Item;
24: import net.minecraft.item.ItemArmor;
25: import net.minecraft.item.ItemStack;
26: import net.minecraft.nbt.NBTTagCompound;
27: import net.minecraft.nbt.NBTTagList;
28: import net.minecraft.world.World;
29: 
30: import java.util.Collections;
31: import java.util.HashMap;
32: import java.util.List;
33: import java.util.Map;
34: 
35: public class ItemRPGArmor extends ItemArmor implements IRPGItemContainer {
36: 
37:     private final String rpgItemId;
38:     public static final String NBT_TRACKER_TAG = "DarkGreyRPG_ExcelEnchants";
39:     public static final String NBT_VERSION_TAG = "DarkGreyRPG_DataVersion";
40: 
41:     private List<IRPGComponent> allComponents;
42:     private List<IOnWornTick> wornTickHandlers;
43:     private List<IOnHurt> hurtHandlers;
44:     private List<IOnEquip> equipHandlers;
45:     private List<IOnUnequip> unequipHandlers;
46:     private List<IHasTooltip> tooltipHandlers;
47:     private List<IAttributeModifier> attributeHandlers;
48:     private List<IOnPlayerDeath> playerDeathHandlers;
49:     private List<IOnHit> hitHandlers;
50: 
51:     public ItemRPGArmor(String id, ArmorMaterial material, int renderIndex, int armorType,
52:                         List<IRPGComponent> components) {
53:         super(material, renderIndex, armorType);
54:         this.rpgItemId = id;
55:         this.allComponents = Collections.unmodifiableList(components);
56:         this.wornTickHandlers = IRPGComponent.filterByCapability(components, IOnWornTick.class);
57:         this.hurtHandlers = IRPGComponent.filterByCapability(components, IOnHurt.class);
58:         this.equipHandlers = IRPGComponent.filterByCapability(components, IOnEquip.class);
59:         this.unequipHandlers = IRPGComponent.filterByCapability(components, IOnUnequip.class);
60:         this.tooltipHandlers = IRPGComponent.filterByCapability(components, IHasTooltip.class);
61:         this.attributeHandlers = IRPGComponent.filterByCapability(components, IAttributeModifier.class);
62:         this.playerDeathHandlers = IRPGComponent.filterByCapability(components, IOnPlayerDeath.class);
63:         this.hitHandlers = IRPGComponent.filterByCapability(components, IOnHit.class);
64:     }
65: 
66:     @Override
67:     public String getRpgItemId() {
68:         return rpgItemId;
69:     }
70: 
71:     @Override
72:     public String getItemStackDisplayName(ItemStack stack) {
73:         RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance().getConfig(this.rpgItemId);
74:         if (config != null && config.displayName != null && !config.displayName.isEmpty()) {
75:             return config.displayName;
76:         }
77:         return super.getItemStackDisplayName(stack);
78:     }
79: 
80:     @Override
81:     public void rebuildComponents() {
82:         RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance().getConfig(rpgItemId);
83:         if (config == null || config.componentsJson == null) return;
84: 
85:         List<IRPGComponent> newComponents = new java.util.ArrayList<>();
86:         for (com.google.gson.JsonElement compElem : config.componentsJson) {
87:             com.google.gson.JsonObject compObj = compElem.getAsJsonObject();
88:             String compName = compObj.get("name").getAsString();
89:             com.google.gson.JsonObject params = compObj.has("params") ? compObj.getAsJsonObject("params") : new com.google.gson.JsonObject();
90:             try {
91:                 newComponents.add(com.greyhat.dark_grey.api.ComponentRegistry.create(compName, params));
92:             } catch (Exception e) {}
93:         }
94: 
95:         this.allComponents = Collections.unmodifiableList(newComponents);
96:         this.wornTickHandlers = IRPGComponent.filterByCapability(newComponents, IOnWornTick.class);
97:         this.hurtHandlers = IRPGComponent.filterByCapability(newComponents, IOnHurt.class);
98:         this.equipHandlers = IRPGComponent.filterByCapability(newComponents, IOnEquip.class);
99:         this.unequipHandlers = IRPGComponent.filterByCapability(newComponents, IOnUnequip.class);
100:         this.tooltipHandlers = IRPGComponent.filterByCapability(newComponents, IHasTooltip.class);
101:         this.attributeHandlers = IRPGComponent.filterByCapability(newComponents, IAttributeModifier.class);
102:         this.playerDeathHandlers = IRPGComponent.filterByCapability(newComponents, IOnPlayerDeath.class);
103:         this.hitHandlers = IRPGComponent.filterByCapability(newComponents, IOnHit.class);
104:     }
105: 
106:     @Override
107:     public void onArmorTick(World world, EntityPlayer player, ItemStack armorStack) {
108:         if (world.isRemote) return;
109:         if (world.getTotalWorldTime() % 20 != 0) return;
110:         for (IOnWornTick handler : wornTickHandlers) {
111:             handler.onWornTick(world, player, armorStack);
112:         }
113:     }
114: 
115:     @Override
116:     @SideOnly(Side.CLIENT)
117:     @SuppressWarnings("unchecked")
118:     public void addInformation(ItemStack armorStack, EntityPlayer player, List tooltipLines, boolean showAdvanced) {
119:         super.addInformation(armorStack, player, tooltipLines, showAdvanced);
120:         for (IHasTooltip handler : tooltipHandlers) {
121:             handler.addTooltipLines(armorStack, player, tooltipLines, showAdvanced);
122:         }
123:     }
124: 
125:     @Override
126:     @SuppressWarnings("unchecked")
127:     public Multimap getItemAttributeModifiers() {
128:         Multimap<String, AttributeModifier> attributeMap = super.getItemAttributeModifiers();
129:         for (IAttributeModifier handler : attributeHandlers) {
130:             handler.modifyAttributes(attributeMap);
131:         }
132:         return attributeMap;
133:     }
134: 
135:     @Override
136:     public int getMaxDamage(ItemStack stack) {
137:         RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance().getConfig(rpgItemId);
138:         if (config != null && config.durability > 0) {
139:             return config.durability;
140:         }
141:         return super.getMaxDamage(stack);
142:     }
143: 
144:     @Override
145:     public void onUpdate(ItemStack stack, World world, net.minecraft.entity.Entity entity, int itemSlot, boolean isSelected) {
146:         if (world == null || world.isRemote) return;
147:         RPGItemDataManager dataManager = RPGItemDataManager.getInstance();
148:         RPGItemDataManager.ItemConfig config = dataManager.getConfig(rpgItemId);
149:         if (config == null) return;
150: 
151:         NBTTagCompound nbt = stack.getTagCompound();
152:         if (nbt == null) {
153:             nbt = new NBTTagCompound();
154:             stack.setTagCompound(nbt);
155:         }
156: 
157:         int currentDataVersion = dataManager.getDataVersion();
158:         int itemDataVersion = nbt.getInteger(NBT_VERSION_TAG);
159:         if (itemDataVersion == currentDataVersion) return;
160: 
161:         syncEnchantments(stack, nbt, config);
162:         nbt.setInteger(NBT_VERSION_TAG, currentDataVersion);
163:     }
164: 
165:     private void syncEnchantments(ItemStack stack, NBTTagCompound nbt, RPGItemDataManager.ItemConfig config) {
166:         Map<Integer, Integer> excelEnchants = parseEnchantments(config.enchantments);
167:         NBTTagCompound tracker = nbt.getCompoundTag(NBT_TRACKER_TAG);
168:         NBTTagList enchList = nbt.getTagList("ench", 10);
169:         NBTTagList newEnchList = new NBTTagList();
170: 
171:         for (int i = 0; i < enchList.tagCount(); i++) {
172:             NBTTagCompound enchTag = enchList.getCompoundTagAt(i);
173:             int id = enchTag.getShort("id");
174:             int lvl = enchTag.getShort("lvl");
175:             boolean wasAppliedBySystem = tracker.hasKey(String.valueOf(id)) && tracker.getInteger(String.valueOf(id)) == lvl;
176:             if (!wasAppliedBySystem) {
177:                 newEnchList.appendTag(enchTag.copy());
178:             }
179:         }
180: 
181:         NBTTagCompound newTracker = new NBTTagCompound();
182:         for (Map.Entry<Integer, Integer> entry : excelEnchants.entrySet()) {
183:             int newId = entry.getKey();
184:             int newLvl = entry.getValue();
185:             boolean playerHasOverride = false;
186:             for (int i = 0; i < newEnchList.tagCount(); i++) {
187:                 if (newEnchList.getCompoundTagAt(i).getShort("id") == newId) {
188:                     playerHasOverride = true;
189:                     break;
190:                 }
191:             }
192:             if (!playerHasOverride) {
193:                 NBTTagCompound newEnchTag = new NBTTagCompound();
194:                 newEnchTag.setShort("id", (short) newId);
195:                 newEnchTag.setShort("lvl", (short) newLvl);
196:                 newEnchList.appendTag(newEnchTag);
197:                 newTracker.setInteger(String.valueOf(newId), newLvl);
198:             }
199:         }
200: 
201:         if (newEnchList.tagCount() > 0) {
202:             nbt.setTag("ench", newEnchList);
203:         } else {
204:             nbt.removeTag("ench");
205:         }
206:         nbt.setTag(NBT_TRACKER_TAG, newTracker);
207:     }
208: 
209:     private Map<Integer, Integer> parseEnchantments(String enchantmentsStr) {
210:         Map<Integer, Integer> map = new HashMap<>();
211:         if (enchantmentsStr == null || enchantmentsStr.trim().isEmpty()) return map;
212:         String[] parts = enchantmentsStr.split(",");
213:         for (String part : parts) {
214:             String[] ench = part.trim().split(":");
215:             if (ench.length >= 1) {
216:                 try {
217:                     int id = Integer.parseInt(ench[0].trim());
218:                     int lvl = 1;
219:                     if (ench.length >= 2) {
220:                         try { lvl = Integer.parseInt(ench[1].trim()); }
221:                         catch (NumberFormatException e) {
222:                             if (ench.length >= 3) {
223:                                 try { lvl = Integer.parseInt(ench[2].trim()); }
224:                                 catch (NumberFormatException ignored) {}
225:                             }
226:                         }
227:                     }
228:                     if (id >= 0 && id < Enchantment.enchantmentsList.length && Enchantment.enchantmentsList[id] != null) {
229:                         map.put(id, lvl);
230:                     }
231:                 } catch (NumberFormatException ignored) {}
232:             }
233:         }
234:         return map;
235:     }
236: 
237:     @Override
238:     @SideOnly(Side.CLIENT)
239:     @SuppressWarnings("unchecked")
240:     public void getSubItems(Item item, CreativeTabs tab, List list) {
241:         ItemStack stack = new ItemStack(item, 1, 0);
242:         RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance().getConfig(rpgItemId);
243:         if (config != null) {
244:             NBTTagCompound nbt = new NBTTagCompound();
245:             nbt.setInteger(NBT_VERSION_TAG, RPGItemDataManager.getInstance().getDataVersion());
246:             syncEnchantments(stack, nbt, config);
247:             stack.setTagCompound(nbt);
248:         }
249:         list.add(stack);
250:     }
251: 
252:     public List<IRPGComponent> getAllComponents() { return allComponents; }
253:     public List<IOnHurt> getHurtHandlers() { return hurtHandlers; }
254:     public List<IOnEquip> getEquipHandlers() { return equipHandlers; }
255:     public List<IOnUnequip> getUnequipHandlers() { return unequipHandlers; }
256:     public List<IOnPlayerDeath> getPlayerDeathHandlers() { return playerDeathHandlers; }
257:     public List<IOnHit> getHitHandlers() { return hitHandlers; }
258: }
259: """
stripped = re.sub(r'^\d+:\s', '', content, flags=re.MULTILINE)
with open(r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\item\ItemRPGArmor.java', 'w', encoding='utf-8') as f:
    f.write(stripped)
print("Wrote ItemRPGArmor.java")
