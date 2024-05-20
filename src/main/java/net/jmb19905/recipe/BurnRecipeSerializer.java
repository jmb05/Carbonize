package net.jmb19905.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.jmb19905.Carbonize;
import net.minecraft.block.Block;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class BurnRecipeSerializer implements RecipeSerializer<BurnRecipe> {

    private static final MapCodec<BurnRecipe> CODEC = RecordCodecBuilder.mapCodec((instance) ->
            instance.group(
                    TagKey.codec(RegistryKeys.BLOCK)
                            .fieldOf("input")
                            .forGetter(BurnRecipe::input),
                    Identifier.CODEC
                            .fieldOf("output")
                            .forGetter((recipe) ->
                                    Registries.BLOCK.getId(recipe.result())))
                    .apply(instance, (blockTagKey, identifier) -> new BurnRecipe(blockTagKey, Registries.BLOCK.getOrEmpty(identifier).orElseThrow())));
    public static final PacketCodec<RegistryByteBuf, BurnRecipe> PACKET_CODEC = PacketCodec.ofStatic(BurnRecipeSerializer::write, BurnRecipeSerializer::read);

    public static final BurnRecipeSerializer INSTANCE = new BurnRecipeSerializer();
    public static final Identifier ID = new Identifier(Carbonize.MOD_ID, "burn");

    public static BurnRecipe read(RegistryByteBuf buf) {
        Identifier tagId = Identifier.PACKET_CODEC.decode(buf);
        Identifier blockId = Identifier.PACKET_CODEC.decode(buf);
        TagKey<Block> tag = TagKey.of(RegistryKeys.BLOCK, tagId);
        Block block = Registries.BLOCK.get(blockId);
        return new BurnRecipe(tag, block);
    }

    public static void write(RegistryByteBuf buf, BurnRecipe recipe) {
        Identifier.PACKET_CODEC.encode(buf, recipe.input().id());
        Identifier.PACKET_CODEC.encode(buf, Registries.BLOCK.getId(recipe.result()));
    }

    @Override
    public MapCodec<BurnRecipe> codec() {
        return CODEC;
    }

    @Override
    public PacketCodec<RegistryByteBuf, BurnRecipe> packetCodec() {
        return PACKET_CODEC;
    }
}
