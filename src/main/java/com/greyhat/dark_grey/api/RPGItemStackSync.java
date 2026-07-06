package com.greyhat.dark_grey.api;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * P2 #13: 抽取物品类的 NBT 同步逻辑，彻底消除 6 个物品类中的冗余同步代码。
 */
public class RPGItemStackSync {

    public static final String NBT_TRACKER_TAG = "DarkGreyRPG_ExcelEnchants";
    public static final String NBT_VERSION_TAG = "DarkGreyRPG_DataVersion";

    /**
     * P1 #6: 优先读取 NBT 版本与主数据版本对比。
     * 若匹配，则直接返回，避免每 tick 均调用 dataManager.getConfig() 进入锁和 HashMap 检索。
     *
     * <p>
     * R1: 仅在服务端执行。专用服务器上客户端有独立的 dataVersion 计数，
     * 客户端本地改写 NBT 会与服务端同步包互相覆盖，造成附魔闪烁和无谓的每 tick NBT 操作。
     * </p>
     */
    public static void syncIfVersionChanged(ItemStack stack, String rpgItemId, net.minecraft.world.World world) {
        if (world == null || world.isRemote) {
            return;
        }
        RPGItemDataManager dataManager = RPGItemDataManager.getInstance();
        int currentDataVersion = dataManager.getDataVersion();

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null) {
            int itemDataVersion = nbt.getInteger(NBT_VERSION_TAG);
            if (itemDataVersion == currentDataVersion) {
                return;
            }
        }

        // 仅在版本不一致时进行配置读取与 NBT 同步
        RPGItemDataManager.ItemConfig config = dataManager.getConfig(rpgItemId);
        if (config == null) {
            return;
        }

        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }

        syncEnchantments(stack, nbt, config);
        nbt.setInteger(NBT_VERSION_TAG, currentDataVersion);
    }

    /**
     * 执行初始化同步，主要用于 getSubItems (创造模式物品栏展示初始化)。
     */
    public static void forceSync(ItemStack stack, String rpgItemId) {
        RPGItemDataManager dataManager = RPGItemDataManager.getInstance();
        RPGItemDataManager.ItemConfig config = dataManager.getConfig(rpgItemId);
        if (config != null) {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setInteger(NBT_VERSION_TAG, dataManager.getDataVersion());
            syncEnchantments(stack, nbt, config);
            stack.setTagCompound(nbt);
        }
    }

    private static void syncEnchantments(ItemStack stack, NBTTagCompound nbt, RPGItemDataManager.ItemConfig config) {
        Map<Integer, Integer> excelEnchants = parseEnchantments(config.enchantments);
        NBTTagCompound tracker = nbt.getCompoundTag(NBT_TRACKER_TAG);
        NBTTagList enchList = nbt.getTagList("ench", 10);
        NBTTagList newEnchList = new NBTTagList();

        // 过滤系统同步附魔，保留玩家自身附魔
        for (int i = 0; i < enchList.tagCount(); i++) {
            NBTTagCompound enchTag = enchList.getCompoundTagAt(i);
            int id = enchTag.getShort("id");
            int lvl = enchTag.getShort("lvl");
            boolean wasAppliedBySystem = tracker.hasKey(String.valueOf(id))
                && tracker.getInteger(String.valueOf(id)) == lvl;
            if (!wasAppliedBySystem) {
                newEnchList.appendTag(enchTag.copy());
            }
        }

        NBTTagCompound newTracker = new NBTTagCompound();
        for (Map.Entry<Integer, Integer> entry : excelEnchants.entrySet()) {
            int newId = entry.getKey();
            int newLvl = entry.getValue();
            boolean playerOverride = false;
            for (int i = 0; i < newEnchList.tagCount(); i++) {
                if (newEnchList.getCompoundTagAt(i)
                    .getShort("id") == newId) {
                    playerOverride = true;
                    break;
                }
            }
            if (!playerOverride) {
                NBTTagCompound newEnchTag = new NBTTagCompound();
                newEnchTag.setShort("id", (short) newId);
                newEnchTag.setShort("lvl", (short) newLvl);
                newEnchList.appendTag(newEnchTag);
                newTracker.setInteger(String.valueOf(newId), newLvl);
            }
        }

        if (newEnchList.tagCount() > 0) {
            nbt.setTag("ench", newEnchList);
        } else {
            nbt.removeTag("ench");
        }
        nbt.setTag(NBT_TRACKER_TAG, newTracker);
    }

    private static Map<Integer, Integer> parseEnchantments(String enchantmentsStr) {
        Map<Integer, Integer> map = new HashMap<>();
        if (enchantmentsStr == null || enchantmentsStr.trim()
            .isEmpty()) {
            return map;
        }
        String[] parts = enchantmentsStr.split(",");
        for (String part : parts) {
            String[] ench = part.trim()
                .split(":");
            if (ench.length >= 1) {
                try {
                    int id = Integer.parseInt(ench[0].trim());
                    int lvl = 1;
                    if (ench.length >= 2) {
                        try {
                            lvl = Integer.parseInt(ench[1].trim());
                        } catch (NumberFormatException e) {
                            if (ench.length >= 3) {
                                try {
                                    lvl = Integer.parseInt(ench[2].trim());
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                    if (id >= 0 && id < Enchantment.enchantmentsList.length
                        && Enchantment.enchantmentsList[id] != null) {
                        map.put(id, lvl);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return map;
    }
}
