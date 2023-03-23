package dev.thedocruby.resounding.mixin;

import dev.thedocruby.resounding.raycast.LiquidStorage;
import dev.thedocruby.resounding.toolbox.WorldChunkAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.*;
import net.minecraft.world.gen.chunk.BlendingData;
import net.minecraft.world.tick.ChunkTickScheduler;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin extends Chunk implements WorldChunkAccess {
	private LiquidStorage notAirLiquidStorage = null;
	//private LiquidStorage waterLiquidStorage = null;//todo

	public LiquidStorage getNotAirLiquidStorage() {return notAirLiquidStorage;}
	//public LiquidStorage getWaterLiquidStorage() {return waterLiquidStorage;}

	@Shadow @Final World world;

	// pass along to super {
	public WorldChunkMixin(ChunkPos pos, UpgradeData upgradeData, HeightLimitView heightLimitView, Registry<Biome> biome, long inhabitedTime, @Nullable ChunkSection[] sectionArrayInitializer, @Nullable BlendingData blendingData) {
		super(pos, upgradeData, heightLimitView, biome, inhabitedTime, sectionArrayInitializer, blendingData);
	}
	// }

	// TODO: inspect
	// upon receiving a packet, initialize storage {
	@Inject(method = "loadFromPacket(Lnet/minecraft/network/PacketByteBuf;Lnet/minecraft/nbt/NbtCompound;Ljava/util/function/Consumer;)V", at = @At("RETURN"))
	private void load(PacketByteBuf buf, NbtCompound nbt, Consumer<ChunkData.BlockEntityVisitor> consumer, CallbackInfo ci){initStorage();}
	// }

	// upon creation of world, initialize storage {
	@Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/chunk/UpgradeData;Lnet/minecraft/world/tick/ChunkTickScheduler;Lnet/minecraft/world/tick/ChunkTickScheduler;J[Lnet/minecraft/world/chunk/ChunkSection;Lnet/minecraft/world/chunk/WorldChunk$EntityLoader;Lnet/minecraft/world/gen/chunk/BlendingData;)V", at = @At("RETURN"))
	private void create(World world, ChunkPos pos, UpgradeData upgradeData, ChunkTickScheduler<?> blockTickScheduler, ChunkTickScheduler<?> fluidTickScheduler, long inhabitedTime, ChunkSection[] sectionArrayInitializer, WorldChunk.EntityLoader entityLoader, BlendingData blendingData, CallbackInfo ci) {
		if (sectionArrayInitializer != null) initStorage();
	}
	// }

	private void initStorage() {
		if (world == null || !world.isClient) return;
		// 16³ blocks
		ChunkSection[] chunkSections = getSectionArray();
		// sections containing blocks
		boolean[][] activeSections = new boolean[512][];
		AtomicInteger bottomNotAir = new AtomicInteger(-600);
		AtomicInteger topNotAir = new AtomicInteger(-600);
		// sections regarded as solid
		boolean[] solidSections = new boolean[512];

		// thread chunking to keep loading quick
		Stream.of(chunkSections).parallel().forEach((chunkSection) -> {
			if (chunkSection.isEmpty()) return;
			for (int y = chunkSection.getYOffset(), l = y+16; y<l; y++) {
				// flat 16² block layer
				boolean[] slice = LiquidStorage.empty();
				int count = 0;
				for (int x = 0; x < 16; x++) {
					for (int z = 0; z < 16; z++) {
						Block block = chunkSection.getBlockState(x, y & 15, z).getBlock();
						if (!LiquidStorage.LIQUIDS.AIR.matches(block)) { slice[x+(z<<4)]=true; count++; }
					}
				}
				if (count!=0){
					synchronized (activeSections) {activeSections[y+64] = slice;}
					int Y = y;
					bottomNotAir.getAndUpdate((v) -> v == -600 ? Y : Math.min(v, Y));
					topNotAir.getAndUpdate((v) -> v == -600 ? Y : Math.max(v, Y));
					synchronized (solidSections) {solidSections[y+64] = (count == 16*16);}
				}
			}
		});

		if (topNotAir.get() != -600)
			notAirLiquidStorage = new LiquidStorage(
				ArrayUtils.subarray(activeSections, bottomNotAir.get() +64, topNotAir.get() +64+1),
				topNotAir.get(), bottomNotAir.get(),
				ArrayUtils.subarray(solidSections, bottomNotAir.get() +64, topNotAir.get() +64+1),
				(WorldChunk) (Object) this
			);
		else notAirLiquidStorage = new LiquidStorage((WorldChunk) (Object) this);
		WorldChunkAccess[] adj = new WorldChunkAccess[4];
		adj[0] = (WorldChunkAccess) world.getChunk(super.pos.x - 1, super.pos.z + 0, ChunkStatus.FULL, false);
		adj[1] = (WorldChunkAccess) world.getChunk(super.pos.x + 1, super.pos.z + 0, ChunkStatus.FULL, false);
		adj[2] = (WorldChunkAccess) world.getChunk(super.pos.x + 0, super.pos.z - 1, ChunkStatus.FULL, false);
		adj[3] = (WorldChunkAccess) world.getChunk(super.pos.x + 0, super.pos.z + 1, ChunkStatus.FULL, false);
		if (adj[0] != null) {adj[0].getNotAirLiquidStorage().xm = (WorldChunk) (Object) this; notAirLiquidStorage.xp = (WorldChunk) adj[0];}
		if (adj[1] != null) {adj[1].getNotAirLiquidStorage().xp = (WorldChunk) (Object) this; notAirLiquidStorage.xm = (WorldChunk) adj[1];}
		if (adj[2] != null) {adj[2].getNotAirLiquidStorage().zm = (WorldChunk) (Object) this; notAirLiquidStorage.zp = (WorldChunk) adj[2];}
		if (adj[3] != null) {adj[3].getNotAirLiquidStorage().zp = (WorldChunk) (Object) this; notAirLiquidStorage.zm = (WorldChunk) adj[3];}
	}

	@Shadow public ChunkStatus getStatus() {return null;}

	// upon setting block in client, update our copy {
	@Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;", at = @At("HEAD"))
	private void setBlock(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir){
		if (!world.isClient) return;
		Block block = state.getBlock();
		// TODO: make LiquidStorage obsolete
		notAirLiquidStorage.setBlock(pos.getX() & 15, pos.getY(), pos.getZ() & 15, !LiquidStorage.LIQUIDS.AIR.matches(block));
		// tree.update(block);
	}
	// }

	@Mixin(ClientChunkManager.class)
	public abstract static class Unloaded {
		@Shadow @SuppressWarnings("SameReturnValue")
		public WorldChunk getChunk(int x, int z, ChunkStatus chunkStatus, boolean bl){
			return null;
		}
		@Inject(method = "unload(II)V", at = @At("HEAD"))
		public void unload(int x, int z, CallbackInfo ci) {
			WorldChunkAccess[] adj = new WorldChunkAccess[4];
			adj[0] = (WorldChunkAccess) getChunk(x - 1, z + 0, ChunkStatus.FULL, false);
			adj[1] = (WorldChunkAccess) getChunk(x + 1, z + 0, ChunkStatus.FULL, false);
			adj[2] = (WorldChunkAccess) getChunk(x + 0, z - 1, ChunkStatus.FULL, false);
			adj[3] = (WorldChunkAccess) getChunk(x + 0, z + 1, ChunkStatus.FULL, false);
			if (adj[0] != null) adj[0].getNotAirLiquidStorage().xm = null;
			if (adj[1] != null) adj[1].getNotAirLiquidStorage().xp = null;
			if (adj[2] != null) adj[2].getNotAirLiquidStorage().zm = null;
			if (adj[3] != null) adj[3].getNotAirLiquidStorage().zp = null;
		}
	}
}

