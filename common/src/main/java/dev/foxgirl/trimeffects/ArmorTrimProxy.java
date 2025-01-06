package dev.foxgirl.trimeffects;

import net.minecraft.item.trim.ArmorTrimMaterial;
import net.minecraft.item.trim.ArmorTrimPattern;
import net.minecraft.registry.entry.RegistryEntry;

public interface ArmorTrimProxy {
    RegistryEntry<ArmorTrimMaterial> trimeffects$getMaterial();
    RegistryEntry<ArmorTrimPattern> trimeffects$getPattern();
}
