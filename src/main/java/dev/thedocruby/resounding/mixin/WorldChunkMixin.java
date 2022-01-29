package dev.thedocruby.resounding.mixin;

import dev.thedocruby.resounding.performance.LiquidStorage;
import dev.thedocruby.resounding.performance.WorldChunkAccess;
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

    public WorldChunkMixin(ChunkPos pos, UpgradeData upgradeData, HeightLimitView heightLimitView, Registry<Biome> biome, long inhabitedTime, @Nullable ChunkSection[] sectionArrayInitializer, @Nullable BlendingData blendingData) {
        super(pos, upgradeData, heightLimitView, biome, inhabitedTime, sectionArrayInitializer, blendingData);
    }

    @Inject(method = "loadFromPacket(Lnet/minecraft/network/PacketByteBuf;Lnet/minecraft/nbt/NbtCompound;Ljava/util/function/Consumer;)V", at = @At("RETURN"))
    private void load(PacketByteBuf buf, NbtCompound nbt, Consumer<ChunkData.BlockEntityVisitor> consumer, CallbackInfo ci){initStorage();}

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/chunk/UpgradeData;Lnet/minecraft/world/tick/ChunkTickScheduler;Lnet/minecraft/world/tick/ChunkTickScheduler;J[Lnet/minecraft/world/chunk/ChunkSection;Lnet/minecraft/world/chunk/WorldChunk$EntityLoader;Lnet/minecraft/world/gen/chunk/BlendingData;)V", at = @At("RETURN"))
    private void create(World world, ChunkPos pos, UpgradeData upgradeData, ChunkTickScheduler<?> blockTickScheduler, ChunkTickScheduler<?> fluidTickScheduler, long inhabitedTime, ChunkSection[] sectionArrayInitializer, WorldChunk.EntityLoader entityLoader, BlendingData blendingData, CallbackInfo ci){if (sectionArrayInitializer != null) initStorage();}

    private void initStorage() {
        if (world == null || !world.isClient) return;
        ChunkSection[] chunkSections = getSectionArray();
        boolean[][] notAirSections = new boolean[512][]; AtomicInteger bottomNotAir = new AtomicInteger(-600); AtomicInteger topNotAir = new AtomicInteger(-600); boolean[] notAirFull = new boolean[512];

        //for (ChunkSection chunkSection: chunkSections) { //horrible performance code
        Stream.of(chunkSections).parallel().forEach((chunkSection) -> { // surprisingly good-performance code (actually 0 impact)
            if (chunkSection.isEmpty()) return;
            for (int y = chunkSection.getYOffset(), l = y+16; y<l; y++){
                boolean[] notAirSlice = LiquidStorage.empty(); int notAirCount = 0;
                for (int x = 0; x < 16; x++) {

                    for (int z = 0; z < 16; z++) {
                        Block block = chunkSection.getBlockState(x, y & 15, z).getBlock();
                        if (!LiquidStorage.LIQUIDS.AIR.matches(block)) { notAirSlice[x+(z<<4)]=true; notAirCount++; }
                    }
                }
                if (notAirCount!=0){
                    synchronized (notAirSections) {notAirSections[y+64] = notAirSlice;}
                    int Y = y;
                    bottomNotAir.getAndUpdate((v) -> v == -600 ? Y : Math.min(v, Y));
                    topNotAir.getAndUpdate((v) -> v == -600 ? Y : Math.max(v, Y));
                    synchronized (notAirFull) {notAirFull[y+64] = (notAirCount == 16*16);}
                }
            }
        });

        if (topNotAir.get() != -600) notAirLiquidStorage = new LiquidStorage(ArrayUtils.subarray(notAirSections, bottomNotAir.get() +64, topNotAir.get() +64+1), topNotAir.get(), bottomNotAir.get(), ArrayUtils.subarray(notAirFull, bottomNotAir.get() +64, topNotAir.get() +64+1), (WorldChunk) (Object) this);
        else notAirLiquidStorage = new LiquidStorage((WorldChunk) (Object) this);
        WorldChunkAccess[] adj = new WorldChunkAccess[4];
        for (int i = 0; i <= 3; i++) {
            adj[i] = (WorldChunkAccess) world.getChunk(super.pos.x + (i==0?-1:i==1?1:0), super.pos.z + (i==2?-1:i==3?1:0), ChunkStatus.FULL, false);
        }
        if (adj[0] != null) {adj[0].getNotAirLiquidStorage().xm = (WorldChunk) (Object) this; notAirLiquidStorage.xp = (WorldChunk) adj[0];}
        if (adj[1] != null) {adj[1].getNotAirLiquidStorage().xp = (WorldChunk) (Object) this; notAirLiquidStorage.xm = (WorldChunk) adj[1];}
        if (adj[2] != null) {adj[2].getNotAirLiquidStorage().zm = (WorldChunk) (Object) this; notAirLiquidStorage.zp = (WorldChunk) adj[2];}
        if (adj[3] != null) {adj[3].getNotAirLiquidStorage().zp = (WorldChunk) (Object) this; notAirLiquidStorage.zm = (WorldChunk) adj[3];}
    }

    @Shadow public ChunkStatus getStatus() {return null;}

    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;", at = @At("HEAD"))
    private void setBlock(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir){
        if (!world.isClient) return;
        Block block = state.getBlock();
        notAirLiquidStorage.setBlock(pos.getX() & 15, pos.getY(), pos.getZ() & 15, !LiquidStorage.LIQUIDS.AIR.matches(block));
    }

    @Mixin(ClientChunkManager.class)
    public abstract static class Unloaded {
        @SuppressWarnings("SameReturnValue") @Shadow public WorldChunk getChunk(int x, int z, ChunkStatus chunkStatus, boolean bl){return null;}
        @Inject(method = "unload(II)V", at = @At("HEAD"))
        public void unload(int chunkX, int chunkZ, CallbackInfo ci) {

            WorldChunkAccess[] adj = new WorldChunkAccess[4];
            for (int i = 0; i <= 3; i++) {
                adj[i] = (WorldChunkAccess) getChunk(chunkX + (i==0?-1:i==1?1:0), chunkZ + (i==2?-1:i==3?1:0), ChunkStatus.FULL, false);
            }
            if (adj[0] != null) adj[0].getNotAirLiquidStorage().xm = null;
            if (adj[1] != null) adj[1].getNotAirLiquidStorage().xp = null;
            if (adj[2] != null) adj[2].getNotAirLiquidStorage().zm = null;
            if (adj[3] != null) adj[3].getNotAirLiquidStorage().zp = null;
        }
    }
}

