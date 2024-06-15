package dev.foxgirl.trimeffects;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.impl.FabricLoaderImpl;

public final class TrimEffectsMod implements ModInitializer {

    public TrimEffectsMod() {
        TrimEffects.createInstance();
    }

    @Override
    public void onInitialize() {
        TrimEffects.getInstance().initialize(FabricLoaderImpl.INSTANCE.getConfigDir());
    }

}
