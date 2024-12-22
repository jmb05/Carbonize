package net.jmb19905.charcoal_pit.block;

import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.jmb19905.charcoal_pit.CharcoalPitInit;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

import java.util.function.Supplier;

public class CharringWoodBlockModel extends ForwardingBakedModel {

    public CharringWoodBlockModel(BakedModel base) {
        this.wrapped = base;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
        var blockEntity = blockView.getBlockEntity(pos, CharcoalPitInit.CHARRING_WOOD_TYPE);
        if (blockEntity.isPresent()) {
            var medium = blockEntity.get().getRenderData();
            var model = MinecraftClient.getInstance().getBlockRenderManager().getModel(medium);
            if (!model.equals(this)) {
                model.emitBlockQuads(blockView, state, pos, randomSupplier, context);
                return;
            }
        }
        super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
    }
}
