package dev.thedocruby.resounding.raycast;

import dev.thedocruby.resounding.toolbox.Shapes;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Cache {

    public static Map<BlockPos, Branch> overlay = new ConcurrentHashMap<BlockPos,Branch>();
    // public static long lastUpd = 0;
    public static Map<Long, Shapes> shape = new ConcurrentHashMap<>(2048);
    // {pos, (block state, block, fluid) }

    // do these really belong here?
    public final static VoxelShape EMPTY = VoxelShapes.empty();
    public final static VoxelShape CUBE = VoxelShapes.fullCube();

    public final static Map<Block, Pair<Double,Double>> blockMap = new HashMap<Block,Pair<Double,Double>>() {{
        put(null        , pair(1.0, 0.0));
        put(Blocks.STONE, pair(0.9, 0.4));
        put(Blocks.AIR  , pair(0.0, 0.1));
    }};

    // coefficient for reflection & coefficient for block permeability (inverse of absorption)
    private static Pair<Double,Double> pair(Double ref, Double perm) {
        return new Pair<>(ref, perm);
    }
}
