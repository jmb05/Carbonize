package net.jmb19905.model;

import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
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
        var mimic = blockView.getBlockEntityRenderData(pos);
        if (mimic instanceof BlockState mimicState) {
            var model = MinecraftClient.getInstance().getBlockRenderManager().getModel(mimicState);
            if (!equals(model))
                synchronized (this) {
                    this.wrapped = model;
                }
        }
        super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
    }
}
