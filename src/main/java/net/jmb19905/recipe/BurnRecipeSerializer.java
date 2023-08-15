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
        String tagString = json.get("input").getAsString();
        String blockString = json.get("output").getAsString();
        TagKey<Block> tag = TagKey.of(RegistryKeys.BLOCK, new Identifier(tagString));
        Block block = Registries.BLOCK.getOrEmpty(new Identifier(blockString))
                .orElseThrow(() -> new JsonSyntaxException("No such Block: " + blockString));
        return new BurnRecipe(id, tag, block);
    }

    @Override
    public BurnRecipe read(Identifier id, PacketByteBuf buf) {
        Identifier tagId = buf.readIdentifier();
        Identifier blockId = buf.readIdentifier();
        TagKey<Block> tag = TagKey.of(RegistryKeys.BLOCK, tagId);
        Block block = Registries.BLOCK.get(blockId);
        return new BurnRecipe(id, tag, block);
    }

    @Override
    public void write(PacketByteBuf buf, BurnRecipe recipe) {
        buf.writeIdentifier(recipe.input().id());
        buf.writeIdentifier(Registries.BLOCK.getId(recipe.result()));
    }
}
