package dev.thedocruby.resounding.performance;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.shape.VoxelShape;

@Environment(EnvType.CLIENT)
public record Shapes(VoxelShape solid, VoxelShape liquid) {

    public VoxelShape getSolid() {
        return this.solid;
    }
    public VoxelShape getLiquid() {
        return this.liquid;
    }
}
