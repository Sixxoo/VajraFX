package cz.sixxo.vajrafx;

import net.minecraft.item.Item;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        VajraConfig.load(event.getSuggestedConfigurationFile());
        VajraState state = VajraState.INSTANCE;
        MinecraftForge.EVENT_BUS.register(state); // texture stitch, world render
        FMLCommonHandler.instance()
            .bus()
            .register(state); // client tick, render tick
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        Item vajra = findVajra();
        if (vajra == null) {
            FMLLog.warning("[VajraFX] GregTech Vajra not found - animation disabled.");
            return;
        }
        VajraState.INSTANCE.vajra = vajra;
        VajraState.INSTANCE.renderer = new VajraRenderer();
        MinecraftForgeClient.registerItemRenderer(vajra, VajraState.INSTANCE.renderer);
        FMLLog.info("[VajraFX] Mining animation enabled for %s", Item.itemRegistry.getNameForObject(vajra));
    }

    /** Finds the Vajra by its class name, so the mod does not need GregTech to compile. */
    private static Item findVajra() {
        for (Object o : Item.itemRegistry) {
            if (o instanceof Item && "ToolVajra".equals(
                o.getClass()
                    .getSimpleName()))
                return (Item) o;
        }
        return null;
    }
}
