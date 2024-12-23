package net.jmb19905.charcoal_pit;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.jmb19905.block.StackBlock;
import net.jmb19905.charcoal_pit.block.CharringWoodBlock;
import net.jmb19905.charcoal_pit.block.CharringWoodBlockEntity;
import net.jmb19905.charcoal_pit.multiblock.CharcoalPitManager;
import net.jmb19905.charcoal_pit.multiblock.CharcoalPitMultiblock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import static net.jmb19905.Carbonize.*;

public class CharcoalPitInit {
    public static final Block CHARRING_WOOD = new CharringWoodBlock(FabricBlockSettings.create().nonOpaque().luminance(15).sounds(BlockSoundGroup.WOOD).dropsNothing());
    public static final Identifier CHARRING_WOOD_ID = new Identifier(MOD_ID, "charring_wood");
    public static final BlockEntityType<CharringWoodBlockEntity> CHARRING_WOOD_TYPE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            CHARRING_WOOD_ID,
            FabricBlockEntityTypeBuilder.create(CharringWoodBlockEntity::new).addBlock(CHARRING_WOOD).build()
    );
    public static final Block CHARRING_STACK = new StackBlock(FabricBlockSettings.create().nonOpaque());

    public static final BlockItem CHARRING_WOOD_ITEM = new BlockItem(CHARRING_WOOD, new FabricItemSettings());

    public static void init() {
        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "charring_wood"), CHARRING_WOOD);
        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "charring_stack"), CHARRING_STACK);

        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "charring_wood"), CHARRING_WOOD_ITEM);
        FlammableBlockRegistry.getDefaultInstance().add(CHARRING_WOOD, 15, 30);
        FlammableBlockRegistry.getDefaultInstance().add(CHARRING_STACK, 15, 30);

        ServerTickEvents.START_WORLD_TICK.register(world -> CharcoalPitManager.get(world).tick());
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!CONFIG.charcoalPile()) return ActionResult.PASS;
            ItemStack stack = player.getStackInHand(hand);
            if (!world.isClient && stack.isIn(IGNITERS) && player.isSneaking()) {
                if (hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockPos pos = hitResult.getBlockPos();
                    int blockCount = CharcoalPitMultiblock.collectFuels((ServerWorld) world, pos, hitResult.getSide()).size();
                    if (blockCount >= CONFIG.charcoalPileMinimumCount() && handleIgnition(stack, player, hand)) {
                        BlockState parentState = world.getBlockState(pos);
                        world.playSound(null, pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 2, 1);
                        world.setBlockState(pos, CHARRING_WOOD.getDefaultState());
                        world.getBlockEntity(pos, CHARRING_WOOD_TYPE).ifPresent(blockEntity -> blockEntity.sync(parentState));
                        return ActionResult.CONSUME;
                    }
                }
            }
            return ActionResult.PASS;
        });
    }

    private static boolean handleIgnition(ItemStack stack, PlayerEntity player, Hand hand) {
        if (stack.isIn(DAMAGE_IGNITERS)) {
            stack.damage(stack.getDamage() + 1, player, p -> p.sendToolBreakStatus(hand));
            return true;
        }

        if (stack.isIn(CONSUME_IGNITERS)) {
            stack.decrement(1);
            return true;
        }

        return false;
    }
}
