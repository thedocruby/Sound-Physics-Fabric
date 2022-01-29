package dev.thedocruby.resounding;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;

@Environment(EnvType.CLIENT)
public class RayResult {
    public int lastBounce;
    public double missed;
    public double totalDistance;
    public double totalReflectivity;
    public double[] shared = new double[pC.nRayBounces];
    // public double[] energyToPlayer = new double[pC.nRayBounces];
    // public double[] bounceDistance = new double[pC.nRayBounces];
    public double[] totalBounceDistance = new double[pC.nRayBounces];
    public double[] bounceReflectivity = new double[pC.nRayBounces];
    public double[] totalBounceReflectivity = new double[pC.nRayBounces];
}
