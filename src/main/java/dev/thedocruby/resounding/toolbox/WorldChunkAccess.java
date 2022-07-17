package dev.thedocruby.resounding.toolbox;

import dev.thedocruby.resounding.raycast.LiquidStorage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface WorldChunkAccess {

	LiquidStorage getNotAirLiquidStorage();
	//LiquidStorage getWaterLiquidStorage();
	//LiquidStorage getLavaLiquidStorage();

}
