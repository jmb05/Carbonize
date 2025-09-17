package net.jmb19905.util;

import net.minecraft.util.math.BlockPos;

public class BlockPosWrapper {
    private final BlockPos pos;

    public BlockPosWrapper(BlockPos pos) {
        this.pos = pos;
    }

    public int[] toArray() {
        return reduce(pos);
    }

    public BlockPos expose() {
        return pos;
    }

    @Override
    public boolean equals(Object obj) {
        var bool = false;
        if (obj instanceof BlockPosWrapper wrapper) obj = wrapper.pos;
        if (obj instanceof BlockPos objPos)
            bool = pos.getX() == objPos.getX() && pos.getY() == objPos.getY() && pos.getZ() == objPos.getZ();
        return bool;
    }

    public static int[] reduce(BlockPos pos) {
        return new int[] {pos.getX(), pos.getY(), pos.getZ()};
    }

    public static BlockPos raise(int[] pos) {
        return new BlockPos(pos[0], pos[1], pos[2]);
    }
}
