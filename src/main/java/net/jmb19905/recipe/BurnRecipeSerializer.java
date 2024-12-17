package net.jmb19905.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.jmb19905.Carbonize;
import net.minecraft.block.Block;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class BurnRecipeSerializer implements RecipeSerializer<BurnRecipe> {

    public static final BurnRecipeSerializer INSTANCE = new BurnRecipeSerializer();
    public static final Identifier ID = new Identifier(Carbonize.MOD_ID, "burn");

    @Override
    public BurnRecipe read(Identifier id, JsonObject json) {
        String rawInput = json.get("input").getAsString();
        String rawMedium = json.has("medium") ? json.get("medium").getAsString() : null;
        String rawOutput = json.get("output").getAsString();
        TagKey<Block> input = TagKey.of(RegistryKeys.BLOCK, new Identifier(rawInput));
        Block medium = rawMedium != null ? findBlock(rawMedium): null;
        Block output = findBlock(rawOutput);
        return new BurnRecipe(id, input, medium, output);
    }

    @Override
    public BurnRecipe read(Identifier id, PacketByteBuf buf) {
        Identifier idInput = buf.readIdentifier();
        Identifier idMedium = buf.readIdentifier();
        Identifier idOutput = buf.readIdentifier();
        TagKey<Block> input = TagKey.of(RegistryKeys.BLOCK, idInput);
        Block medium = Registries.BLOCK.get(idOutput);
        Block output = Registries.BLOCK.get(idMedium);
        return new BurnRecipe(id, input, medium, output);
    }

    @Override
    public void write(PacketByteBuf buf, BurnRecipe recipe) {
        buf.writeIdentifier(recipe.input().id());
        buf.writeIdentifier(Registries.BLOCK.getId(recipe.medium()));
        buf.writeIdentifier(Registries.BLOCK.getId(recipe.result()));
    }

    private static Block findBlock(String identifier) {
        return identifier == null ? null :
                Registries.BLOCK.getOrEmpty(new Identifier(identifier)).orElseThrow(() -> new JsonSyntaxException("No such Block: " + identifier));
    }
}
