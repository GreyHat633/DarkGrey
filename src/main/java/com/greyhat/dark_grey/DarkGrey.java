// 
// Decompiled by Procyon v0.6.0
// 

package com.greyhat.dark_grey;

import org.apache.logging.log4j.LogManager;
import com.greyhat.dark_grey.command.CommandRPGHelp;
import net.minecraft.command.ICommand;
import com.greyhat.dark_grey.command.CommandRPGReload;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import com.google.gson.Gson;
import java.io.IOException;
import com.google.gson.GsonBuilder;
import java.io.FileWriter;
import java.io.File;
import com.google.gson.JsonElement;
import net.minecraft.util.StatCollector;
import com.google.gson.JsonObject;
import net.minecraft.enchantment.Enchantment;
import com.google.gson.JsonArray;
import net.minecraft.launchwrapper.Launch;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import com.greyhat.dark_grey.event.RPGCoreEventHandler;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import com.greyhat.dark_grey.entity.EntityScythe;
import com.greyhat.dark_grey.entity.EntityMadokaRing;
import com.greyhat.dark_grey.entity.EntityMadokaArrow;
import com.greyhat.dark_grey.common.EntityVampire;
import cpw.mods.fml.common.registry.EntityRegistry;
import com.greyhat.dark_grey.api.RPGItemLoader;
import com.greyhat.dark_grey.api.impl.ComponentCalamity;
import com.greyhat.dark_grey.api.impl.ComponentBloodSacrifice;
import com.greyhat.dark_grey.component.ComponentTrueLawOfCycles;
import com.greyhat.dark_grey.component.ComponentLawOfCycles;
import com.greyhat.dark_grey.component.ComponentSupernovaSet;
import com.greyhat.dark_grey.api.impl.LifestealComponent;
import com.greyhat.dark_grey.api.IRPGComponent;
import java.util.function.Supplier;
import com.greyhat.dark_grey.api.ComponentRegistry;
import com.greyhat.dark_grey.api.impl.CleaveComponent;
import com.greyhat.dark_grey.common.DarkGreyTab;
import cpw.mods.fml.common.registry.GameRegistry;
import com.greyhat.dark_grey.common.ItemTabIcon;
import com.greyhat.dark_grey.api.RPGItemDataManager;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.SidedProxy;
import com.greyhat.dark_grey.common.CommonProxy;
import org.apache.logging.log4j.Logger;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import cpw.mods.fml.common.Mod;

@Mod(modid = "dark_grey", version = "1.0", name = "DarkGrey", acceptedMinecraftVersions = "[1.7.10]")
public class DarkGrey
{
    public static final String MODID = "dark_grey";
    public static final String NAME = "DarkGrey";
    public static final String VERSION = "1.0";
    @Mod.Instance("dark_grey")
    public static DarkGrey instance;
    public static Item tabIcon;
    public static Item arrowOfLight;
    public static CreativeTabs creativeTab;
    public static final Logger LOG;
    @SidedProxy(clientSide = "com.greyhat.dark_grey.common.ClientProxy", serverSide = "com.greyhat.dark_grey.common.CommonProxy")
    public static CommonProxy proxy;
    
    @Mod.EventHandler
    public void preInit(final FMLPreInitializationEvent event) {
        DarkGrey.proxy.preInit(event);
        RPGItemDataManager.getInstance().initialize(event.getModConfigurationDirectory());
        GameRegistry.registerItem(DarkGrey.tabIcon = new ItemTabIcon(), "tab_icon");
        DarkGrey.creativeTab = new DarkGreyTab("DarkGrey");
        ComponentRegistry.register("\u5207\u5272", (Supplier<IRPGComponent>)CleaveComponent::new);
        ComponentRegistry.register("\u5438\u8840", (Supplier<IRPGComponent>)LifestealComponent::new);
        ComponentRegistry.register("\u8d85\u65b0\u661f", (Supplier<IRPGComponent>)ComponentSupernovaSet::new);
        ComponentRegistry.register("\u8679\u4e4b\u613f", (Supplier<IRPGComponent>)ComponentLawOfCycles::new);
        ComponentRegistry.register("\u5706\u73af\u4e4b\u7406", (Supplier<IRPGComponent>)ComponentTrueLawOfCycles::new);
        ComponentRegistry.register("\u8840\u796d", (Supplier<IRPGComponent>)ComponentBloodSacrifice::new);
        ComponentRegistry.register("\u52ab\u96be", (Supplier<IRPGComponent>)ComponentCalamity::new);
        ComponentRegistry.register("\u7075\u6c14\u6d2a\u6d41", (Supplier<IRPGComponent>)com.greyhat.dark_grey.component.ComponentAuraTorrent::new);
        ComponentRegistry.register("\u70AC\u706B\u7684\u6B8B\u5149", (Supplier<IRPGComponent>)com.greyhat.dark_grey.api.impl.ComponentTorchAfterglow::new);
        ComponentRegistry.register("\u70AC\u706B\u6B8B\u5149", (Supplier<IRPGComponent>)com.greyhat.dark_grey.api.impl.ComponentTorchAfterglow::new);
        DarkGrey.LOG.info("======== DarkGrey Mod: RPG \u7ec4\u4ef6\u5df2\u6ce8\u518c ========");
        RPGItemLoader.loadItemsFromData();
        DarkGrey.LOG.info("======== DarkGrey Mod: RPG \u7269\u54c1\u5df2\u52a0\u8f7d ========");
        final int randomEntityId = EntityRegistry.findGlobalUniqueEntityId();
        EntityRegistry.registerGlobalEntityID((Class)EntityVampire.class, "vampire", randomEntityId, 0, 16711680);
        EntityRegistry.registerModEntity((Class)EntityMadokaArrow.class, "madoka_arrow", randomEntityId + 1, (Object)DarkGrey.instance, 64, 20, true);
        EntityRegistry.registerModEntity((Class)EntityMadokaRing.class, "madoka_ring", randomEntityId + 2, (Object)DarkGrey.instance, 64, 20, true);
        EntityRegistry.registerModEntity((Class)EntityScythe.class, "calamity_scythe", randomEntityId + 3, (Object)DarkGrey.instance, 64, 20, true);
        EntityRegistry.registerModEntity((Class)com.greyhat.dark_grey.entity.EntityAuraTorrent.class, "aura_torrent", randomEntityId + 4, (Object)DarkGrey.instance, 64, 20, true);
        DarkGrey.LOG.info("======== DarkGrey Mod: \u5438\u8840\u9b3c\u5b9e\u4f53\u5df2\u6ce8\u518c ========");
    }
    
    @Mod.EventHandler
    public void init(final FMLInitializationEvent event) {
        DarkGrey.LOG.info("======== DarkGrey Mod: Init \u9636\u6bb5\u5f00\u59cb ========");
        DarkGrey.proxy.registerRenderers();
        DarkGrey.proxy.init(event);
        DarkGrey.LOG.info("======== DarkGrey Mod: Init \u9636\u6bb5\u5b8c\u6210 ========");
        MinecraftForge.EVENT_BUS.register((Object)new RPGCoreEventHandler());
        DarkGrey.LOG.info("======== DarkGrey Mod: RPG \u4e8b\u4ef6\u5904\u7406\u5668\u5df2\u6ce8\u518c ========");
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
                obj.addProperty("id", (Number)ench.effectId);
                obj.addProperty("name", StatCollector.translateToLocal(ench.getName()));
                obj.addProperty("unlocalizedName", ench.getName());
                enchantArray.add((JsonElement)obj);
            }
        }
        final File dumpFile = new File("../RPGItem/enchantments_dump.json");
        try (final FileWriter writer = new FileWriter(dumpFile)) {
            final Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson((JsonElement)enchantArray, (Appendable)writer);
            DarkGrey.LOG.info("======== DarkGrey Mod: \u6210\u529f\u5bfc\u51fa\u9644\u9b54\u5217\u8868\u5230 " + dumpFile.getAbsolutePath() + " ========");
        }
        catch (final IOException e) {
            DarkGrey.LOG.error("Failed to dump enchantments", (Throwable)e);
        }
    }
    
    @Mod.EventHandler
    public void serverStarting(final FMLServerStartingEvent event) {
        DarkGrey.proxy.serverStarting(event);
        event.registerServerCommand((ICommand)new CommandRPGReload());
        event.registerServerCommand((ICommand)new CommandRPGHelp());
    }
    
    static {
        LOG = LogManager.getLogger("dark_grey");
    }
}
