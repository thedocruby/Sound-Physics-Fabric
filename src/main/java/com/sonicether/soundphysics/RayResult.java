package com.sonicether.soundphysics;

import static com.sonicether.soundphysics.config.PrecomputedConfig.pC;

public class RayResult {
    public int bounce;
    public double missed;
    public double totalDistance;
    public double totalReflectivity;
    public double[] shared = new double[pC.nRayBounces];
    public double[] playerDistance = new double[pC.nRayBounces];
    public double[] bounceDistance = new double[pC.nRayBounces];
    public double[] totalBounceDistance = new double[pC.nRayBounces];
    public double[] bounceReflectivity = new double[pC.nRayBounces];
    public double[] totalBounceReflectivity = new double[pC.nRayBounces];
}
