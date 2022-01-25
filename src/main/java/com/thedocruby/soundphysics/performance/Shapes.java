package com.thedocruby.soundphysics.performance;

import net.minecraft.util.shape.VoxelShape;

public record Shapes(VoxelShape solid, VoxelShape liquid) {

    public VoxelShape getSolid() {
        return this.solid;
    }
    public VoxelShape getLiquid() {
        return this.liquid;
    }
}
