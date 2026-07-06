package com.greyhat.dark_grey.api;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.greyhat.dark_grey.DarkGrey;

import cpw.mods.fml.common.FMLLog;

/**
 * Global singleton manager for RPG item data.
 * Supports hot-reloading configurations from JSON without restarting the game.
 */
public class RPGItemDataManager {

    private static final RPGItemDataManager INSTANCE = new RPGItemDataManager();

    // In-memory cache of global item configurations, keyed by item ID (e.g. "cleave_sword")
    // P1 #6: 采用不可变 Map + volatile 整体替换，完全消除读路径的 synchronized 锁竞争
    private volatile Map<String, ItemConfig> configCache = java.util.Collections.emptyMap();

    // A version counter that increments every time the data is reloaded.
    // Useful for items to quickly check if their NBT needs an update pass.
    private volatile int dataVersion = 1;

    private File configFile;
    private Thread watcherThread;
    private volatile boolean reloadPending = false;

    private RPGItemDataManager() {}

    public static RPGItemDataManager getInstance() {
        return INSTANCE;
    }

    public boolean isReloadPending() {
        return reloadPending;
    }

    public void setReloadPending(boolean pending) {
        this.reloadPending = pending;
    }

    public void initialize(File configDir) {
        File darkGreyConfigDir = new File(configDir, "dark_grey");
        if (!darkGreyConfigDir.exists()) {
            darkGreyConfigDir.mkdirs();
        }
        this.configFile = new File(darkGreyConfigDir, "rpg_items.json");

        if (!this.configFile.exists()) {
            // Copy default from assets if available
            try (InputStream is = RPGItemDataManager.class
                .getResourceAsStream("/assets/dark_grey/data/rpg_items.json")) {
                if (is != null) {
                    Files.copy(is, this.configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    DarkGrey.LOG.info("Copied default rpg_items.json to config directory.");
                } else {
                    DarkGrey.LOG.warn("Could not find default rpg_items.json in assets. Creating empty file.");
                    this.configFile.createNewFile();
                    Files.write(this.configFile.toPath(), "{\"items\":[]}".getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                DarkGrey.LOG.error("Failed to copy default config.", e);
            }
        }

        reload(false);
        startWatchService(darkGreyConfigDir.toPath());
    }

    private void startWatchService(final Path configDirPath) {
        if (watcherThread != null && watcherThread.isAlive()) {
            return;
        }

        watcherThread = new Thread(() -> {
            try (WatchService watchService = configDirPath.getFileSystem()
                .newWatchService()) {
                configDirPath
                    .register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);
                DarkGrey.LOG.info("Started JSON WatchService for RPG items on: " + configDirPath.toString());

                while (true) {
                    WatchKey key = watchService.take();
                    boolean modified = false;
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;

                        Path changed = (Path) event.context();
                        if (changed.getFileName()
                            .toString()
                            .equals("rpg_items.json")) {
                            modified = true;
                        }
                    }

                    if (modified) {
                        // Debounce slightly to ensure the file writing is completely finished
                        Thread.sleep(500);
                        // P0 #2: 不在守护线程中直接 reload，而是将标志置为 true，由主线程 Tick 触发重载
                        reloadPending = true;
                    }

                    if (!key.reset()) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                DarkGrey.LOG.info("RPGItemDataManager WatchService interrupted.");
            } catch (Exception e) {
                DarkGrey.LOG.error("RPGItemDataManager WatchService error:", e);
            }
        });
        watcherThread.setDaemon(true);
        watcherThread.setName("DarkGrey-RPG-Watcher");
        watcherThread.start();
    }

    public void reload() {
        reload(false);
    }

    public void reload(boolean isAutoWatch) {
        this.reloadPending = false;
        if (configFile == null || !configFile.exists()) {
            DarkGrey.LOG.warn(
                "RPG items config file not found at " + (configFile != null ? configFile.getAbsolutePath() : "null"));
            return;
        }

        try (Reader reader = Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            JsonArray items = root.getAsJsonArray("items");

            Map<String, ItemConfig> newCache = new HashMap<>();

            for (JsonElement itemElem : items) {
                JsonObject itemObj = itemElem.getAsJsonObject();
                if (!itemObj.has("id")) {
                    throw new RuntimeException("RPGItem config is missing 'id' field!");
                }
                String id = itemObj.get("id")
                    .getAsString();

                ItemConfig config = new ItemConfig();
                config.type = itemObj.has("type") ? itemObj.get("type")
                    .getAsString() : "Weapon";

                // Parse display name for logging
                String displayName = id;
                if (itemObj.has("displayName")) {
                    JsonObject dpObj = itemObj.getAsJsonObject("displayName");
                    if (dpObj.has("zh_CN")) displayName = dpObj.get("zh_CN")
                        .getAsString();
                }
                config.displayName = displayName;

                config.texture = itemObj.has("texture") ? itemObj.get("texture")
                    .getAsString() : "";
                config.durability = itemObj.has("durability") ? itemObj.get("durability")
                    .getAsInt() : 0;
                config.damage = itemObj.has("damage") ? itemObj.get("damage")
                    .getAsFloat() : 0.0f;
                config.enchantments = itemObj.has("enchantments") ? itemObj.get("enchantments")
                    .getAsString() : "";

                if (itemObj.has("components")) {
                    config.componentsJson = itemObj.getAsJsonArray("components");
                }

                newCache.put(id, config);
                // P2 #19: 热重载打 info 日志降级为 debug 或汇总，防止刷屏
                FMLLog.fine(
                    "[DarkGrey-Data] Successfully loaded weapon: " + displayName
                        + " (ID: "
                        + id
                        + "), Damage: "
                        + config.damage
                        + ", Durability: "
                        + config.durability
                        + ", Enchants: ["
                        + config.enchantments
                        + "]");
            }

            // P1 #6: 写入时整体替换 configCache
            this.configCache = java.util.Collections.unmodifiableMap(newCache);
            this.dataVersion++;

            DarkGrey.LOG.info(
                "Successfully loaded " + newCache.size()
                    + " RPG items into global data manager. (Data Version: "
                    + dataVersion
                    + ")");

            // P1 #5: 热重载不单重建 Weapon，而是对所有实现 IRPGItemContainer 的物品进行重建
            for (Object obj : net.minecraft.item.Item.itemRegistry) {
                if (obj instanceof com.greyhat.dark_grey.api.IRPGItemContainer) {
                    ((com.greyhat.dark_grey.api.IRPGItemContainer) obj).rebuildComponents();
                }
            }

            if (isAutoWatch && MinecraftServer.getServer() != null) {
                MinecraftServer.getServer()
                    .getConfigurationManager()
                    .sendChatMsg(new ChatComponentText("§a[DarkGrey] 策划数据更新，已自动全服重载！"));
            }

        } catch (Exception e) {
            DarkGrey.LOG.error("Failed to load RPG items from " + configFile.getAbsolutePath(), e);
        }
    }

    // P1 #6: 读路径完全无锁，取消 synchronized 锁竞争
    public ItemConfig getConfig(String itemId) {
        return configCache.get(itemId);
    }

    public Map<String, ItemConfig> getAllConfigs() {
        return configCache;
    }

    public int getDataVersion() {
        return dataVersion;
    }

    public static class ItemConfig {

        public String displayName;
        public String type;
        public String texture;
        public int durability;
        public float damage;
        public String enchantments;
        public JsonArray componentsJson;
    }
}
