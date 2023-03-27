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
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class Collision extends HitResult {
	private final Direction side;
	private final BlockPos blockPos;
	private final boolean missed;
	private final BlockState blockState;
	public final WorldChunk chunk;

	// TODO: determine if this is still needed
	public final Vec3d split;

	@Contract("_, _, _, _ -> new")
	public static @NotNull Collision miss(Vec3d pos, Direction side, BlockPos blockPos, WorldChunk c) {
		return new Collision(true, pos, side, blockPos, null, c);
	}


	public Collision(@NotNull BlockHitResult blockHitResult, BlockState bs, WorldChunk c) {
		super(blockHitResult.getPos());
		this.missed = false;//blockHitResult.getType() == Type.MISS;
		this.side = blockHitResult.getSide();
		this.blockPos = blockHitResult.getBlockPos();
		this.blockState = bs;
		this.chunk = c;
		this.split = null;
	}

	public Collision(boolean missed, Vec3d pos, Direction side, BlockPos blockPos, BlockState bs, WorldChunk c) {
		super(pos);
		this.missed = missed;
		this.side = side;
		this.blockPos = blockPos;
		this.blockState = bs;
		this.chunk = c;
		this.split = null;
	}

	public Collision(Vec3d split, Vec3d pos, Direction side, BlockPos blockPos, BlockState bs, WorldChunk c) {
		super(pos);
		this.missed = false;
		this.side = side;
		this.blockPos = blockPos;
		this.blockState = bs;
		this.chunk = c;
		this.split = null;
	}

	public static Collision hit(BlockHitResult bhr, BlockState bs, WorldChunk c){
		if (bhr == null) return null;
		return new Collision(bhr, bs, c);
	}

	public BlockPos getBlockPos() {return this.blockPos;}
	public Direction getSide() {return this.side;}
	@Deprecated
	public Type getType() {return this.missed ? Type.MISS : Type.BLOCK;}
	public boolean isMissed() {return this.missed;}
	public BlockState getBlockState() {return blockState;}
	public @Nullable Vec3d getSplit() {return this.split;}
}
