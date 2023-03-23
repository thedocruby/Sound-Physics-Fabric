package dev.thedocruby.resounding.raycast;

import static dev.thedocruby.resounding.Engine.env;
import static dev.thedocruby.resounding.Engine.pseudoReflect;
import static dev.thedocruby.resounding.raycast.Cache.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;

@Environment(EnvType.CLIENT)
public class Cast {

    public static World world = null;

    private Cast() {
    }

    private final static BlockPos single = new BlockPos(1,1,1);

    private static Vec3d getStep(Vec3d base, Vec3d size, Vec3d position, Vec3d vector) {
        /* return a new position, based on which bounding wall will be hit first
         * this is for path tracing using an octree

         ** base     = block/chunk start position
         ** size     = octree segment size
         ** position = current ray position
         ** vector   = trajectory
         */

        // normalize magnitude to closest wall
        double xstep = boundAxis(base.getX(), position.getX(), size.getX(), vector.getX());
        double ystep = boundAxis(base.getY(), position.getY(), size.getY(), vector.getY());
        double zstep = boundAxis(base.getZ(), position.getZ(), size.getZ(), vector.getZ());

        // closest wall -> magnitude
        return vector.multiply(Math.min(xstep, Math.min(ystep, zstep)));
    }

    private static double boundAxis(double base, double pos, double size, double angle) {
        final double isPos = angle > 0 ? 1 : 0;
        // normalize position, determine distance, apply direction
        return (base - pos  + size * isPos) / angle;
        /*     (dist + (   size   )) / angle = magnitude
         *     (   1 + (16  * 0   )) / -2    = -1/2
         *     (   7 + (16  * 1   )) /  2    = 14/2
         */
    }

    public static LinkedList<Collision> raycast(@Nullable WorldChunk chunk, @NotNull Vec3d position, @NotNull Vec3d trajectory) {
        return raycast(chunk, position, trajectory, 128);
    }

    public static LinkedList<Collision> raycast(@Nullable WorldChunk chunk, @NotNull Vec3d position, @NotNull Vec3d trajectory, double power) {
        // vibrations dissipate in the environment, power = limiter
        Vec3d vector = trajectory;
        Branch branch = getBlock(chunk, position);
        LinkedList<Collision> collisions = new LinkedList<>();
        while (power > 1) {
            // miss when chunk not loaded
            if (branch == null) break;
            assert branch.state != null; // should never be null
            Pair<Double,Double> attributes = blockMap.get(branch.state.getBlock());
            /* use step algorithm
             * (A) position of ray
             * (B) start    of branch
             * (C) size     of branch
             * (D) direction (disregarding magnitude)
             */
            Vec3d step = getStep(position,blockToVec(branch.start), blockToVec(branch.size), vector);
            double distance = step.length();
            position = position.add(step);
            branch = getBlock(chunk, position);

            double reflect = attributes.getRight();
            double absorb = attributes.getLeft();
            if (reflect > absorb) {
                if (absorb < 0.5) {
                    // TODO determine proper way to add extra detail here
                    // THIS IS *NOT* the proper way to do this, but it might work for now
                    LinkedList<Collision> newCollisions = raycast(chunk, position, vector, power / 2);
                    collisions.addAll(newCollisions);
                }
                vector = pseudoReflect(vector, new Vec3i(1, 1, 1));
            } else {
                // absorption
                power *= 1 - attributes.getRight() * distance;
            }
            power -= absorb - reflect;
            // reflectivity
            // power *= ?;
        }
        return collisions;
    }

    public static Branch getBlock(@Nullable WorldChunk chunk, Vec3d pos) {
        if (chunk == null) return null;
        // round position
        BlockPos block = new BlockPos(pos);
        // obtain tree for section within chunk
        @NotNull Branch tree = overlay.get(chunk.getPos());
        Branch branch = tree.get(block);
        // when branch is nonexistent, wrap underlying block
        if (branch.state == null) {
            return new Branch(block, single, chunk.getBlockState(block));
        } else return branch;
    }

    private static Vec3d blockToVec(BlockPos pos) {
        return new Vec3d(pos.getX(), pos.getY(), pos.getZ());
    }

    private static Vec3d normalize(Vec3d pos) {
        return new Vec3d(pos.getX() % 16, pos.getY() % 16, pos.getZ() % 16);
    }
}