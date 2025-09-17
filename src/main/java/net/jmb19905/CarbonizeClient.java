package net.jmb19905;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.jmb19905.charcoal_pit.block.CharringWoodBlockModel;
import static net.jmb19905.Carbonize.*;
import static net.jmb19905.charcoal_pit.CharcoalPitInit.CHARRING_WOOD_ID;

public class CarbonizeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLoadingPlugin.register(pluginContext -> pluginContext.modifyModelAfterBake().register((model, bakeContext) -> {
			if (bakeContext.id().getNamespace().equals(MOD_ID) && bakeContext.id().getPath().equals(CHARRING_WOOD_ID.getPath())) {
				return new CharringWoodBlockModel(model);
			} else return model;
		}));
	}
}