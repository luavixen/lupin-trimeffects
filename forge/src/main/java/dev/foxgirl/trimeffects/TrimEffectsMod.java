package dev.foxgirl.trimeffects;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod("trimeffects")
public final class TrimEffectsMod {

    public TrimEffectsMod() {
        TrimEffects.createInstance();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        TrimEffects.getInstance().initialize(FMLPaths.CONFIGDIR.get());
    }

}
