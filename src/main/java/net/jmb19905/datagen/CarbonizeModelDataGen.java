package net.jmb19905.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.jmb19905.Carbonize;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.data.client.Models;

public class CarbonizeModelDataGen extends FabricModelProvider {

    public CarbonizeModelDataGen(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        blockStateModelGenerator.registerLog(Carbonize.CHARCOAL_LOG).log(Carbonize.CHARCOAL_LOG);
        blockStateModelGenerator.registerSimpleCubeAll(Carbonize.CHARCOAL_PLANKS);
        blockStateModelGenerator.registerSimpleCubeAll(Carbonize.CHARCOAL_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(Carbonize.CHARRING_WOOD);
    }


    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        itemModelGenerator.register(Carbonize.ASH, Models.GENERATED);
    }
}
