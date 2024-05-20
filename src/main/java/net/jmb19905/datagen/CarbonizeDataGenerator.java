package net.jmb19905.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class CarbonizeDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		//fixme: DataGen is broken
		var pack = fabricDataGenerator.createPack();
		pack.addProvider(CarbonizeModelDataGen::new);
		pack.addProvider(CarbonizeLanguageDataGen::new);
		pack.addProvider(CarbonizeLootDataGen::new);
		pack.addProvider(CarbonizeRecipeDataGen::new);
	}
}
