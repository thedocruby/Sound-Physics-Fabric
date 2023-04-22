package dev.thedocruby.resounding.toolbox;

import dev.thedocruby.resounding.raycast.Branch;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Environment(EnvType.CLIENT)
public interface ChunkChain {
	Branch getBranch(int y);
	@NotNull Map<Long, VoxelShape> getShapes();
	//*/
	public String yOffset = ""; // 1.18 (0- -16(y) >> 4)

	public ChunkChain set(int plane, ChunkChain negative, ChunkChain positive);

	public ChunkChain set(int plane, int index, ChunkChain link);

	public ChunkChain get(int plane, int index);

	// one-dimensional traversal
	public ChunkChain traverse(int d, int plane);

	public ChunkChain access_(int tx, int tz);

	public ChunkChain access(int x, int z);

    void initStorage();

	public Branch layer(Branch root);
}
