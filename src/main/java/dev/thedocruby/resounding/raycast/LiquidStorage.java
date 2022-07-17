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
	private boolean[] sFull;
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
		public boolean matches(Block b){ return allowed.contains(b); }
	}

	public boolean isEmpty(){return !full;}

	public boolean[] getSection(int y) {
		if (!full || y > top || y < bottom) return null;
		return sections[y-bottom];
	}

	public boolean[] getOrCreateSection(int y) {
		if (getSection(y) == null) return initSection(y);
		return sections[y-bottom];
	}

	public boolean getBlock(int x, int y, int z) { // must be very fast
		if (!full || y > top || y < bottom) return false;
		boolean[] section = sections[y-bottom];
		if (section == null) return false;
		return section[x+(z<<4)];
	}

	public boolean[] initSection(int y) {
		if (!full) {
			sFull = new boolean[]{false};
			bottom = y;
			top = y;
			full = true;
			return (sections = new boolean[][]{empty()})[0];
		} else if (y < bottom) {
			sFull = ArrayUtils.addAll(new boolean[bottom-y], sFull);
			sections = ArrayUtils.addAll(new boolean[bottom-y][], sections);
			bottom = y;
			return sections[0]=empty();
		} else if (y > top) {
			sFull = ArrayUtils.addAll(sFull, new boolean[y-top]);
			sections = ArrayUtils.addAll(sections, new boolean[y-top][]);
			top = y;
		}
		return sections[y-bottom]=empty();
	}

	//public LiquidStorage(){};
	public LiquidStorage(boolean @NotNull [][] s, int t, int b, boolean[] sf, WorldChunk ch ){
		int n = t-b+1; if (s.length != n || sf.length != n) Engine.LOGGER.error("Top("+t+") to Bottom("+b+") != "+s.length+" or "+sf.length);
		full = true; sections = s; top = t; bottom = b; sFull = sf; chunk = ch;
	}

	public LiquidStorage(WorldChunk ch ){
		full = false; chunk = ch;
	}

	public void setBlock(int x, int y, int z, boolean block) { // rare ⇒ can be expensive
		if (x >= 16 || x < 0 || z >= 16 || z < 0 || y >= 320 || y < -64) Engine.LOGGER.error("Block coords ["+x+", "+y+", "+z+"] are out of bounds!");
		else if (getBlock(x, y, z) != block) {
			getOrCreateSection(y)[x+(z<<4)] = block;
			if (!block) {
				sFull[y-bottom] = false;
				tryCull(y);
			}
			else {
				if (!ArrayUtils.contains(sections[y-bottom], false)) sFull[y-bottom] = true;
			}
		}
	}

	public void tryCull(int y){
		if (!ArrayUtils.contains(sections[y - bottom], true)) {
			sections[y - bottom] = null;
			if (y == bottom) {
				int y1 = y;
				for (boolean[] s: sections) { if (s == null) y1++; else break; }
				if (y1 > top) unload();
				else {
					sections = ArrayUtils.subarray(sections, y1-bottom ,  top-bottom+1);
					sFull = ArrayUtils.subarray(sFull, y1-bottom ,  top-bottom+1);
					bottom = y1;
				}
			}
			else if(y == top){
				int y1 = y;
				for (int i = 0, l = sections.length; i < l; i++) {if (sections[l-i-1] != null) {y1-=i; break;}}
				if (y1 == y) unload();
				else {
					sFull = ArrayUtils.subarray(sFull, 0 ,  y1-bottom+1);
					sections = ArrayUtils.subarray(sections, 0 ,  y1-bottom+1);
					top = y1;
				}
			}
		}
	}
	public void unload() {full = false; sections = null; sFull = null;}
}
