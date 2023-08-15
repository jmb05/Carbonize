package net.jmb19905.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.jmb19905.Carbonize;

public class CarbonizeLanguageDataGen extends FabricLanguageProvider {

    protected CarbonizeLanguageDataGen(FabricDataOutput dataOutput) {
        super(dataOutput, "en_us");
    }

    @Override
    public void generateTranslations(TranslationBuilder translationBuilder) {
        translationBuilder.add(Carbonize.CHARCOAL_PLANKS, "Charcoal Planks");
        translationBuilder.add(Carbonize.CHARCOAL_STAIRS, "Charcoal Planks Stairs");
        translationBuilder.add(Carbonize.CHARCOAL_LOG, "Charcoal Log");
        translationBuilder.add(Carbonize.CHARCOAL_BLOCK, "Charcoal Block");
        translationBuilder.add(Carbonize.ASH_LAYER, "Ash Layer");
        translationBuilder.add(Carbonize.ASH_BLOCK, "Ash Block");
        translationBuilder.add(Carbonize.ASH, "Ash");
    }
}
