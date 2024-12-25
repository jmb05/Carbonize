package net.jmb19905.mixin;

import net.jmb19905.Carbonize;
import net.jmb19905.charcoal_pit.multiblock.CharcoalPitManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.RandomSequencesState;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

@SuppressWarnings("rawtypes")
@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {
    @Unique private CharcoalPitManager pitManager;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void carbonize$loadMultiBlock(MinecraftServer server, Executor workerExecutor, LevelStorage.Session session, ServerWorldProperties properties, RegistryKey worldKey, DimensionOptions dimensionOptions, WorldGenerationProgressListener worldGenerationProgressListener, boolean debugWorld, long seed, List spawners, boolean shouldTickTime, RandomSequencesState randomSequencesState, CallbackInfo ci) {
        Carbonize.LOGGER.info("Loading charcoal pit data for '" + this + "'/" + ((ServerWorld)(Object)this).getRegistryKey().getValue());
        pitManager = CharcoalPitManager.get((ServerWorld) (Object) this);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void carbonize$tickMultiBlock(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if (pitManager != null)
            pitManager.tick();
        else pitManager = CharcoalPitManager.get((ServerWorld) (Object) this);
    }

    @Inject(method = "saveLevel", at = @At("HEAD"))
    private void carbonize$saveMultiBlock(CallbackInfo ci) {
        if (pitManager != null) {
            Carbonize.LOGGER.info("Saving charcoal pit data for '" + this + "'/" + ((ServerWorld)(Object)this).getRegistryKey().getValue());
            pitManager.markDirty();
        }
    }
}
