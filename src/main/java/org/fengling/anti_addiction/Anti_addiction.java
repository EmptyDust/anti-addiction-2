package org.fengling.anti_addiction;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Anti_addiction.MODID)
public class Anti_addiction
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "anti_addiction";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    private static Anti_addiction INSTANCE;

    public static Anti_addiction getInstance() {
        return INSTANCE;
    }

    public Anti_addiction()
    {
        INSTANCE = this;

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(new PlayTimeKick());
        MinecraftForge.EVENT_BUS.register(new CommandRegistry());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
    }
}
