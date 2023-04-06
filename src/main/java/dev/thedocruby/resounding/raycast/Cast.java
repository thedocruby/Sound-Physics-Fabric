package dev.thedocruby.resounding.raycast;

import dev.thedocruby.resounding.Cache;
import dev.thedocruby.resounding.toolbox.ChunkChain;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static dev.thedocruby.resounding.Cache.*;
import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;

@Environment(EnvType.CLIENT)
public class Cast {

    public @NotNull World world;

    public @Nullable ChunkChain chunk = null;
    public @Nullable Branch tree = null;

    public Cast(@NotNull World world, @Nullable Branch tree, @Nullable ChunkChain chunk) {
        this.world = world;
        this.tree = tree;
        this.chunk = chunk;
    }

    private static Pair<Vec3d,Vec3i> getStep(Vec3d base, int size, Vec3d position, Vec3d vector) {
        /* return a new position, based on which bounding wall will be hit first
         * this is for path tracing using an octree

         ** base     = diquad start position
         ** size     = diquad size
         ** position = ray position
         ** vector   = ray trajectory
         */

        // normalize magnitude to closest wall
        double coefficient = boundAxis(base.getX(), position.getX(), size, vector.getX()); // (xstep)
        double ystep       = boundAxis(base.getY(), position.getY(), size, vector.getY());
        double zstep       = boundAxis(base.getZ(), position.getZ(), size, vector.getZ());

        Vec3i planarIndex = new Vec3i(1,0,0);

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
        double value = (base - pos  +  size * isPos) / angle;
        // zeroes break the min² in getStep, infinity is always more than non-infinity
        if (value <= 0 || Double.isNaN(value)) value = Double.POSITIVE_INFINITY;
        return value;
        /*     (dist + (   size   )) / angle = magnitude
         *     (   1 + (16  * 0   )) / -2    = -1/2
         *     (   7 + (16  * 1   )) /  2    = 14/2
         */
    }

    public Ray raycast(@NotNull Vec3d position, @NotNull Vec3d trajectory) {
        // TODO: settings.rayStrength & volume -> amplitude
        return raycast(position, trajectory, 128);
    }

    public Ray raycast(@NotNull Vec3d position, @NotNull Vec3d trajectory, double power) {
        Branch branch = getBlock(position);
        // miss when section not loaded
        if (branch == null) return new Ray(0, position, null, 0.0, null, 0.0);

        // get reflectivity and permeability
        // assert branch.state != null; // is never null due to logic inside getBlock
        // TODO clean up
        @Nullable Pair<Double,Double> attributes;// = blockMap.get(branch.state.getBlock());
        /*
        final double reflec =   pC.reflMap.get(branch.state.getBlock().getTranslationKey());
        final double perm   = 1-pC.absMap.get(branch.state.getBlock().getTranslationKey());
        //*/
        // TODO remove
        final double reflec = Math.random();
        final double perm = Math.random();
        //*/
        attributes = new Pair<>(reflec,perm);
        // in the event of a modded block
        /*if (attributes == null) {
            final BlockPos blockPos = new BlockPos(position);
            final double hardness = (double) Math.min(5,world.getBlockState(blockPos).getHardness(world, blockPos)) / 5 / 4;
            attributes = new Pair<>(hardness * 3,1-hardness);
        }*/

        // calculate next position
        @Nullable Pair<Vec3d, Vec3i> step = null;
        double distance;



        // raycast on sub-voxel geometry
        if (branch.size == 1) {
            step = bounce(world, branch.state, new BlockPos(position),position,trajectory);
        }
        // on miss
        if (step == null) {
            /* use step algorithm
             * (A) start    of branch
             * (B) size     of branch
             * (C) position of ray
             * (D) direction (minus magnitude)
             */
            step = getStep(blockToVec(branch.start), branch.size, position, trajectory);
            distance = step.getLeft().length();
            position = position.add(step.getLeft());
        } else {
            // on collision with sub-voxel geometry
            distance = position.distanceTo(step.getLeft());
            position = step.getLeft();
        }

        // coefficients for reflection and for permeability
        final double reflect  = attributes.getLeft()*Math.min(1,distance);
        // final double permeate = Math.min(1,1-attributes.getLeft())*distance;
        final double permeate = Math.min(1,Math.pow(attributes.getRight(),distance)) * Cache.transmission;
        // if reflection / permeation -> calculate -> bounce / refract
        final @Nullable Vec3d reflected = reflect  <= 0 ? null : pseudoReflect(trajectory, step.getRight());
        // use single-surface refraction here, unpredictable effects with larger objects & permeation coefficients
        final @Nullable Vec3d permeated = permeate <= 0 ? null : pseudoReflect(trajectory, step.getRight(), 1-attributes.getRight());
        // TODO: determine if this' needed (seems inaccurate)
        // power--;

        return new Ray(distance, position, permeated, permeate*power, reflected, reflect*power);
    }

    public Branch getBlock(Vec3d pos) {
        if (this.chunk == null || this.tree == null) return null;
        // round position
        final BlockPos block = new BlockPos(pos);
        // obtain tree for layer within section
        // { [ ... ] _ _ _ _ _ _ _ }
        //           ^ ^ ^ ^ ^ ^ ^ state=null
        //      ^ state=e.g. air ...
        // a branch has either 0 or 8 children
        // therefore, tree.get will always return the smallest branch at a given location
        final Branch branch = this.tree.get(block);
        // when null, simply fall through and get the underlying block
        if (branch.state == null) {
            return new Branch(block, 1, ((WorldChunk) this.chunk).getBlockState(block));
        } else return branch;
    }

    private static Vec3d blockToVec(BlockPos pos) { return new Vec3d(pos.getX(), pos.getY(), pos.getZ()); }


    @Contract("_, _ -> new") // reflection
    public static @NotNull Vec3d pseudoReflect(Vec3d ray, @NotNull Vec3i plane) { return pseudoReflect(ray,plane,2); }

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
                );
    }

    private @Nullable Pair<Vec3d,Vec3i> bounce(World world, BlockState state, @NotNull BlockPos pos, Vec3d start, Vec3d vector) {
		final long posl = pos.asLong();
        Map<Long, VoxelShape> shapes = chunk.getShapes();
		VoxelShape shape = shapes.get(posl);
        // TODO evaluate actual benefit for shape cache
		if (shape == null) {
			if (pC.dRays) world.addParticle(ParticleTypes.END_ROD, false, pos.getX() + 0.5d, pos.getY() + 1d, pos.getZ() + 0.5d, 0, 0, 0);
			shape = state.getCollisionShape(world, pos);
            if (shape == null) return null;
			shapes.put(posl, shape);
            shape = shape == EMPTY ? null : shape;
		}

		if (shape == CUBE || shape == EMPTY) return null;
        BlockHitResult hit = shape.raycast(start, start.add(vector.multiply(2)), pos);
        return hit == null ? null : new Pair<>(hit.getPos(),hit.getSide().getVector());
	}
}
