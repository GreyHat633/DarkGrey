package com.greyhat.dark_grey.api;
 
-import com.google.gson.JsonArray;
-import com.google.gson.JsonElement;
-import com.google.gson.JsonObject;
-import com.greyhat.dark_grey.DarkGrey;
-import com.greyhat.dark_grey.item.ItemRPGAccessory;
-import com.greyhat.dark_grey.item.ItemRPGArmor;
-// // import com.greyhat.dark_grey.item.ItemRPGBow;
-import com.greyhat.dark_grey.item.ItemRPGHoe;
-import com.greyhat.dark_grey.item.ItemRPGTool;
-import com.greyhat.dark_grey.item.ItemRPGWeapon;
-// // import com.greyhat.dark_grey.item.ItemRPGScythe;
-// // import com.greyhat.dark_grey.item.ItemRPGAmmo;
-import cpw.mods.fml.common.registry.GameRegistry;
-import net.minecraft.item.Item;
-import net.minecraft.item.ItemArmor.ArmorMaterial;
-import net.minecraftforge.common.util.EnumHelper;
-
-import java.util.ArrayList;
-import java.util.List;
-import java.util.Map;
-
-/**
- * Loads RPG items from the global data manager and registers them in the game.
- *
- * <p>Supports 11 item types mapped from Chinese Excel dropdown values to their
- * corresponding Minecraft item base classes. Each type inherits the vanilla
- * behavior of its base class (e.g. hoe tills, bow shoots arrows).</p>
- */
-public class RPGItemLoader {
-
-    public static void loadItemsFromData() {
-        try {
-            Map<String, RPGItemDataManager.ItemConfig> configs = RPGItemDataManager.getInstance().getAllConfigs();
-            if (configs.isEmpty()) {
-                DarkGrey.LOG.warn("No RPG items found in data manager during initialization.");
-                return;
-            }
-
-            for (Map.Entry<String, RPGItemDataManager.ItemConfig> entry : configs.entrySet()) {
-                String id = entry.getKey();
-                RPGItemDataManager.ItemConfig config = entry.getValue();
-
-                String type = config.type;
-                String texture = config.texture;
-                String unlocalizedName = "dark_grey:" + id;
-
-                
-                Item.ToolMaterial toolMaterial = EnumHelper.addToolMaterial(
-                    "CUSTOM_" + id.toUpperCase(), 0, 100, 2.0F, 0.0F, 15);
-                ArmorMaterial armorMaterial = EnumHelper.addArmorMaterial(
-                    "CUSTOM_" + id.toUpperCase(), 10, new int[]{0, 0, 0, 0}, 15);
-
-                // Build components from JSON
-                List<IRPGComponent> components = buildComponents(id, config);
-
-                // Instantiate the correct container based on type
-                Item rpgItem = createItemForType(type, id, toolMaterial, armorMaterial, components);
-
-                if (rpgItem == null) {
-                    DarkGrey.LOG.warn("Unknown item type: '" + type + "' for item " + id
-                        + ". Valid types: 鍓? 鏂? 闀? 閾? 閿? 寮? 澶寸洈, 鑳哥敳, 鎶よ吙, 闈村瓙, 楗板搧");
-                    continue;
-                }
-
-                // Register
-                rpgItem.setUnlocalizedName(unlocalizedName);
-                if (texture != null && !texture.isEmpty()) {
-                    rpgItem.setTextureName(texture);
-                }
-                rpgItem.setCreativeTab(DarkGrey.creativeTab);
-                GameRegistry.registerItem(rpgItem, id);
-
-                // Auto-detect equipped texture for 3D rendering
-                registerEquippedRenderer(rpgItem, id, texture);
-
-                DarkGrey.LOG.info("Registered RPG Item: " + id + " (type=" + type + ") with "
-                    + components.size() + " components.");
-            }
-        } catch (Exception e) {
-            DarkGrey.LOG.error("Failed to load RPG items from Data Manager", e);
-        }
-    }
-
-    /**
-     * Creates the correct Item subclass based on the type string from Excel.
-     */
-    private static Item createItemForType(String type, String id,
-                                           Item.ToolMaterial toolMaterial,
-                                           ArmorMaterial armorMaterial,
-                                           List<IRPGComponent> components) {
-        if (type == null) return null;
-
-        switch (type) {
-            // 鈹€鈹€ Weapons 鈹€鈹€
-            case "鍓?:
-            case "sword":
-            case "Weapon":  // backward compat
-            case "weapon":
-                return new ItemRPGWeapon(id, toolMaterial, components);
-
-            // 鈹€鈹€ Scythe 鈹€鈹€
-            case "\u9570\u5200": // 闀板垁
-            case "scythe":
-// //                 return new ItemRPGScythe(id, toolMaterial, components);
-
-            // 鈹€鈹€ Tools 鈹€鈹€
-            case "鏂?:
-            case "axe":
-                return new ItemRPGTool(id, toolMaterial, "axe", components);
-            case "闀?:
-            case "pickaxe":
-                return new ItemRPGTool(id, toolMaterial, "pickaxe", components);
-            case "閾?:
-            case "shovel":
-                return new ItemRPGTool(id, toolMaterial, "shovel", components);
-
-            // 鈹€鈹€ Hoe 鈹€鈹€
-            case "\u9504": // 閿?
-            case "hoe":
-                return new ItemRPGHoe(id, toolMaterial, components);
-
-            // 鈹€鈹€ Bow 鈹€鈹€
-            case "\u5F13": // 寮?
-            case "bow":
-// //                 return new ItemRPGBow(id, components);
-
-            // 鈹€鈹€ Ammo 鈹€鈹€
-            case "\u7BAD": // 绠?
-            case "arrow":
-// //                 return new ItemRPGAmmo(id, components);
-
-            // 鈹€鈹€ Armor 鈹€鈹€ (renderIndex: helmet/boots=0, chest/legs=1)
-            case "\u5934\u76D4": // 澶寸洈
-            case "helmet":
-            case "Armor":   // backward compat (defaults to helmet)
-            case "armor":
-                return new ItemRPGArmor(id, armorMaterial, 0, 0, components);
-            case "\u80F8\u7532": // 鑳哥敳
-            case "chestplate":
-                return new ItemRPGArmor(id, armorMaterial, 1, 1, components);
-            case "\u62A4\u817F": // 鎶よ吙
-            case "leggings":
-                return new ItemRPGArmor(id, armorMaterial, 1, 2, components);
-            case "\u9774\u5B50": // 闈村瓙
-            case "boots":
-                return new ItemRPGArmor(id, armorMaterial, 0, 3, components);
-
-            // 鈹€鈹€ Accessory 鈹€鈹€
-            case "\u9970\u54C1": // 楗板搧
-            case "accessory":
-            case "Accessory":  // backward compat
-                return new ItemRPGAccessory(id, components);
-
-            default:
-                return null;
-        }
-    }
-
-    /**
-     * Builds component instances from the JSON config's componentsJson array.
-     */
-    private static List<IRPGComponent> buildComponents(String itemId, RPGItemDataManager.ItemConfig config) {
-        List<IRPGComponent> components = new ArrayList<>();
-        if (config.componentsJson == null) return components;
-
-        for (JsonElement compElem : config.componentsJson) {
-            JsonObject compObj = compElem.getAsJsonObject();
-            String compName = compObj.get("name").getAsString();
-            JsonObject params = compObj.has("params") ? compObj.getAsJsonObject("params") : new JsonObject();
-
-            try {
-                IRPGComponent comp = ComponentRegistry.create(compName, params);
-                components.add(comp);
-            } catch (IllegalArgumentException e) {
-                DarkGrey.LOG.warn(e.getMessage() + " for item " + itemId);
-            }
-        }
-        return components;
-    }
-
-    /**
-     * Registers equipped (3D) texture renderer if an _equipped texture variant exists.
-     */
-    private static void registerEquippedRenderer(Item rpgItem, String id, String texture) {
-// //         if (rpgItem instanceof ItemRPGBow) {
-// //             DarkGrey.proxy.registerBowRenderer(rpgItem);
-            DarkGrey.LOG.info("Bound custom bow renderer for " + id);
-            return; // Don't use normal RPGItemRenderer for bows!
-        }
-
-        if (!(rpgItem instanceof ItemRPGWeapon || rpgItem instanceof ItemRPGTool
-              || rpgItem instanceof ItemRPGHoe)) {
-            return;
-        }
-
-// //         boolean isScythe = rpgItem instanceof ItemRPGScythe;
-
-        String prefix = id;
-        if (texture != null && !texture.isEmpty() && texture.contains(":")) {
-            prefix = texture.substring(texture.indexOf(":") + 1);
-        } else if (texture != null && !texture.isEmpty()) {
-            prefix = texture;
-        }
-
-        String equippedName = prefix + "_equipped";
-        String resourcePath = "/assets/dark_grey/textures/items/" + equippedName + ".png";
-        
-        boolean textureExists = RPGItemLoader.class.getResource(resourcePath) != null;
-        if (!textureExists) {
-            // Fallback for IDE environments where resources aren't synced to classpath yet
-            java.io.File devFile = new java.io.File("src/main/resources" + resourcePath);
-            if (devFile.exists()) {
-                textureExists = true;
-            }
-        }
-
-        if (textureExists) {
-            if (isScythe) {
-// //                 DarkGrey.proxy.registerScytheRenderer(rpgItem, equippedName);
-                DarkGrey.LOG.info("Bound custom scythe renderer for " + id + " using " + equippedName);
-            } else {
-                DarkGrey.proxy.registerItemRenderer(rpgItem, equippedName);
-                DarkGrey.LOG.info("Bound custom 3D renderer for " + id + " using " + equippedName);
-            }
-        }
+package com.greyhat.dark_grey.api;
+
+import com.google.gson.JsonArray;
+import com.google.gson.JsonElement;
+import com.google.gson.JsonObject;
+import com.greyhat.dark_grey.DarkGrey;
+import com.greyhat.dark_grey.item.ItemRPGAccessory;
+import com.greyhat.dark_grey.item.ItemRPGArmor;
+import com.greyhat.dark_grey.item.ItemRPGBow;
+import com.greyhat.dark_grey.item.ItemRPGHoe;
+import com.greyhat.dark_grey.item.ItemRPGTool;
+import com.greyhat.dark_grey.item.ItemRPGWeapon;
+import com.greyhat.dark_grey.item.ItemRPGScythe;
+import com.greyhat.dark_grey.item.ItemRPGAmmo;
+import cpw.mods.fml.common.registry.GameRegistry;
+import net.minecraft.item.Item;
+import net.minecraft.item.ItemArmor.ArmorMaterial;
+import net.minecraftforge.common.util.EnumHelper;
+
+import java.util.ArrayList;
+import java.util.List;
+import java.util.Map;
+
+/**
+ * Loads RPG items from the global data manager and registers them in the game.
+ *
+ * <p>Supports 11 item types mapped from Chinese Excel dropdown values to their
+ * corresponding Minecraft item base classes. Each type inherits the vanilla
+ * behavior of its base class (e.g. hoe tills, bow shoots arrows).</p>
+ */
+public class RPGItemLoader {
+
+    public static void loadItemsFromData() {
+        try {
+            Map<String, RPGItemDataManager.ItemConfig> configs = RPGItemDataManager.getInstance().getAllConfigs();
+            if (configs.isEmpty()) {
+                DarkGrey.LOG.warn("No RPG items found in data manager during initialization.");
+                return;
+            }
+
+            for (Map.Entry<String, RPGItemDataManager.ItemConfig> entry : configs.entrySet()) {
+                String id = entry.getKey();
+                RPGItemDataManager.ItemConfig config = entry.getValue();
+
+                String type = config.type;
+                String texture = config.texture;
+                String unlocalizedName = "dark_grey:" + id;
+
+                
+                Item.ToolMaterial toolMaterial = EnumHelper.addToolMaterial(
+                    "CUSTOM_" + id.toUpperCase(), 0, 100, 2.0F, 0.0F, 15);
+                ArmorMaterial armorMaterial = EnumHelper.addArmorMaterial(
+                    "CUSTOM_" + id.toUpperCase(), 10, new int[]{0, 0, 0, 0}, 15);
+
+                // Build components from JSON
+                List<IRPGComponent> components = buildComponents(id, config);
+
+                // Instantiate the correct container based on type
+                Item rpgItem = createItemForType(type, id, toolMaterial, armorMaterial, components);
+
+                if (rpgItem == null) {
+                    DarkGrey.LOG.warn("Unknown item type: '" + type + "' for item " + id
+                        + ". Valid types: 剑, 斧, 镐, 铲, 锄, 弓, 头盔, 胸甲, 护腿, 靴子, 饰品");
+                    continue;
+                }
+
+                // Register
+                rpgItem.setUnlocalizedName(unlocalizedName);
+                if (texture != null && !texture.isEmpty()) {
+                    rpgItem.setTextureName(texture);
+                }
+                rpgItem.setCreativeTab(DarkGrey.creativeTab);
+                GameRegistry.registerItem(rpgItem, id);
+
+                // Auto-detect equipped texture for 3D rendering
+                registerEquippedRenderer(rpgItem, id, texture);
+
+                DarkGrey.LOG.info("Registered RPG Item: " + id + " (type=" + type + ") with "
+                    + components.size() + " components.");
+            }
+        } catch (Exception e) {
+            DarkGrey.LOG.error("Failed to load RPG items from Data Manager", e);
+        }
+    }
+
+    /**
+     * Creates the correct Item subclass based on the type string from Excel.
+     */
+    private static Item createItemForType(String type, String id,
+                                           Item.ToolMaterial toolMaterial,
+                                           ArmorMaterial armorMaterial,
+                                           List<IRPGComponent> components) {
+        if (type == null) return null;
+
+        switch (type) {
+            // —— Weapons ——
+            case "剑":
+            case "sword":
+            case "Weapon":  // backward compat
+            case "weapon":
+                return new ItemRPGWeapon(id, toolMaterial, components);
+
+            // —— Scythe ——
+            case "\u9570\u5200": // 镰刀
+            case "scythe":
+                return new ItemRPGScythe(id, toolMaterial, components);
+
+            // —— Tools ——
+            case "斧":
+            case "axe":
+                return new ItemRPGTool(id, toolMaterial, "axe", components);
+            case "镐":
+            case "pickaxe":
+                return new ItemRPGTool(id, toolMaterial, "pickaxe", components);
+            case "铲":
+            case "shovel":
+                return new ItemRPGTool(id, toolMaterial, "shovel", components);
+
+            // —— Hoe ——
+            case "\u9504": // 锄
+            case "hoe":
+                return new ItemRPGHoe(id, toolMaterial, components);
+
+            // —— Bow ——
+            case "\u5F13": // 弓
+            case "bow":
+                return new ItemRPGBow(id, components);
+
+            // —— Ammo ——
+            case "\u7BAD": // 箭
+            case "arrow":
+                return new ItemRPGAmmo(id, components);
+
+            // —— Armor —— (renderIndex: helmet/boots=0, chest/legs=1)
+            case "\u5934\u76D4": // 头盔
+            case "helmet":
+            case "Armor":   // backward compat (defaults to helmet)
+            case "armor":
+                return new ItemRPGArmor(id, armorMaterial, 0, 0, components);
+            case "\u80F8\u7532": // 胸甲
+            case "chestplate":
+                return new ItemRPGArmor(id, armorMaterial, 1, 1, components);
+            case "\u62A4\u817F": // 护腿
+            case "leggings":
+                return new ItemRPGArmor(id, armorMaterial, 1, 2, components);
+            case "\u9774\u5B50": // 靴子
+            case "boots":
+                return new ItemRPGArmor(id, armorMaterial, 0, 3, components);
+
+            // —— Accessory ——
+            case "\u9970\u54C1": // 饰品
+            case "accessory":
+            case "Accessory":  // backward compat
+                return new ItemRPGAccessory(id, components);
+
+            default:
+                return null;
+        }
+    }
+
+    /**
+     * Builds component instances from the JSON config's componentsJson array.
+     */
+    private static List<IRPGComponent> buildComponents(String itemId, RPGItemDataManager.ItemConfig config) {
+        List<IRPGComponent> components = new ArrayList<>();
+        if (config.componentsJson == null) return components;
+
+        for (JsonElement compElem : config.componentsJson) {
+            JsonObject compObj = compElem.getAsJsonObject();
+            String compName = compObj.get("name").getAsString();
+            JsonObject params = compObj.has("params") ? compObj.getAsJsonObject("params") : new JsonObject();
+
+            try {
+                IRPGComponent comp = ComponentRegistry.create(compName, params);
+                components.add(comp);
+            } catch (IllegalArgumentException e) {
+                DarkGrey.LOG.warn(e.getMessage() + " for item " + itemId);
+            }
+        }
+        return components;
+    }
+
+    /**
+     * Registers equipped (3D) texture renderer if an _equipped texture variant exists.
+     */
+    private static void registerEquippedRenderer(Item rpgItem, String id, String texture) {
+        if (rpgItem instanceof ItemRPGBow) {
+            DarkGrey.proxy.registerBowRenderer(rpgItem);
+            DarkGrey.LOG.info("Bound custom bow renderer for " + id);
+            return; // Don't use normal RPGItemRenderer for bows!
+        }
+
+        if (!(rpgItem instanceof ItemRPGWeapon || rpgItem instanceof ItemRPGTool
+              || rpgItem instanceof ItemRPGHoe)) {
+            return;
+        }
+
+        boolean isScythe = rpgItem instanceof ItemRPGScythe;
+
+        String prefix = id;
+        if (texture != null && !texture.isEmpty() && texture.contains(":")) {
+            prefix = texture.substring(texture.indexOf(":") + 1);
+        } else if (texture != null && !texture.isEmpty()) {
+            prefix = texture;
+        }
+
+        String equippedName = prefix + "_equipped";
+        String resourcePath = "/assets/dark_grey/textures/items/" + equippedName + ".png";
+        
+        boolean textureExists = RPGItemLoader.class.getResource(resourcePath) != null;
+        if (!textureExists) {
+            // Fallback for IDE environments where resources aren't synced to classpath yet
+            java.io.File devFile = new java.io.File("src/main/resources" + resourcePath);
+            if (devFile.exists()) {
+                textureExists = true;
+            }
+        }
+
+        if (textureExists) {
+            if (isScythe) {
+                DarkGrey.proxy.registerScytheRenderer(rpgItem, equippedName);
+                DarkGrey.LOG.info("Bound custom scythe renderer for " + id + " using " + equippedName);
+            } else {
+                DarkGrey.proxy.registerItemRenderer(rpgItem, equippedName);
+                DarkGrey.LOG.info("Bound custom 3D renderer for " + id + " using " + equippedName);
+            }
+        }
     }
[diff_block_end]

Please note that the above snippet only shows the MODIFIED lines from the last change. It shows up to 3 lines of unchanged lines before and after the modified lines. The actual file contents may have many more lines not shown.

We did our best to apply changes despite some inaccuracies. Double check if the edit applied is what you intended.