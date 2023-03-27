package dev.thedocruby.resounding.raycast;

import static dev.thedocruby.resounding.raycast.Cache.*;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class Cast {

    public @Nullable Chunk chunk = null;
    public @Nullable Branch tree = null;

    public Cast(@Nullable Branch tree, @Nullable Chunk chunk) {
        this.tree = tree;
        this.chunk = chunk;
    }

    private final static BlockPos single = new BlockPos(1,1,1);

    private static Pair<Vec3d,Vec3i> getStep(Vec3d base, Vec3d size, Vec3d position, Vec3d vector) {
        /* return a new position, based on which bounding wall will be hit first
         * this is for path tracing using an octree

         ** base     = diquad start position
         ** size     = diquad size
         ** position = ray position
         ** vector   = ray trajectory
         */

        // normalize magnitude to closest wall
        double xstep = boundAxis(base.getX(), position.getX(), size.getX(), vector.getX());
        double ystep = boundAxis(base.getY(), position.getY(), size.getY(), vector.getY());
        double zstep = boundAxis(base.getZ(), position.getZ(), size.getZ(), vector.getZ());

        Vec3i planarIndex = new Vec3i(1,0,0);
        double coefficient = xstep;

        // same as min(x,min(y,z)) + planar index
        if (ystep < coefficient) {
            coefficient = ystep;
            planarIndex = new Vec3i(0,1,0);
        }
        if (zstep < coefficient) {
            coefficient = zstep;
            planarIndex = new Vec3i(0,0,1);
        }
        // closest wall -> magnitude
        return new Pair<>(vector.multiply(coefficient),planarIndex);
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

    public Ray raycast(@NotNull Vec3d position, @NotNull Vec3d trajectory) {
        // TODO: settings.rayStrength
        return raycast(position, trajectory, 128);
    }

    public Ray raycast(@NotNull Vec3d position, @NotNull Vec3d trajectory, double power) {
        Branch branch = getBlock(position);
        // miss when section not loaded
        if (branch == null) return new Ray(0, position, null, 0.0, null, 0.0);
        assert branch.state != null; // should never be null
        Pair<Double,Double> attributes = blockMap.get(branch.state.getBlock());
        /* use step algorithm
         * (A) position of ray
         * (B) start    of branch
         * (C) size     of branch
         * (D) direction (disregarding magnitude)
         */
        Pair<Vec3d,Vec3i> step = getStep(position,blockToVec(branch.start), blockToVec(branch.size), trajectory);
        final double distance = step.getLeft().length();
        position = position.add(step.getLeft());

        // coefficients for reflection and for permeability
        final double reflect  = attributes.getRight()*Math.min(1,distance);
        final double permeate = Math.min(1,1-attributes.getLeft())*distance;
        // if reflection / permeation -> calculate -> bounce / refract
        final @Nullable Vec3d reflected = reflect  <= 0 ? null : pseudoReflect(trajectory, step.getRight());
        final @Nullable Vec3d permeated = permeate <= 0 ? null : pseudoReflect(trajectory, step.getRight(), permeate);
        power--;

        return new Ray(distance, position, permeated, permeate*power, reflected, reflect*power);
    }

    public Branch getBlock(Vec3d pos) {
        if (this.chunk == null) return null;
        // round position
        BlockPos block = new BlockPos(pos);
        // obtain tree for section within section
        @Nullable Branch branch = this.tree.get(block);
        // when branch is nonexistent, wrap underlying block
        if (branch.state == null) {
            return new Branch(block, single, this.chunk.getBlockState(block));
        } else return branch;
    }

    private static Vec3d blockToVec(BlockPos pos) {
        return new Vec3d(pos.getX(), pos.getY(), pos.getZ());
    }

    private static Vec3d normalize(Vec3d pos) {
        return new Vec3d(pos.getX() % 16, pos.getY() % 16, pos.getZ() % 16);
    }

    @Contract("_, _ -> new") // reflection
    public static @NotNull Vec3d pseudoReflect(Vec3d ray, @NotNull Vec3i plane) {
        return pseudoReflect(ray,plane,2);
    }

    // low-accuracy/performance mode
    @Contract("_, _, _ -> new") // reflection
    public static @NotNull Vec3d pseudoReflect(Vec3d ray, @NotNull Vec3i plane, double fresnel) {
        // Fresnels on a 1-30 scale
        // TODO account for http://hyperphysics.phy-astr.gsu.edu/hbase/Tables/indrf.html

        final Vec3d planeD = new Vec3d(
            (double) plane.getX(),
            (double) plane.getY(),
            (double) plane.getZ()
        );
        // ( ray - plane * (normal/air) * dot(ray,plane) ) / air * normal
        // https://blog.demofox.org/2017/01/09/raytracing-reflection-refraction-fresnel-total-internal-reflection-and-beers-law/
        // adjusted for refraction approximation
        // and deliberately ignoring absorption (would decrease raytracing
        // accuracy, ironically)
        // for a visualization, see: https://www.math3d.org/UYUQRza8n
        assert fresnel != 0;
        return ray.subtract(
                planeD.multiply
                    ( fresnel / 5 // value arbitrarily set
                    * ray.dotProduct(planeD)
                    )
                // TODO research, is this branchless?
                ); // .normalize(); // retain velocity
    }
}
