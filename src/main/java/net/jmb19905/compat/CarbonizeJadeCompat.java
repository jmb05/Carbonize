package net.jmb19905.compat;

import net.jmb19905.Carbonize;
import net.jmb19905.block.CharringWoodBlock;
import net.jmb19905.blockEntity.CharringWoodBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public class CarbonizeJadeCompat implements IWailaPlugin, IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    public static final Identifier SHOW_SIZE = new Identifier(Carbonize.MOD_ID, "show_size");
    public static final Identifier SHOW_STAGE = new Identifier(Carbonize.MOD_ID, "show_stage");
    public static final Identifier SHOW_REMAINING_BURN_TIME = new Identifier(Carbonize.MOD_ID, "show_remaining_burn_time");
    private static final Identifier UID = new Identifier(Carbonize.MOD_ID, "plugin");

    @Override
    public void registerClient(IWailaClientRegistration register) {
        register.registerBlockComponent(this, Block.class);
        register.registerBlockIcon(this, CharringWoodBlock.class);
        register.addConfig(SHOW_SIZE, true);
        register.addConfig(SHOW_STAGE, true);
        register.addConfig(SHOW_REMAINING_BURN_TIME, true);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlock() instanceof CharringWoodBlock) {
            var data = accessor.getServerData();
            if (config.get(SHOW_STAGE))
                tooltip.add(Text.translatable("text.jade.carbonize.charring_wood.stage", data.getString("Stage")));
            if (config.get(SHOW_REMAINING_BURN_TIME))
                tooltip.add(Text.translatable("text.jade.carbonize.charring_wood.remaining_burn_time", data.getInt("RemainingBurnTime")));
        }
    }

    @Override
    public void register(IWailaCommonRegistration register) {
        register.registerBlockDataProvider(this, CharringWoodBlockEntity.class);
    }

    @Override
    public Identifier getUid() {
        return UID;
    }

    @Override
    public void appendServerData(NbtCompound nbtCompound, BlockAccessor accessor) {
        if (accessor.getBlock() instanceof CharringWoodBlock) {
            nbtCompound.putString("Stage", accessor.getBlockState().get(CharringWoodBlock.STAGE).name().toUpperCase());
            nbtCompound.putInt("RemainingBurnTime", ((CharringWoodBlockEntity) accessor.getBlockEntity()).getRemainingBurnTime());
        }
    }
}
