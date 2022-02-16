package dev.thedocruby.resounding.raycast;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class SPHitResult extends HitResult {
    private final Direction side;
    private final BlockPos blockPos;
    private final boolean missed;
    private final BlockState blockState;
    public final WorldChunk chunk;

    @Contract("_, _, _, _ -> new")
    public static @NotNull SPHitResult createMissed(Vec3d pos, Direction side, BlockPos blockPos, WorldChunk c) {
        return new SPHitResult(true, pos, side, blockPos, null, c);
    }

    public SPHitResult(@NotNull BlockHitResult blockHitResult, BlockState bs, WorldChunk c) {
        super(blockHitResult.getPos());
        this.missed = false;//blockHitResult.getType() == Type.MISS;
        this.side = blockHitResult.getSide();
        this.blockPos = blockHitResult.getBlockPos();
        this.blockState = bs;
        this.chunk = c;
    }

    public SPHitResult(boolean missed, Vec3d pos, Direction side, BlockPos blockPos, BlockState bs, WorldChunk c) {
        super(pos);
        this.missed = missed;
        this.side = side;
        this.blockPos = blockPos;
        this.blockState = bs;
        this.chunk = c;
    }
    public static SPHitResult get(BlockHitResult bhr, BlockState bs, WorldChunk c){
        if (bhr == null) return null;
        return new SPHitResult(bhr, bs, c);
    }

    public BlockPos getBlockPos() {return this.blockPos;}
    public Direction getSide() {return this.side;}
    @Deprecated
    public Type getType() {return this.missed ? Type.MISS : Type.BLOCK;}
    public boolean isMissed() {return this.missed;}
    public BlockState getBlockState() {return blockState;}
}