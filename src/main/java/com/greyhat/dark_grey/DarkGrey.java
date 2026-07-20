//
// Decompiled by Procyon v0.6.0
//

package com.greyhat.dark_grey;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Supplier;

import net.minecraft.command.ICommand;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.ComponentRegistry;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.RPGItemDataManager;
import com.greyhat.dark_grey.api.RPGItemLoader;
import com.greyhat.dark_grey.command.CommandRPGHelp;
import com.greyhat.dark_grey.command.CommandRPGReload;
import com.greyhat.dark_grey.common.CommonProxy;
import com.greyhat.dark_grey.common.DarkGreyTab;
import com.greyhat.dark_grey.common.EntityVampire;
import com.greyhat.dark_grey.common.ItemTabIcon;
import com.greyhat.dark_grey.component.ComponentBloodSacrifice;
import com.greyhat.dark_grey.component.ComponentCalamity;
import com.greyhat.dark_grey.component.ComponentLawOfCycles;
import com.greyhat.dark_grey.component.ComponentSolarFlare;
import com.greyhat.dark_grey.component.ComponentSupernovaSet;
import com.greyhat.dark_grey.component.ComponentTrueLawOfCycles;
import com.greyhat.dark_grey.component.HeavyStrikeComponent;
import com.greyhat.dark_grey.component.LifestealComponent;
import com.greyhat.dark_grey.entity.EntityMadokaArrow;
import com.greyhat.dark_grey.entity.EntityMadokaRing;
import com.greyhat.dark_grey.entity.EntityPhantomStrike;
import com.greyhat.dark_grey.entity.EntityScythe;
import com.greyhat.dark_grey.event.RPGCoreEventHandler;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(modid = "dark_grey", version = Tags.VERSION, name = "DarkGrey", acceptedMinecraftVersions = "[1.7.10]")
public class DarkGrey {

    public static final String MODID = "dark_grey";
    public static final String NAME = "DarkGrey";
    public static final String VERSION = Tags.VERSION;
    @Mod.Instance("dark_grey")
    public static DarkGrey instance;
    public static Item tabIcon;
    public static Item arrowOfLight;
    public static CreativeTabs creativeTab;
    public static final Logger LOG;
    public static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("darkgrey");
    @SidedProxy(
        clientSide = "com.greyhat.dark_grey.common.ClientProxy",
        serverSide = "com.greyhat.dark_grey.common.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(final FMLPreInitializationEvent event) {
        DarkGrey.proxy.preInit(event);
        DarkGrey.proxy.registerNetworkHandlers();
        RPGItemDataManager.getInstance()
            .initialize(event.getModConfigurationDirectory());
        GameRegistry.registerItem(DarkGrey.tabIcon = new ItemTabIcon(), "tab_icon");
        DarkGrey.creativeTab = new DarkGreyTab("DarkGrey");
        ComponentRegistry.register("重击", (Supplier<IRPGComponent>) HeavyStrikeComponent::new);
        ComponentRegistry.register("吸血", (Supplier<IRPGComponent>) LifestealComponent::new);
        ComponentRegistry.register("超新星", (Supplier<IRPGComponent>) ComponentSupernovaSet::new);
        ComponentRegistry.register("虹之愿", (Supplier<IRPGComponent>) ComponentLawOfCycles::new);
        ComponentRegistry.register("圆环之理", (Supplier<IRPGComponent>) ComponentTrueLawOfCycles::new);
        ComponentRegistry.register("血祭", (Supplier<IRPGComponent>) ComponentBloodSacrifice::new);
        ComponentRegistry.register("劫难", (Supplier<IRPGComponent>) ComponentCalamity::new);
        ComponentRegistry
            .register("灵气洪流", (Supplier<IRPGComponent>) com.greyhat.dark_grey.component.ComponentAuraTorrent::new);
        ComponentRegistry
            .register("炬火的残光", (Supplier<IRPGComponent>) com.greyhat.dark_grey.component.ComponentTorchAfterglow::new);
        ComponentRegistry
            .register("炬火残光", (Supplier<IRPGComponent>) com.greyhat.dark_grey.component.ComponentTorchAfterglow::new);
        ComponentRegistry.register("耀斑", (Supplier<IRPGComponent>) ComponentSolarFlare::new);
        ComponentRegistry
            .register("倒悬", (Supplier<IRPGComponent>) com.greyhat.dark_grey.component.ComponentSuspendedClockhand::new);
        DarkGrey.LOG.info("======== DarkGrey Mod: RPG 组件已注册 ========");
        RPGItemLoader.loadItemsFromData();
        DarkGrey.LOG.info("======== DarkGrey Mod: RPG 物品已加载 ========");
        EntityRegistry
            .registerModEntity((Class) EntityVampire.class, "vampire", 0, (Object) DarkGrey.instance, 64, 3, true);
        EntityRegistry.registerModEntity(
            (Class) EntityMadokaArrow.class,
            "madoka_arrow",
            1,
            (Object) DarkGrey.instance,
            64,
            2,
            true);
        EntityRegistry.registerModEntity(
            (Class) EntityMadokaRing.class,
            "madoka_ring",
            2,
            (Object) DarkGrey.instance,
            64,
            20,
            true);
        EntityRegistry.registerModEntity(
            (Class) EntityScythe.class,
            "calamity_scythe",
            3,
            (Object) DarkGrey.instance,
            64,
            20,
            true);
        EntityRegistry.registerModEntity(
            (Class) com.greyhat.dark_grey.entity.EntityAuraTorrent.class,
            "aura_torrent",
            4,
            (Object) DarkGrey.instance,
            64,
            20,
            true);
        EntityRegistry.registerModEntity(
            (Class) EntityPhantomStrike.class,
            "phantom_strike",
            5,
            (Object) DarkGrey.instance,
            64,
            20,
            true);
        DarkGrey.LOG.info("======== DarkGrey Mod: 吸血鬼实体已注册 ========");
    }

    @Mod.EventHandler
    public void init(final FMLInitializationEvent event) {
        DarkGrey.LOG.info("======== DarkGrey Mod: Init 阶段开始 ========");
        DarkGrey.proxy.registerRenderers();
        DarkGrey.proxy.init(event);
        DarkGrey.LOG.info("======== DarkGrey Mod: Init 阶段完成 ========");
        RPGCoreEventHandler eventHandler = new RPGCoreEventHandler();
        MinecraftForge.EVENT_BUS.register((Object) eventHandler);
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register((Object) eventHandler);
        DarkGrey.LOG.info("======== DarkGrey Mod: RPG 事件处理器已注册 ========");
    }

    @Mod.EventHandler
    public void postInit(final FMLPostInitializationEvent event) {
        DarkGrey.proxy.postInit(event);
        final Boolean isDevEnv = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
        if (isDevEnv != null && isDevEnv) {
            this.dumpEnchantments();
        }
    }

    private void dumpEnchantments() {
        final JsonArray enchantArray = new JsonArray();
        for (final Enchantment ench : Enchantment.enchantmentsList) {
            if (ench != null) {
                final JsonObject obj = new JsonObject();
                obj.addProperty("id", (Number) ench.effectId);
                obj.addProperty("name", StatCollector.translateToLocal(ench.getName()));
                obj.addProperty("unlocalizedName", ench.getName());
                enchantArray.add((JsonElement) obj);
            }
        }
        final File dumpFile = new File("../RPGItem/enchantments_dump.json");
        try (final FileWriter writer = new FileWriter(dumpFile)) {
            final Gson gson = new GsonBuilder().setPrettyPrinting()
                .create();
            gson.toJson((JsonElement) enchantArray, (Appendable) writer);
            DarkGrey.LOG.info(
                "======== DarkGrey Mod: \u6210\u529f\u5bfc\u51fa\u9644\u9b54\u5217\u8868\u5230 "
                    + dumpFile.getAbsolutePath()
                    + " ========");
        } catch (final IOException e) {
            DarkGrey.LOG.error("Failed to dump enchantments", (Throwable) e);
        }
    }

    @Mod.EventHandler
    public void serverStarting(final FMLServerStartingEvent event) {
        DarkGrey.proxy.serverStarting(event);
        event.registerServerCommand((ICommand) new CommandRPGReload());
        event.registerServerCommand((ICommand) new CommandRPGHelp());
        event.registerServerCommand((ICommand) new com.greyhat.dark_grey.command.CommandDarkGrey());
    }

    static {
        LOG = LogManager.getLogger("dark_grey");
    }
}
