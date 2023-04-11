package dev.thedocruby.resounding.mixin;

import dev.thedocruby.resounding.raycast.Branch;
import dev.thedocruby.resounding.toolbox.ChunkChain;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
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
	@Override
	public Branch getBranch(int y) { return ArrayUtils.get(branches,(y>>4)+this.yOffset, null); }
	public ChunkChain[] xPlane = {null, this, null};
	public ChunkChain[] zPlane = {null, this, null};
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
		int px = pos.x;
		int pz = pos.z;
		return access_(x-px, z-pz); // TODO - validate
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

		// 16³ blocks
		ChunkSection[] chunkSections = getSectionArray();
		// LOGGER.info(String.valueOf(chunkSections.length) /* +"\t"+Arrays.toString(chunkSections)*/); // TODO remove
		this.yOffset = -chunkSections[0].getYOffset() >> 4;
		final ChunkPos pos = this.getPos();
		final Branch[] branches = new Branch[chunkSections.length];
		
		// chunk up a section into an octree
		Stream.of(chunkSections).parallel().forEach((chunkSection) -> {
			int y = chunkSection.getYOffset();
			Branch branch = layer(new Branch(new BlockPos(pos.x,y,pos.z),16,null));
			synchronized (branches) {
				branches[(y>>4)+this.yOffset] = branch;
			}
		});

		this.branches = branches;

		/* TODO remove entire block: /*
		String[] states = new String[branches.length];
		for (int i = 0; i < branches.length; i++) {
			final String state = branches[i].state == null ? "null" : branches[i].state.toString();
			states[i] = state + "\t" + branches[i].start.getY();
		}
		LOGGER.info(Arrays.toString(states));
		LOGGER.info(Arrays.toString(branches));
		// TODO remove */

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


	// upon setting block in client, update our copy {
	@Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;", at = @At("HEAD"))
	private void setBlock(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir){
		if (!world.isClient) return;
		// TODO
		// this.branches[pos.getY()<<4].setBlock(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, state, moved);
		this.shapes.clear();
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

	private BlockPos base = new BlockPos(0, 0, 0);

	private BlockPos[] sequence2 = {
			new BlockPos(1, 0, 0),
			new BlockPos(0, 1, 0),
			new BlockPos(1, 1, 0),
			new BlockPos(0, 0, 1),
			new BlockPos(1, 0, 1),
			new BlockPos(0, 1, 1),
			new BlockPos(1, 1, 1)
	};

	private BlockPos[] sequence = ArrayUtils.addAll(new BlockPos[] {base}, sequence2);




	// TODO integrate into initStorage() and onUpdate/setBlock
	public Branch layer(Branch root) {
		return root; // TODO remove / fix
		/*
		// determine scale to play with
		final int scale = root.size >> 1;
		final BlockPos start = root.start;
		// get first state at root position
		BlockState state = this.getBlockState(start);
		Pair<Double,Double> material = Cache.blockMap.get(state.getBlock());
		if (material == null) material = new Pair<>(0.1,0.7);
		boolean valid = true;
		boolean any   = false;
		if (scale != 1) {
			for (BlockPos block : sequence) {
				final BlockPos position = start.add(block.multiply(scale));
				// use recursion here
				Branch leaf = layer(new Branch(position,scale,null));
				if (leaf.state != null) {
					if (!leaf.isEmpty()) any = true;
					// TODO clean up
					Pair<Double,Double> next;// = Cache.blockMap.get(leaf.state.getBlock());
					final double reflect =   pC.reflMap.get(state.getBlock().getTranslationKey());
					final double perm    = 1-pC.absMap.get(state.getBlock().getTranslationKey());
					next = new Pair<>(reflect,perm);
					//if (next == null) next = new Pair<>(0.1,0.7); // TODO replace default
					if (Math.abs(material.getLeft () - next.getLeft ()) > .1
					&&  Math.abs(material.getRight() - next.getRight()) > .1) {
						valid = false;
						// don't break here, as understanding adjacent sections is important
					}
				}
				root.put(start.asLong(),leaf);
			}
			if (!any) {
				root.empty();
			}
		// for single-blocks
		} else {
			for (BlockPos block : sequence) {
				final BlockPos position = start.add(block.multiply(scale));
				state = this.getBlockState(position);
				// TODO clean up
				Pair<Double,Double> next;// = Cache.blockMap.get(state.getBlock());
				final double reflect =   pC.reflMap.get(state.getBlock().getTranslationKey());
				final double perm    = 1-pC.absMap.get(state.getBlock().getTranslationKey());
				next = new Pair<>(reflect,perm);
				//if (next == null) next = new Pair<>(0.1,0.7); // TODO replace default
				// break if next block isn't similar enough
				if (Math.abs(material.getLeft () - next.getLeft ()) > .1
				&&  Math.abs(material.getRight() - next.getRight()) > .1) {
					valid = false;
					break;
				}
			}
		}
		if (valid) {
			root.set(state);
		}
		return root;

	//*/
	}
}

