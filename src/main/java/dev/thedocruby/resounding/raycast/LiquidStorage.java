package dev.thedocruby.resounding.raycast;

import dev.thedocruby.resounding.Engine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/*
	Data structure to detect if a desired block is the medium for sound.

	Should attach to every chunk
 */

@Environment(EnvType.CLIENT)
public class LiquidStorage {
	private boolean full;
	public int bottom;
	public int top;
	private boolean[][] sections;
	private boolean[] sectionsFullMap;
	@Contract(value = " -> new", pure = true)
	public static boolean @NotNull [] empty() {return new boolean[16*16];}
	public final WorldChunk chunk;

	public WorldChunk xp = null;
	public WorldChunk xm = null;
	public WorldChunk zp = null;
	public WorldChunk zm = null;

	public enum LIQUIDS {
		//WATER(Set.of(Blocks.WATER, Blocks.BUBBLE_COLUMN)),
		//LAVA(Set.of(Blocks.LAVA)),
		AIR(Set.of(Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR, Blocks.SCAFFOLDING));
		final Set<Block> allowed;
		LIQUIDS(Set<Block> a){allowed = a;}
		public boolean matches(Block b)  { return allowed.contains(b); }
	}

	public boolean isEmpty() { return !full; }

	public boolean[] getSection(int y) {
		return (isEmpty() || y > top || y < bottom) ? null : sections[y-bottom];
	}

	public boolean[] getOrCreateSection(int y) {
		return getSection(y) == null ? initSection(y) : sections[y-bottom];
	}

	public boolean getBlock(int x, int y, int z) { // must be very fast
		final boolean[] section = getSection(y);
		return section != null && section[x + (z << 4)];
	}

	public boolean[] initSection(int y) {
		if (!full) {
			full = true;
			sectionsFullMap = new boolean[]{false};
			bottom = y;
			top = y;
			return (sections = new boolean[][]{{true}})[0];
		// prepend sections and update @bottom
		} else if (y < bottom) {
			sectionsFullMap = ArrayUtils.addAll(new boolean[bottom-y], sectionsFullMap);
			sections = ArrayUtils.addAll(new boolean[bottom-y][], sections);
			bottom = y;
		// append sections and update @top
		} else if (y > top) {
			sectionsFullMap = ArrayUtils.addAll(sectionsFullMap, new boolean[y-top]);
			sections = ArrayUtils.addAll(sections, new boolean[y-top][]);
			top = y;
		}
		// handles when full is true -> regardless of (bottom < y > top)
		return sections[y-bottom]=empty();
	}

	// allow explicit & implicit initialization {
	public LiquidStorage(boolean @NotNull [][] sections_, int top_, int bottom_, boolean[] sectionsFullMap_, WorldChunk chunk_){
		// if incorrect number of sections -> log an error, always initialize, however
		final int n = top_-bottom_+1;
		if (sections_.length != n || sectionsFullMap_.length != n) Engine.LOGGER.error("Top("+top_+") to Bottom("+bottom_+") != "+sections_.length+" or "+sectionsFullMap_.length);
		full = true;
		sections = sections_;
		top = top_;
		bottom = bottom_;
		sectionsFullMap = sectionsFullMap_;
		chunk = chunk_;
	}

	public LiquidStorage(WorldChunk chunk_ ){
		full = false; chunk = chunk_;
	}
	// }

	public void setBlock(int x, int y, int z, boolean block) { // rare ⇒ can be expensive
		if (x >= 16 || x < 0 || z >= 16 || z < 0 || y >= 320 || y < -64) Engine.LOGGER.error("Block coords ["+x+", "+y+", "+z+"] are out of bounds!");
		else if (getBlock(x, y, z) != block) {
			getOrCreateSection(y)[x+(z<<4)] = block;
			if (!block) {
				sectionsFullMap[y-bottom] = false;
				tryCull(y);
			}
			else {
				if (!ArrayUtils.contains(sections[y-bottom], false)) sectionsFullMap[y-bottom] = true;
			}
		}
	}

	public void tryCull(int y) {
		// when there's no loaded path to lower chunks, unload them
		if (!ArrayUtils.contains(sections[y - bottom], true)) {
			sections[y - bottom] = null;
			if (y == bottom) {
				int y1 = y;
				for (boolean[] s: sections) { if (s == null) y1++; else break; }
				if (y1 > top) unload();
				else {
					boolean[] x = ArrayUtils.subarray(sectionsFullMap, 0, 2);
					sections = ArrayUtils.subarray(sections, y1-bottom ,  top-bottom+1);
					sectionsFullMap = ArrayUtils.subarray(sectionsFullMap, y1-bottom ,  top-bottom+1);
					bottom = y1;
				}
			}
			else if (y == top) {
				int y1 = y;
				for (int i = 0, l = sections.length; i < l; i++) {if (sections[l-i-1] != null) {y1-=i; break;}}
				if (y1 == y) unload();
				else {
					sectionsFullMap = ArrayUtils.subarray(sectionsFullMap, 0 ,  y1-bottom+1);
					sections = ArrayUtils.subarray(sections, 0 ,  y1-bottom+1);
					top = y1;
				}
			}
		}
	}
	public void unload() {full = false; sections = null; sectionsFullMap = null;}
}
