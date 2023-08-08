package dev.thedocruby.resounding.raycast;

import dev.thedocruby.resounding.Cache;
import dev.thedocruby.resounding.Material;
import dev.thedocruby.resounding.toolbox.MaterialData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

import static dev.thedocruby.resounding.Cache.blockMap;

@Environment(EnvType.CLIENT)
public class Branch {
    public BlockPos start;
    public int size;
    public @NotNull VoxelShape shape = Cache.CUBE;
    public @Nullable Material material; // TODO: use!

    public @NotNull HashMap<Long, Branch> leaves;


    public Branch(BlockPos start, int size) {
        this.leaves = new HashMap<>(size < 4 ? 0 : 8, 2 /* should never be reached */);
        this.start = start;
        this.size = size;
    }

    public Branch(BlockPos start, int size, @NotNull VoxelShape shape) {
        this(start, size);
        set(shape);
    }

    public Branch(BlockPos start, int size, @Nullable Material material) {
        this(start, size);
        set(material);
    }

    public Branch(BlockPos start, int size, @Nullable VoxelShape shape, @Nullable Material material) {
        this(start, size);
        set(shape);
        set(material);
    }

    public Branch set(@Nullable VoxelShape shape) {
        this.shape = shape;
        return this;
    }

    public Branch set(@Nullable Material material) {
        this.material = material;
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
    public Branch put(Long pos, Branch branch) { return leaves.put(pos, branch); }

    public Branch empty() {
        leaves.clear();
        return this;
    }

    public boolean isEmpty() {
        return leaves.isEmpty();
    }

    public Branch replace(Long pos, Branch branch) { return leaves.replace(pos, branch); }

    public static Pair<Double,Double> blockAttributes(BlockState state) {
        @Nullable Pair<Double,Double> attributes = blockMap.get(state.getBlock());
        return attributes == null ? new Pair<>(0.0,0.0) : attributes;
    }
}
