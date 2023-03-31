package dev.thedocruby.resounding.raycast;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

import static dev.thedocruby.resounding.Cache.blockMap;

@Environment(EnvType.CLIENT)
public class Branch {
    BlockPos start;
    int size;
    @Nullable BlockState state;

    HashMap<Long, Branch> leaves = new HashMap<>(8);

    public Branch(BlockPos start, int size, @Nullable BlockState state) {
        this.start = start;
        this.size = size;
        this.state = state;
        if (this.size == 2) leaves = null;
    }

    public Branch set(@Nullable BlockState state) {
        this.state = state;
        return this;
    }

    public Branch set(int size) {
        this.size = size;
        return this;
    }

    public @NotNull Branch get(BlockPos pos) { return this.get(pos, 3); }

    // recursively search tree for corresponding branch
    // positions are normalized by section (16³)
    public @NotNull Branch get(BlockPos pos, int layer) {
        // if branch isn't subdivided, return self
        if (leaves.isEmpty()) return this;
        @Nullable Branch leaf = leaves.get(
                // round position for node
                shift(pos, layer).asLong());
        return leaf == null ? this : leaf.get(pos, layer-1);
    }

    private static BlockPos shift(BlockPos pos, int n) {
        return new BlockPos(pos.getX() >> n, pos.getY() >> n, pos.getZ() >> n);
    }

    private static Vec3d shift(Vec3d pos, int n) {
        return new Vec3d((int) pos.x >> n, (int) pos.y >> n, (int) pos.z >> n);
    }

//    private static BlockPos sub(BlockPos pos, BlockPos octo, int n) {
//        return pos.subtract(octo.multiply(n));
//    }

    // this should only be used
    @Deprecated // NOT REALLY, but @Unsafe isn't available... :/
    public Branch put(Long pos, Branch branch) {
        return leaves.put(pos, branch);
    }

    public Branch empty() {
        leaves = new HashMap<>(8);
        return this;
    }

    public Branch replace(Long pos, Branch branch) {
        return leaves.replace(pos, branch);
    }

    public static Pair<Double,Double> blockAttributes(BlockState state) {
        @Nullable Pair<Double,Double> attributes = blockMap.get(state.getBlock());
        return attributes == null ? new Pair<>(0.0,0.0) : attributes;
    }

    public void setBlock(int x, int y, int z, BlockState state, boolean moved) {
    }
}
