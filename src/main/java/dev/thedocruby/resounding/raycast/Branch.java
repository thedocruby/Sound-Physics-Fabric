package dev.thedocruby.resounding.raycast;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static dev.thedocruby.resounding.raycast.Cache.blockMap;

@Environment(EnvType.CLIENT)
public class Branch {
    BlockPos start;
    BlockPos size;
    @Nullable BlockState state;

    HashMap<BlockPos, Branch> leaves = new HashMap<>(8);

    public boolean parent = false;

    public Branch(BlockPos start, BlockPos size, @Nullable BlockState state) {
        this.start = start;
        this.size = size;
        this.state = state;
    }

    public Branch set(@Nullable BlockState state) {
        this.state = state;
        return this;
    }

    public Branch set(@Nullable BlockPos size) {
        this.size = size;
        return this;
    }

    public @NotNull Branch get(BlockPos pos) {
        return this.get(pos, 3);
    }

    // recursively search tree for corresponding branch
    // positions are normalized by section (16³)
    public @NotNull Branch get(BlockPos pos, int n) {
        // if branch isn't subdivided, return self
        if (!this.parent) return this;
        // round position for node
        BlockPos octo = shift(pos, n);
        @Nullable Branch leaf = leaves.get(octo);
        return leaf == null ? this : leaf.get(pos, n-1);
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

    public Branch put(BlockPos pos, Branch branch) {
        this.parent = true;
        return leaves.put(pos, branch);
    }

    public Branch remove(BlockPos pos) {
        this.parent = !leaves.isEmpty();
        return leaves.remove(pos);
    }

    public static Pair<Double,Double> blockAttributes(BlockState state) {
        @Nullable Pair<Double,Double> attributes = blockMap.get(state.getBlock());
        return attributes == null ? new Pair<>(0.0,0.0) : attributes;
    }
}
