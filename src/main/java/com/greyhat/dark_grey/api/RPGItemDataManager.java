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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
    private volatile Set<String> registeredItemIds = java.util.Collections.emptySet();
    private volatile Map<String, String> registeredItemShapes = java.util.Collections.emptyMap();
    private volatile String currentJson = "{\"items\":[]}";

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

        synchronizeBundledConfig();

        reload(false);
        startWatchService(darkGreyConfigDir.toPath());
    }

    /**
     * Keeps the external working copy in lockstep with the JSON bundled in the mod jar.
     * Item IDs are part of the Forge registry and therefore must not drift between a
     * client and server that use the same jar.
     */
    private void synchronizeBundledConfig() {
        try (InputStream is = RPGItemDataManager.class.getResourceAsStream("/assets/dark_grey/data/rpg_items.json")) {
            if (is == null) {
                DarkGrey.LOG.error("Bundled rpg_items.json is missing; refusing to replace the external config.");
                return;
            }

            Path bundledCopy = Files.createTempFile(
                configFile.getParentFile()
                    .toPath(),
                "rpg_items-",
                ".json");
            try {
                Files.copy(is, bundledCopy, StandardCopyOption.REPLACE_EXISTING);
                byte[] bundledBytes = Files.readAllBytes(bundledCopy);
                boolean needsUpdate = !configFile.exists()
                    || !Arrays.equals(Files.readAllBytes(configFile.toPath()), bundledBytes);

                if (needsUpdate) {
                    if (configFile.exists()) {
                        Path backup = new File(configFile.getParentFile(), "rpg_items.json.previous").toPath();
                        Files.copy(configFile.toPath(), backup, StandardCopyOption.REPLACE_EXISTING);
                    }
                    Files.move(bundledCopy, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    DarkGrey.LOG.info("Updated external rpg_items.json from the mod jar.");
                }
            } finally {
                Files.deleteIfExists(bundledCopy);
            }
        } catch (Exception e) {
            DarkGrey.LOG.error("Failed to synchronize bundled rpg_items.json.", e);
        }
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

    public boolean reload() {
        return reload(false);
    }

    public boolean reload(boolean isAutoWatch) {
        this.reloadPending = false;
        if (configFile == null || !configFile.exists()) {
            DarkGrey.LOG.warn(
                "RPG items config file not found at " + (configFile != null ? configFile.getAbsolutePath() : "null"));
            return false;
        }

        try (Reader reader = Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            Map<String, ItemConfig> newCache = parseConfigs(root);

            validateReload(newCache);
            activateConfig(newCache, gson.toJson(root));

            MinecraftServer server = MinecraftServer.getServer();
            if (server != null && server.getConfigurationManager() != null) {
                java.util.List<net.minecraft.entity.player.EntityPlayerMP> players = server
                    .getConfigurationManager().playerEntityList;
                for (net.minecraft.entity.player.EntityPlayerMP player : players) {
                    SetBonusManager.recalculateSets(player);
                    syncToPlayer(player);
                }
            }

            if (isAutoWatch && server != null) {
                server.getConfigurationManager()
                    .sendChatMsg(new ChatComponentText("§a[DarkGrey] 策划数据更新，已自动全服重载！"));
            }

            return true;

        } catch (Exception e) {
            DarkGrey.LOG.error("Failed to load RPG items from " + configFile.getAbsolutePath(), e);
            return false;
        }
    }

    public boolean applyRemoteConfig(String json) {
        try {
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(json, JsonObject.class);
            Map<String, ItemConfig> newCache = parseConfigs(root);
            validateReload(newCache);
            activateConfig(newCache, gson.toJson(root));
            return true;
        } catch (Exception e) {
            DarkGrey.LOG.error("Rejected RPG item configuration received from server", e);
            return false;
        }
    }

    private Map<String, ItemConfig> parseConfigs(JsonObject root) {
        if (root == null || !root.has("items")
            || !root.get("items")
                .isJsonArray()) {
            throw new IllegalArgumentException("RPG item JSON must contain an 'items' array");
        }
        JsonArray items = root.getAsJsonArray("items");
        Map<String, ItemConfig> newCache = new LinkedHashMap<>();

        for (JsonElement itemElem : items) {
            if (!itemElem.isJsonObject()) {
                throw new IllegalArgumentException("RPG item entry must be an object");
            }
            JsonObject itemObj = itemElem.getAsJsonObject();
            if (!itemObj.has("id")) {
                throw new IllegalArgumentException("RPGItem config is missing 'id' field");
            }
            String id = itemObj.get("id")
                .getAsString();
            if (!id.matches("[a-z0-9_]+")) {
                throw new IllegalArgumentException("Invalid RPG item id: " + id);
            }
            if (newCache.containsKey(id)) {
                throw new IllegalArgumentException("Duplicate RPG item id: " + id);
            }

            ItemConfig config = new ItemConfig();
            config.type = itemObj.has("type") ? itemObj.get("type")
                .getAsString() : "Weapon";
            String displayName = id;
            if (itemObj.has("displayName")) {
                JsonObject displayNames = itemObj.getAsJsonObject("displayName");
                if (displayNames.has("zh_CN")) displayName = displayNames.get("zh_CN")
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
            FMLLog.fine("[DarkGrey-Data] Loaded RPG item config: " + id);
        }
        return newCache;
    }

    private void activateConfig(Map<String, ItemConfig> newCache, String json) {
        this.configCache = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(newCache));
        this.currentJson = json;
        this.dataVersion++;

        DarkGrey.LOG.info(
            "Successfully loaded " + newCache.size()
                + " RPG items into global data manager. (Data Version: "
                + dataVersion
                + ")");
        for (Object obj : net.minecraft.item.Item.itemRegistry) {
            if (obj instanceof com.greyhat.dark_grey.api.IRPGItemContainer) {
                ((com.greyhat.dark_grey.api.IRPGItemContainer) obj).rebuildComponents();
            }
        }
    }

    public void syncToPlayer(net.minecraft.entity.player.EntityPlayerMP player) {
        // Integrated-server host shares this singleton and the item registry with the client.
        // Sending the packet back through the local channel would apply the same reload twice.
        if (player != null && player.playerNetServerHandler != null
            && !player.playerNetServerHandler.netManager.isLocalChannel()) {
            DarkGrey.NETWORK.sendTo(new com.greyhat.dark_grey.network.ConfigSyncMessage(this.currentJson), player);
        }
    }

    private void validateReload(Map<String, ItemConfig> newCache) {
        Set<String> frozenIds = this.registeredItemIds;
        if (!frozenIds.isEmpty() && !frozenIds.equals(newCache.keySet())) {
            throw new IllegalArgumentException(
                "Runtime reload cannot add, remove, or rename Forge registry IDs. Expected " + frozenIds
                    + " but found "
                    + newCache.keySet());
        }

        if (frozenIds.isEmpty()) {
            return;
        }
        for (Map.Entry<String, ItemConfig> entry : newCache.entrySet()) {
            ItemConfig config = entry.getValue();
            String expectedShape = this.registeredItemShapes.get(entry.getKey());
            String actualShape = config.type + "\u0000" + config.texture;
            if (expectedShape != null && !expectedShape.equals(actualShape)) {
                throw new IllegalArgumentException(
                    "Runtime reload cannot change item type or texture for " + entry.getKey());
            }
            JsonArray components = config.componentsJson;
            if (components == null) continue;
            for (JsonElement componentElement : components) {
                if (!componentElement.isJsonObject()) {
                    throw new IllegalArgumentException("Component entry must be an object for item " + entry.getKey());
                }
                JsonObject component = componentElement.getAsJsonObject();
                if (!component.has("name")) {
                    throw new IllegalArgumentException("Component is missing 'name' for item " + entry.getKey());
                }
                String name = component.get("name")
                    .getAsString();
                JsonObject params = component.has("params") ? component.getAsJsonObject("params") : new JsonObject();
                ComponentRegistry.create(name, params);
            }
        }
    }

    public void freezeRegistry(Map<String, ItemConfig> configs) {
        this.registeredItemIds = java.util.Collections.unmodifiableSet(new HashSet<>(configs.keySet()));
        Map<String, String> shapes = new LinkedHashMap<>();
        for (Map.Entry<String, ItemConfig> entry : configs.entrySet()) {
            ItemConfig config = entry.getValue();
            shapes.put(entry.getKey(), config.type + "\u0000" + config.texture);
        }
        this.registeredItemShapes = java.util.Collections.unmodifiableMap(shapes);
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
