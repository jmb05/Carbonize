package net.jmb19905.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.jmb19905.Carbonize;
import net.jmb19905.block.AshBlock;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.BlockStatePropertyLootCondition;
import net.minecraft.loot.condition.EntityPropertiesLootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.entry.AlternativeEntry;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LeafEntry;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.predicate.StatePredicate;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class CarbonizeLootDataGen extends FabricBlockLootTableProvider {

    protected CarbonizeLootDataGen(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        this.addDrop(Carbonize.ASH_BLOCK, block -> this.drops(block, Carbonize.ASH, ConstantLootNumberProvider.create(4)));
        this.addDrop(Carbonize.ASH_LAYER, block -> LootTable.builder().pool(LootPool.builder().conditionally(EntityPropertiesLootCondition.create(LootContext.EntityTarget.THIS)).with(AlternativeEntry.builder(AlternativeEntry.builder(AshBlock.LAYERS.getValues(), integer -> ((LeafEntry.Builder<?>) ItemEntry.builder(Carbonize.ASH).conditionally(BlockStatePropertyLootCondition.builder(block).properties(StatePredicate.Builder.create().exactMatch(AshBlock.LAYERS, integer)))).apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(integer)))).conditionally(WITHOUT_SILK_TOUCH), AlternativeEntry.builder(AshBlock.LAYERS.getValues(), integer -> integer == 8 ? ItemEntry.builder(Carbonize.ASH_BLOCK) : ((LootPoolEntry.Builder<?>) ItemEntry.builder(Carbonize.ASH_LAYER).apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(integer)))).conditionally(BlockStatePropertyLootCondition.builder(block).properties(StatePredicate.Builder.create().exactMatch(AshBlock.LAYERS, integer))))))));
        this.addDrop(Carbonize.CHARCOAL_LOG, block -> this.drops(block, Items.CHARCOAL, ConstantLootNumberProvider.create(4)));
        this.addDrop(Carbonize.CHARCOAL_PLANKS, block -> this.drops(block, Items.CHARCOAL, ConstantLootNumberProvider.create(4)));
        this.addDrop(Carbonize.CHARCOAL_STAIRS, block -> this.drops(block, Items.CHARCOAL, ConstantLootNumberProvider.create(3)));
        this.addDrop(Carbonize.CHARCOAL_SLAB, block -> this.drops(block, Items.CHARCOAL, ConstantLootNumberProvider.create(2)));
        this.addDrop(Carbonize.CHARCOAL_BLOCK);
        this.excludeFromStrictValidation(Carbonize.CHARRING_WOOD);
    }
}
