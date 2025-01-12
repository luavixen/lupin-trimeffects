package dev.foxgirl.trimeffects;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.impl.FabricLoaderImpl;

public final class TrimEffectsMod implements ModInitializer {

    public TrimEffectsMod() {
    }

    @Override
    public void onInitialize() {
        TrimEffects2.INSTANCE.initialize(FabricLoaderImpl.INSTANCE.getConfigDir());
    }

}
