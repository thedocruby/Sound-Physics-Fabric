package dev.thedocruby.resounding.toolbox;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.shape.VoxelShape;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public interface ChunkChain {
	// TODO determine 16/24/48?
	public static Map<Long, VoxelShape> shapes = new ConcurrentHashMap<>(48);
	public @NotNull Branch[] branches = new Branch[0];
	public int yOffset = 1; // 1.18 (0- -16(y) >> 4)
	public ChunkChain[] xPlane = null;
	public ChunkChain[] zPlane = null;
	public ChunkChain[][] planes = {xPlane, zPlane};

	public ChunkChain set(int plane, ChunkChain negative, ChunkChain positive);

	public ChunkChain set(int plane, int index, ChunkChain link);

	public ChunkChain get(int plane, int index);

	// one-dimensional traversal
	public ChunkChain traverse(int d, int plane);

	public ChunkChain access_(int tx, int tz);

	public ChunkChain access(int x, int z);
}
