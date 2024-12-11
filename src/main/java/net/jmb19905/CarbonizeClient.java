package net.jmb19905;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.jmb19905.model.CharringWoodBlockModel;
import net.minecraft.client.util.ModelIdentifier;

public class CarbonizeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLoadingPlugin.register(pluginContext -> pluginContext.modifyModelAfterBake().register((model, bakeContext) -> {
			if (bakeContext.id().equals(new ModelIdentifier(Carbonize.CHARRING_WOOD_ID, ""))) {
				return new CharringWoodBlockModel(model);
			} else return model;
		}));
	}
}