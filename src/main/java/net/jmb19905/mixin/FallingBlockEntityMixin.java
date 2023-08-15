package net.jmb19905.mixin;

import net.jmb19905.Carbonize;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin extends Entity {

    @Shadow
    private boolean destroyedOnLanding;
    @Shadow @Final private static Logger LOGGER;
    @Unique
    private static final TrackedData<Boolean> ON_FIRE = DataTracker.registerData(FallingBlockEntityMixin.class, TrackedDataHandlerRegistry.BOOLEAN);

    public FallingBlockEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    public void initDataTracker$inject(CallbackInfo ci) {
        this.dataTracker.startTracking(ON_FIRE, false);
    }

    @Override
    public void setOnFire(boolean onFire) {
        this.dataTracker.set(ON_FIRE, onFire);
        super.setOnFire(onFire);
        LOGGER.info("Set on fire: " + onFire);
    }

    public boolean isOnFire() {
        boolean bl = this.getWorld() != null && this.getWorld().isClient;
        return (this.dataTracker.get(ON_FIRE) || bl && this.getFlag(ON_FIRE_FLAG_INDEX));
    }

    @Override
    public boolean doesRenderOnFire() {
        return this.dataTracker.get(ON_FIRE);
    }

    @Inject(at = @At(value = "HEAD"), method = "tick")
    private void tick$setOnFire(CallbackInfo ci) {
        if (this.dataTracker.get(ON_FIRE) && !this.getFlag(ON_FIRE_FLAG_INDEX)) {
            setOnFire(true);
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/FallingBlockEntity;onDestroyedOnLanding(Lnet/minecraft/block/Block;Lnet/minecraft/util/math/BlockPos;)V", shift = At.Shift.AFTER), method = "tick", cancellable = true)
    private void tick$dontDropAshLayer(CallbackInfo ci) {
        var entity = (FallingBlockEntity) (Object) this;
        var pos = entity.getBlockPos();
        var world = entity.getWorld();
        var state = world.getBlockState(pos);
        if (destroyedOnLanding || state.isOf(Carbonize.ASH_LAYER)) {
            this.setVelocity(this.getVelocity().multiply(0.98));
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/FallingBlockEntity;dropItem(Lnet/minecraft/item/ItemConvertible;)Lnet/minecraft/entity/ItemEntity;"), cancellable = true)
    private void tick$dropAsItem(CallbackInfo ci){
        FallingBlockEntity instance = (FallingBlockEntity) (Object) this;
        if (instance.getBlockState().isOf(Carbonize.ASH_LAYER) || instance.getBlockState().isOf(Carbonize.CHARCOAL_LOG) || instance.getBlockState().isOf(Carbonize.CHARCOAL_PLANKS)) {
            Block.dropStacks(instance.getBlockState(), instance.getWorld(), instance.getBlockPos());
            this.setVelocity(this.getVelocity().multiply(0.98));
            ci.cancel();
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    private void writeNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("Fire", this.dataTracker.get(ON_FIRE));
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    private void readNbt(NbtCompound nbt, CallbackInfo ci) {
        this.dataTracker.set(ON_FIRE, nbt.getBoolean("Fire"));
    }

}
