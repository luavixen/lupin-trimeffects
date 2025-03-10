package dev.foxgirl.trimeffects;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod("trimeffects")
public final class TrimEffectsMod {

    public TrimEffectsMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        TrimEffects2.INSTANCE.initialize(FMLPaths.CONFIGDIR.get());
    }

}
