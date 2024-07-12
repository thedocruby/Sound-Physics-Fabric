package dev.thedocruby.resounding.mixin;

import dev.thedocruby.resounding.Cache;
import dev.thedocruby.resounding.Material;
import dev.thedocruby.resounding.raycast.Branch;
import dev.thedocruby.resounding.toolbox.ChunkChain;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.*;
import net.minecraft.world.gen.chunk.BlendingData;
import net.minecraft.world.tick.ChunkTickScheduler;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static dev.thedocruby.resounding.Cache.material;
import static dev.thedocruby.resounding.Engine.hasLoaded;

@Environment(EnvType.CLIENT)
@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin extends Chunk implements ChunkChain {
	@Shadow @Final
	World world;

	@Shadow
	public abstract ChunkStatus getStatus();

	@Shadow @Final
	static Logger LOGGER;

	@Shadow public abstract BlockState getBlockState(BlockPos pos);

	public int yOffset = 1; // 1.18 (0- -16(y) >> 4)

	public Map<Long, VoxelShape> shapes = new ConcurrentHashMap<>(48);

	public @NotNull Map<Long, VoxelShape> getShapes() { return shapes; }

	public @NotNull Branch[] branches = new Branch[0];

	public boolean loaded = true;

	@Override
	public Branch getBranch(int y) { return ArrayUtils.get(branches,y+this.yOffset, null); }

	public ChunkChain[]   xPlane = {null, this, null};
	public ChunkChain[]   zPlane = {null, this, null};
	public ChunkChain[][] planes = {xPlane, zPlane};

	public ChunkChain set(int plane, ChunkChain negative, ChunkChain positive) {
		this.planes[plane][0] = negative;
		this.planes[plane][2] = positive;
		return this;
	}

	public ChunkChain set(int plane, int index, ChunkChain link) {
		this.planes[plane][1+index] = link;
		return this;
	}

	public ChunkChain get(int plane, int index) { return this.planes[plane][1+index]; }

	// one-dimensional traversal
	public ChunkChain traverse(int d, int plane) {
		int dx = (int) Math.signum(d);
		if (dx == 0) return this;
		@Nullable ChunkChain next = this.planes[plane][1+dx];
		return next == null ? null
				: next.traverse(d-dx, plane);
	}

	public ChunkChain access_(int tx, int tz) {
		ChunkChain next = traverse(tx, 0);
		return next == null ? null
				: next.traverse(tz, 1);
	}

	public ChunkChain access(int x, int z) {
		ChunkPos pos = this.getPos();
		return access_(x-pos.x, z-pos.z); // TODO - validate
	}

	// pass along to super {
	public WorldChunkMixin(ChunkPos pos, UpgradeData upgradeData, HeightLimitView heightLimitView, Registry<Biome> biome, long inhabitedTime, @Nullable ChunkSection[] sectionArrayInitializer, @Nullable BlendingData blendingData) {
		super(pos, upgradeData, heightLimitView, biome, inhabitedTime, sectionArrayInitializer, blendingData);
		// LOGGER.info("I like 'em chunky");  // TODO remove
		// if (sectionArrayInitializer != null) this.initStorage(); // TODO ^
	}

	// }
	// upon receiving a packet, initialize storage {
	@Inject(method = "loadFromPacket(Lnet/minecraft/network/PacketByteBuf;Lnet/minecraft/nbt/NbtCompound;Ljava/util/function/Consumer;)V", at = @At("RETURN"))
	private void load(PacketByteBuf buf, NbtCompound nbt, Consumer<ChunkData.BlockEntityVisitor> consumer, CallbackInfo ci){ initStorage(); }
	// }

	// upon creation of world, initialize storage {
	@Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/chunk/UpgradeData;Lnet/minecraft/world/tick/ChunkTickScheduler;Lnet/minecraft/world/tick/ChunkTickScheduler;J[Lnet/minecraft/world/chunk/ChunkSection;Lnet/minecraft/world/chunk/WorldChunk$EntityLoader;Lnet/minecraft/world/gen/chunk/BlendingData;)V", at = @At("RETURN"))
	private void create(World world, ChunkPos pos, UpgradeData upgradeData, ChunkTickScheduler<?> blockTickScheduler, ChunkTickScheduler<?> fluidTickScheduler, long inhabitedTime, ChunkSection[] sectionArrayInitializer, WorldChunk.EntityLoader entityLoader, BlendingData blendingData, CallbackInfo ci) {
		if (sectionArrayInitializer != null) initStorage();
	}
	// }

	// for readability
	private ChunkChain take(int x, int z) {
		return (ChunkChain) world.getChunk(super.pos.x + x,super.pos.z + z,ChunkStatus.FULL,false);
	}

	public void initStorage() {
		if (world == null || !world.isClient) return;
		//* TODO remove
		if (!hasLoaded) {
			hasLoaded = Cache.generate();
//			return;
		}
		// */

		// 16³ blocks
		ChunkSection[] chunkSections = getSectionArray();
		// LOGGER.info(String.valueOf(chunkSections.length) /* +"\t"+Arrays.toString(chunkSections)*/); // TODO remove
		this.yOffset = -chunkSections[0].getYOffset() >> 4;
		final ChunkPos pos = this.getPos();
		final double x     = pos.x << 4; // * 16
		final double z     = pos.z << 4; // * 16
		this.branches = new Branch[chunkSections.length];
		final Branch[] branches = new Branch[chunkSections.length];

		// chunk up a section into an octree
		Stream.of(chunkSections).parallel().forEach((chunkSection) -> {
			int y = chunkSection.getYOffset();
			final int index = this.yOffset + (y >> 4);
			boolean empty = chunkSection.isEmpty();

			Branch air = new Branch(new BlockPos(x,y,z),16, material(Blocks.AIR.getDefaultState()));
			Branch blank = new Branch(new BlockPos(x,y,z),16);

			// provide fallback or all-air branch when necessary
			synchronized (branches) {
				branches[index] = empty ? air : blank;
			}
			// only calculate if necessary
			if (empty) {
				Cache.counter++;
				Cache.octreePool.execute(() -> Cache.plantOctree(this, index, blank));
			}
		});

		this.branches = branches;

		ChunkChain[] adj = new ChunkChain[4];
		// retrieve & save locally
		this.set(0,-1,adj[0] = take(-1, +0));
		this.set(0,+1,adj[1] = take(+1, +0));
		this.set(1,-1,adj[2] = take(+0, -1));
		this.set(1,+1,adj[3] = take(+0, +1));

		// update self-references using reversed direction
		if (adj[0] != null) adj[0].set(0,+1,this);
		if (adj[1] != null) adj[1].set(0,-1,this);
		if (adj[2] != null) adj[2].set(1,+1,this);
		if (adj[3] != null) adj[3].set(1,-1,this);
	}

	public void set(int index, Branch branch) {
		this.branches[index] = branch;
	}


	// upon setting block in client, update our copy {
	@Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;", at = @At("HEAD"))
	private void setBlock(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir) {
		if (!world.isClient) return;
		if (loaded)
			updateBlock(pos, state, moved);
	}
	// }

	@Mixin(ClientChunkManager.class)
	public abstract static class Unloaded {
		@Shadow @SuppressWarnings("SameReturnValue")
		public WorldChunk getChunk(int x, int z, ChunkStatus chunkStatus, boolean bl){
			return null;
		}

		// for readability
		private ChunkChain take(int x, int z) {
			return (ChunkChain) getChunk(x,z,ChunkStatus.FULL,false);
		}

		@Inject(method = "unload(II)V", at = @At("HEAD"))
		public void unload(int x, int z, CallbackInfo ci) {
			ChunkChain[] adj = new ChunkChain[4];
			adj[0] = take(x - 1, z + 0);
			adj[1] = take(x + 1, z + 0);
			adj[2] = take(x + 0, z - 1);
			adj[3] = take(x + 0, z + 1);

			// delete self-references & using reversed direction
			if (adj[0] != null) adj[0].set(0,+1,null);
			if (adj[1] != null) adj[1].set(0,-1,null);
			if (adj[2] != null) adj[2].set(1,+1,null);
			if (adj[3] != null) adj[3].set(1,-1,null);
		}
	}


	private void updateBlock(BlockPos pos, BlockState state, boolean moved) {
		// get smallest branch at position
		final Branch branch = this.getBranch(pos.getY() >> 4).get(pos);

		Material material = material(state);
		// if block is homogenous with branch
		//* TODO remove
		if (Objects.equals(material.equals, branch.material)) return;
		// */

		// will get optimized on reload, must keep this function quick
		branch.material = null;
		this.shapes.remove(pos.asLong());
	}
}

