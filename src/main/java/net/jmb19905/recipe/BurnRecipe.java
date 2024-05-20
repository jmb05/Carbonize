package net.jmb19905.recipe;

import net.jmb19905.Carbonize;
import net.minecraft.block.Block;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.World;

public record BurnRecipe(TagKey<Block> input, Block result) implements Recipe<SimpleInventory> {

    @Override
    public boolean matches(SimpleInventory inventory, World world) {
        return false;
    }

    @Override
    public ItemStack craft(SimpleInventory inventory, RegistryWrapper.WrapperLookup lookup) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean fits(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registriesLookup) {
        return null;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BurnRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Carbonize.BURN_RECIPE_TYPE;
    }

}
