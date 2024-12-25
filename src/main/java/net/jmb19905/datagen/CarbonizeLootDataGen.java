package net.jmb19905.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.jmb19905.Carbonize;
import net.jmb19905.block.AshBlock;
import net.jmb19905.charcoal_pit.CharcoalPitInit;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.BlockStatePropertyLootCondition;
import net.minecraft.loot.condition.EntityPropertiesLootCondition;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.entry.AlternativeEntry;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LeafEntry;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.BinomialLootNumberProvider;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.predicate.StatePredicate;

public class CarbonizeLootDataGen extends FabricBlockLootTableProvider {

    protected CarbonizeLootDataGen(FabricDataOutput dataOutput) {
        super(dataOutput);
    }

    @Override
    public void generate() {
        this.addDrop(Carbonize.ASH_BLOCK, block -> this.drops(block, Carbonize.ASH, ConstantLootNumberProvider.create(4)));
        this.addDrop(Carbonize.ASH_LAYER, block -> LootTable.builder().pool(LootPool.builder().conditionally(EntityPropertiesLootCondition.create(LootContext.EntityTarget.THIS)).with(AlternativeEntry.builder(AlternativeEntry.builder(AshBlock.LAYERS.getValues(), integer -> ((LeafEntry.Builder<?>) ItemEntry.builder(Carbonize.ASH).conditionally(BlockStatePropertyLootCondition.builder(block).properties(StatePredicate.Builder.create().exactMatch(AshBlock.LAYERS, integer)))).apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(integer)))).conditionally(WITHOUT_SILK_TOUCH), AlternativeEntry.builder(AshBlock.LAYERS.getValues(), integer -> integer == 8 ? ItemEntry.builder(Carbonize.ASH_BLOCK) : ((LootPoolEntry.Builder<?>) ItemEntry.builder(Carbonize.ASH_LAYER).apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(integer)))).conditionally(BlockStatePropertyLootCondition.builder(block).properties(StatePredicate.Builder.create().exactMatch(AshBlock.LAYERS, integer))))))));
        this.addDrop(Carbonize.WOOD_STACK, block -> this.drops(block, Items.STICK, UniformLootNumberProvider.create(6, 8)));
        this.addDrop(Carbonize.CHARCOAL_STACK, block -> dropsWithSilkTouch(block, this.applyExplosionDecay(block, ItemEntry.builder(Items.CHARCOAL).apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(3, 4))).apply(ApplyBonusLootFunction.uniformBonusCount(Enchantments.FORTUNE)))));
        this.addDrop(Carbonize.CHARCOAL_LOG, block -> dropsWithSilkTouch(block, this.applyExplosionDecay(block, ItemEntry.builder(Items.CHARCOAL).apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(2))).apply(ApplyBonusLootFunction.uniformBonusCount(Enchantments.FORTUNE).conditionally(RandomChanceLootCondition.builder(0.5f))))));
        this.addDrop(Carbonize.CHARCOAL_PLANKS, block -> dropsWithSilkTouch(block, this.applyExplosionDecay(block, ItemEntry.builder(Items.CHARCOAL).apply(SetCountLootFunction.builder(BinomialLootNumberProvider.create(1, 0.5f))))));
        this.addDrop(Carbonize.CHARCOAL_FENCE, block -> dropsWithSilkTouch(block, this.applyExplosionDecay(block, ItemEntry.builder(Items.CHARCOAL).apply(SetCountLootFunction.builder(BinomialLootNumberProvider.create(1, 0.5f))))));
        this.addDrop(Carbonize.CHARCOAL_FENCE_GATE, block -> dropsWithSilkTouch(block, this.applyExplosionDecay(block, ItemEntry.builder(Items.CHARCOAL).apply(SetCountLootFunction.builder(BinomialLootNumberProvider.create(1, 0.5f))))));
        this.addDrop(Carbonize.CHARCOAL_STAIRS, block -> dropsWithSilkTouch(block, this.applyExplosionDecay(block, ItemEntry.builder(Items.CHARCOAL).apply(SetCountLootFunction.builder(BinomialLootNumberProvider.create(1, 0.375f))))));
        this.addDrop(Carbonize.CHARCOAL_SLAB, block -> LootTable.builder().pool(LootPool.builder().rolls(ConstantLootNumberProvider.create(1.0F))
                .with(this.applyExplosionDecay(block, ItemEntry.builder(block)
                        .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(2.0F))
                                .conditionally(BlockStatePropertyLootCondition.builder(block).properties(StatePredicate.Builder.create().exactMatch(SlabBlock.TYPE, SlabType.DOUBLE)))
                        ).conditionally(WITH_SILK_TOUCH))
                )
                .with(this.applyExplosionDecay(block, ItemEntry.builder(Items.CHARCOAL))
                        .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(2.0F))
                                .conditionally(BlockStatePropertyLootCondition.builder(block).properties(StatePredicate.Builder.create().exactMatch(SlabBlock.TYPE, SlabType.DOUBLE)))
                        ).conditionally(WITHOUT_SILK_TOUCH).conditionally(RandomChanceLootCondition.builder(0.25f))
                )
        ));
        this.addDrop(Carbonize.CHARCOAL_BLOCK);
        this.excludeFromStrictValidation(CharcoalPitInit.CHARRING_WOOD);
    }
}
