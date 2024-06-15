package dev.foxgirl.trimeffects;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.fml.loading.FMLPaths;

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
