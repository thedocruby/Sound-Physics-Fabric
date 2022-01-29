package dev.thedocruby.resounding.performance;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface WorldChunkAccess {

    LiquidStorage getNotAirLiquidStorage();
    //LiquidStorage getWaterLiquidStorage();
    //LiquidStorage getLavaLiquidStorage();

}
