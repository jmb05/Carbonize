package net.jmb19905;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.jmb19905.block.*;
import net.jmb19905.blockEntity.CharringWoodBlockEntity;
import net.jmb19905.config.CarbonizeConfig;
import net.jmb19905.recipe.BurnRecipe;
import net.jmb19905.recipe.BurnRecipeSerializer;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.Instrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Carbonize implements ModInitializer {

	public static final String MOD_ID = "carbonize";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final CarbonizeConfig CONFIG = CarbonizeConfig.createAndLoad();

	public static final Block WOOD_STACK = new StackBlock(FabricBlockSettings.create()
			.instrument(Instrument.BASS)
			.strength(2.0f)
			.sounds(BlockSoundGroup.WOOD)
			.nonOpaque()
			.burnable());

	public static final Block CHARCOAL_LOG = new FlammableFallingPillarBlock(FabricBlockSettings.create()
			.mapColor(state -> MapColor.BLACK)
			.instrument(Instrument.BASS)
			.strength(2.0f)
			.sounds(BlockSoundGroup.WOOD)
			.burnable());
	public static final Block CHARCOAL_PLANKS = new FlammableFallingBlock(FabricBlockSettings.create()
			.mapColor(state -> MapColor.BLACK)
			.instrument(Instrument.BASS)
			.strength(2.0f)
			.sounds(BlockSoundGroup.WOOD)
			.burnable());
	public static final Block CHARCOAL_STACK = new FlammableFallingStackBlock(FabricBlockSettings.copy(CHARCOAL_PLANKS).nonOpaque());
	public static final Block CHARCOAL_STAIRS = new FlammableFallingStairsBlock(CHARCOAL_PLANKS.getDefaultState(), FabricBlockSettings.copy(CHARCOAL_PLANKS));
	public static final Block CHARCOAL_SLAB = new FlammableFallingSlabBlock(FabricBlockSettings.copy(CHARCOAL_PLANKS));
	public static final Block CHARCOAL_FENCE = new FlammableFallingFenceBlock(FabricBlockSettings.copy(CHARCOAL_PLANKS));
	public static final Block CHARCOAL_FENCE_GATE = new FlammableFallingFenceGateBlock(FabricBlockSettings.copy(CHARCOAL_PLANKS));

	public static final Block ASH_LAYER = new AshBlock(FabricBlockSettings.create()
			.mapColor(MapColor.GRAY)
			.sounds(BlockSoundGroup.SAND)
			.replaceable()
			.notSolid()
			.ticksRandomly()
			.strength(0.1f)
			.blockVision((state, world, pos) -> state.get(SnowBlock.LAYERS) >= 8)
			.pistonBehavior(PistonBehavior.DESTROY)
			.ticksRandomly());
	public static final Block ASH_BLOCK = new FallingBlock(FabricBlockSettings.create()
			.mapColor(MapColor.GRAY)
			.sounds(BlockSoundGroup.SAND));
	public static final Block CHARRING_WOOD = new CharringWoodBlock(FabricBlockSettings.create().nonOpaque().luminance(15));
	public static final Block CHARRING_STACK = new StackBlock(FabricBlockSettings.create().nonOpaque().luminance(15));
	public static final Block CHARCOAL_BLOCK = new Block(FabricBlockSettings.copy(Blocks.COAL_BLOCK));

	public static final BlockItem WOOD_STACK_ITEM = new BlockItem(WOOD_STACK, new FabricItemSettings());
	public static final BlockItem CHARCOAL_STACK_ITEM = new BlockItem(CHARCOAL_STACK, new FabricItemSettings());
	public static final BlockItem CHARCOAL_LOG_ITEM = new BlockItem(CHARCOAL_LOG, new FabricItemSettings());
	public static final BlockItem CHARCOAL_PLANKS_ITEM = new BlockItem(CHARCOAL_PLANKS, new FabricItemSettings());
	public static final BlockItem CHARCOAL_STAIRS_ITEM = new BlockItem(CHARCOAL_STAIRS, new FabricItemSettings());
	public static final BlockItem CHARCOAL_SLAB_ITEM = new BlockItem(CHARCOAL_SLAB, new FabricItemSettings());
	public static final BlockItem CHARCOAL_FENCE_ITEM = new BlockItem(CHARCOAL_FENCE, new FabricItemSettings());
	public static final BlockItem CHARCOAL_FENCE_GATE_ITEM = new BlockItem(CHARCOAL_FENCE_GATE, new FabricItemSettings());

	public static final BlockItem ASH_LAYER_ITEM = new BlockItem(ASH_LAYER, new FabricItemSettings());
	public static final BlockItem ASH_BLOCK_ITEM = new BlockItem(ASH_BLOCK, new FabricItemSettings());
	public static final BlockItem CHARCOAL_BLOCK_ITEM = new BlockItem(CHARCOAL_BLOCK, new FabricItemSettings());

	public static final Item ASH = new BoneMealItem(new FabricItemSettings());

	public static final Identifier CHARRING_WOOD_ID = new Identifier(MOD_ID, "charring_wood");
	public static final BlockEntityType<CharringWoodBlockEntity> CHARRING_WOOD_TYPE = Registry.register(
			Registries.BLOCK_ENTITY_TYPE,
			CHARRING_WOOD_ID,
			FabricBlockEntityTypeBuilder.create(CharringWoodBlockEntity::new).addBlock(CHARRING_WOOD).build()
	);

	public static final RecipeType<BurnRecipe> BURN_RECIPE_TYPE = registerRecipeType(BurnRecipeSerializer.ID);

	public static final TagKey<Block> CHARCOAL_BLOCKS = TagKey.of(RegistryKeys.BLOCK, new Identifier(MOD_ID, "charcoal_blocks"));
	public static final TagKey<Block> CHARCOAL_PILE_VALID_WALL = TagKey.of(RegistryKeys.BLOCK, new Identifier(MOD_ID, "charcoal_pile_valid_wall"));
	public static final TagKey<Block> CHARCOAL_PILE_VALID_FUEL = TagKey.of(RegistryKeys.BLOCK, new Identifier(MOD_ID, "charcoal_pile_valid_fuel"));
	public static final TagKey<Item> DAMAGE_IGNITERS = TagKey.of(RegistryKeys.ITEM, new Identifier(MOD_ID, "damage_igniters"));
	public static final TagKey<Item> CONSUME_IGNITERS = TagKey.of(RegistryKeys.ITEM, new Identifier(MOD_ID, "consume_igniters"));
	public static final TagKey<Item> IGNITERS = TagKey.of(RegistryKeys.ITEM, new Identifier(MOD_ID, "igniters"));
	@Override
	public void onInitialize() {
		Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "wood_stack"), WOOD_STACK);
		Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "charcoal_stack"), CHARCOAL_STACK);
		Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "charcoal_log"), CHARCOAL_LOG);
		Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "charcoal_planks"), CHARCOAL_PLANKS);
		Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "charcoal_stairs"), CHARCOAL_STAIRS);
		Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "charcoal_slab"), CHARCOAL_SLAB);
		Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "charcoal_fence"), CHARCOAL_FENCE);
		Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "charcoal_fence_gate"), CHARCOAL_FENCE_GATE);

		Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "ash_layer"), ASH_LAYER);
		Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "ash_block"), ASH_BLOCK);
		Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "charring_wood"), CHARRING_WOOD);
		Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "charring_stack"), CHARRING_STACK);
		Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "charcoal_block"), CHARCOAL_BLOCK);
		LOGGER.debug("Registered Blocks");

		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "wood_stack"), WOOD_STACK_ITEM);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "charcoal_stack"), CHARCOAL_STACK_ITEM);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "charcoal_log"), CHARCOAL_LOG_ITEM);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "charcoal_planks"), CHARCOAL_PLANKS_ITEM);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "charcoal_stairs"), CHARCOAL_STAIRS_ITEM);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "charcoal_slab"), CHARCOAL_SLAB_ITEM);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "charcoal_fence"), CHARCOAL_FENCE_ITEM);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "charcoal_fence_gate"), CHARCOAL_FENCE_GATE_ITEM);

		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "ash_layer"), ASH_LAYER_ITEM);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "ash_block"), ASH_BLOCK_ITEM);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "charcoal_block"), CHARCOAL_BLOCK_ITEM);

		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "ash"), ASH);
		LOGGER.debug("Registered Items");

		Registry.register(Registries.RECIPE_SERIALIZER, BurnRecipeSerializer.ID, BurnRecipeSerializer.INSTANCE);

		FlammableBlockRegistry.getDefaultInstance().add(CHARCOAL_FENCE_GATE, 15, 30);
		FlammableBlockRegistry.getDefaultInstance().add(CHARCOAL_FENCE, 15, 30);
		FlammableBlockRegistry.getDefaultInstance().add(CHARCOAL_PLANKS, 15, 30);
		FlammableBlockRegistry.getDefaultInstance().add(CHARCOAL_STAIRS, 15, 30);
		FlammableBlockRegistry.getDefaultInstance().add(CHARCOAL_SLAB, 15, 30);
		FlammableBlockRegistry.getDefaultInstance().add(CHARCOAL_BLOCK, 15, 30);
		FlammableBlockRegistry.getDefaultInstance().add(CHARCOAL_LOG, 15, 30);
		FlammableBlockRegistry.getDefaultInstance().add(CHARCOAL_STACK, 15, 30);
		FlammableBlockRegistry.getDefaultInstance().add(WOOD_STACK, 15, 30);
		FlammableBlockRegistry.getDefaultInstance().add(CHARRING_WOOD, 15, 30);
		FlammableBlockRegistry.getDefaultInstance().add(CHARRING_STACK, 15, 30);


		if (CONFIG.moreBurnableBlocks()) {
			if (CONFIG.burnableContainers()) {
				FlammableBlockRegistry.getDefaultInstance().add(Blocks.CHEST, 5, 5);
				FlammableBlockRegistry.getDefaultInstance().add(Blocks.TRAPPED_CHEST, 5, 5);
				FlammableBlockRegistry.getDefaultInstance().add(Blocks.JUKEBOX, 5, 5);
				FlammableBlockRegistry.getDefaultInstance().add(Blocks.BARREL, 5, 5);
			}

			FlammableBlockRegistry.getDefaultInstance().add(Blocks.NOTE_BLOCK, 5, 5);
			FlammableBlockRegistry.getDefaultInstance().add(Blocks.CRAFTING_TABLE, 5, 5);
			FlammableBlockRegistry.getDefaultInstance().add(Blocks.LADDER, 5, 5);
			FlammableBlockRegistry.getDefaultInstance().add(Blocks.CARTOGRAPHY_TABLE, 5, 5);
			FlammableBlockRegistry.getDefaultInstance().add(Blocks.FLETCHING_TABLE, 5, 5);
			FlammableBlockRegistry.getDefaultInstance().add(Blocks.SMITHING_TABLE, 5, 5);
			FlammableBlockRegistry.getDefaultInstance().add(Blocks.LOOM, 5, 5);

			FlammableBlockRegistry.getDefaultInstance().add(BlockTags.BANNERS, 5, 5);
			FlammableBlockRegistry.getDefaultInstance().add(BlockTags.BEDS, 5, 5);
			FlammableBlockRegistry.getDefaultInstance().add(BlockTags.WOODEN_DOORS, 5, 5);
			FlammableBlockRegistry.getDefaultInstance().add(BlockTags.WOODEN_TRAPDOORS, 5, 5);
			FlammableBlockRegistry.getDefaultInstance().add(BlockTags.WOODEN_BUTTONS, 5, 5);
			FlammableBlockRegistry.getDefaultInstance().add(BlockTags.WOODEN_PRESSURE_PLATES, 5, 5);
			FlammableBlockRegistry.getDefaultInstance().add(BlockTags.ALL_SIGNS, 5, 5);
		}

		FuelRegistry.INSTANCE.add(CHARCOAL_BLOCK, 16000);
		FuelRegistry.INSTANCE.add(CHARCOAL_LOG, 1600 * 4);
		FuelRegistry.INSTANCE.add(CHARCOAL_PLANKS, 1600 * 4);
		FuelRegistry.INSTANCE.add(CHARCOAL_STAIRS, 1600 * 3);
		FuelRegistry.INSTANCE.add(CHARCOAL_SLAB, 1600 * 2);

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(content -> {
			content.add(WOOD_STACK_ITEM);
			content.add(CHARCOAL_STACK_ITEM);
			content.add(CHARCOAL_LOG_ITEM);
			content.add(CHARCOAL_PLANKS_ITEM);
			content.add(CHARCOAL_STAIRS_ITEM);
			content.add(CHARCOAL_SLAB_ITEM);
			content.add(CHARCOAL_BLOCK_ITEM);
			content.add(CHARCOAL_FENCE_ITEM);
			content.add(CHARCOAL_FENCE_GATE_ITEM);
			content.add(ASH_LAYER_ITEM);
			content.add(ASH_BLOCK_ITEM);
		});

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(content -> content.add(ASH));

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (!CONFIG.charcoalPile()) return ActionResult.PASS;
			ItemStack stack = player.getStackInHand(hand);
			if (!world.isClient && stack.isIn(IGNITERS) && player.isSneaking()) {
				if (hitResult.getType() == HitResult.Type.BLOCK) {
					BlockPos pos = hitResult.getBlockPos();
					int blockCount = CharringWoodBlock.checkValid(world, pos, hitResult.getSide());
					if (blockCount >= CONFIG.charcoalPileMinimumCount() && handleIgnition(stack, player, hand)) {
						BlockState parentState = world.getBlockState(pos);
						world.playSound(null, pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 2, 1);
						world.setBlockState(pos, CHARRING_WOOD.getDefaultState().with(CharringWoodBlock.STAGE, CharringWoodBlock.Stage.IGNITING));
						world.getBlockEntity(pos, CHARRING_WOOD_TYPE).ifPresent(blockEntity -> blockEntity.createData(blockCount, parentState));
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

	@SuppressWarnings("SameParameterValue")
	private static <T extends Recipe<?>> RecipeType<T> registerRecipeType(final Identifier id) {
		return Registry.register(Registries.RECIPE_TYPE, id, new RecipeType<T>() {
			public String toString() {
				return id.getPath();
			}
		});
	}

}