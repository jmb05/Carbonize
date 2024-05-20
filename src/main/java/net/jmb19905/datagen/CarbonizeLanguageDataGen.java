package net.jmb19905.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.jmb19905.Carbonize;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class CarbonizeLanguageDataGen extends FabricLanguageProvider {

    protected CarbonizeLanguageDataGen(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, "en_us", registryLookup);
    }

    @Override
    public void generateTranslations(RegistryWrapper.WrapperLookup registryLookup, TranslationBuilder translationBuilder) {
        translationBuilder.add(Carbonize.CHARCOAL_PLANKS, "Charcoal Planks");
        translationBuilder.add(Carbonize.CHARCOAL_STAIRS, "Charcoal Stairs");
        translationBuilder.add(Carbonize.CHARCOAL_SLAB, "Charcoal Slab");
        translationBuilder.add(Carbonize.CHARCOAL_LOG, "Charcoal Log");
        translationBuilder.add(Carbonize.CHARCOAL_BLOCK, "Charcoal Block");
        translationBuilder.add(Carbonize.ASH_LAYER, "Ash Layer");
        translationBuilder.add(Carbonize.ASH_BLOCK, "Ash Block");
        translationBuilder.add(Carbonize.ASH, "Ash");

        translationBuilder.add("text.config.carbonize.option.moreBurnableBlocks", "More flammable blocks");
        translationBuilder.add("text.config.carbonize.option.burnableContainers", "Flammable containers");
        translationBuilder.add("text.config.carbonize.option.charcoalPile", "Charcoal pile");
        translationBuilder.add("text.config.carbonize.option.burnCrafting", "Block conversion by burning");
        translationBuilder.add("text.config.carbonize.option.increasedFireSpreadRange", "Increased fire spread range");

    }
}
