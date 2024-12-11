package net.jmb19905.model;

import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.jmb19905.Carbonize;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

import java.util.function.Supplier;

public class CharringWoodBlockModel extends ForwardingBakedModel {
    private final BakedModel original;

    public CharringWoodBlockModel(BakedModel base) {
        this.original = base;
        setWrapped(base);
    }

    public void setWrapped(BakedModel model) {
        this.wrapped = model;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
        var blockEntity = blockView.getBlockEntity(pos, Carbonize.CHARRING_WOOD_TYPE);
        blockEntity.ifPresent(charringWoodBlockEntity -> wrapped = MinecraftClient.getInstance().getBlockRenderManager().getModel(charringWoodBlockEntity.getRenderData()));
        super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
    }
}
