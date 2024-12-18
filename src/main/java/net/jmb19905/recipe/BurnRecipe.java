package net.jmb19905.recipe;

import net.jmb19905.Carbonize;
import net.minecraft.block.Block;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class BurnRecipe implements Recipe<SimpleInventory> {

    private final Identifier id;
    private final int burnTime;
    private final TagKey<Block> input;
    private final Block medium;
    private final Block result;


    public BurnRecipe(Identifier id, int burnTime, TagKey<Block> input, Block medium, Block result) {
        this.id = id;
        this.burnTime = burnTime;
        this.input = input;
        this.medium = medium;
        this.result = result;
    }

    public int burnTime () {
        return burnTime;
    }

    public TagKey<Block> input() {
        return input;
    }

    public Block medium() {
        return medium;
    }

    public Block result() {
        return result;
    }


    @Override
    public boolean matches(SimpleInventory inventory, World world) {
        return false;
    }

    @Override
    public ItemStack craft(SimpleInventory inventory, DynamicRegistryManager registryManager) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean fits(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getOutput(DynamicRegistryManager registryManager) {
        return ItemStack.EMPTY;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BurnRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Carbonize.BURN_RECIPE_TYPE;
    }

    @Override
    public String toString() {
        return  "BurnRecipe[" +
                    "id=" + id + ", " +
                    "input=" + input + ", " +
                    "medium=" + medium + ", " +
                    "result=" + result +
                "]@" + hashCode();
    }
}
