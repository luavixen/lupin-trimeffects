package dev.foxgirl.trimeffects;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;

@Mod("trimeffects")
public final class TrimEffectsMod {

    public TrimEffectsMod(IEventBus eventBus) {
        eventBus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        TrimEffects2.INSTANCE.initialize(FMLPaths.CONFIGDIR.get());
    }

}
