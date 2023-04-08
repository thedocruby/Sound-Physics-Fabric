package dev.thedocruby.resounding.raycast;

import dev.thedocruby.resounding.toolbox.ChunkChain;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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

import static dev.thedocruby.resounding.Cache.CUBE;
import static dev.thedocruby.resounding.Cache.EMPTY;
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

    private static Step getStep(Vec3d base, int size, Vec3d position, Vec3d vector) {
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

        // branch hint: 1/3 probability -> NO
        // same as min(x,min(y,z)) + planar index
        if (ystep < coefficient) {
            coefficient = ystep;
            planarIndex = new Vec3i(0,1,0);
        }
        if (zstep < coefficient) {
            coefficient = zstep;
            planarIndex = new Vec3i(0,0,1);
        }
        // apply proper normal face
        planarIndex = planarIndex.multiply(-1 * (int) Math.signum(coefficient));
        // closest wall -> magnitude
        return new Step(vector.multiply(coefficient),planarIndex);
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

    public Ray raycast(@NotNull Vec3d position, @NotNull Vec3d trajectory, @NotNull Vec3d base, double transmission, int size) {
        // TODO: settings.rayStrength & volume -> amplitude
        return raycast(position, trajectory, base, transmission, size, 128);
    }

    public Ray raycast(@NotNull Vec3d position, @NotNull Vec3d trajectory, @NotNull Vec3d base, double transmission, int size, double power) {
        /* use step algorithm
         * (A) start    of branch
         * (B) size     of branch
         * (C) position of ray
         * (D) direction (minus magnitude)
         */
        Step step = getStep(base, size, position, trajectory);
        double distance = step.step().length();
        position = position.add(step.step());

        Branch branch = getBlock(position);
        // miss when section not loaded
        if (branch == null) return new Ray(distance, position, null, 0.0, null, 0.0);
        if (branch.size == 1) {
            step = bounce(world,branch.state,new BlockPos(position),position,trajectory);
            if (step != null) {
                distance += step.step().length();
                position = step.step();
            }
        }

        // TODO clean up
        @Nullable Pair<Double,Double> attributes;// = blockMap.get(branch.state.getBlock());
        //*/
        /*
        final double reflec =   pC.reflMap.get(branch.state.getBlock().getTranslationKey());
        final double perm   = 1-pC.absMap.get(branch.state.getBlock().getTranslationKey());
        //*/
        /* TODO remove
        final double reflec = 0.8; // Math.random();
        final double perm = 0.7; // Math.random();
        //*/
        // attributes = new Pair<>(reflec,perm);
        // in the event of a modded block
        /*if (attributes == null) {
            final BlockPos blockPos = new BlockPos(position);
            final double hardness = (double) Math.min(5,world.getBlockState(blockPos).getHardness(world, blockPos)) / 5 / 4;
            attributes = new Pair<>(hardness * 3,1-hardness);
        }*/
        // TODO remove
        if (branch.state.getBlock() == Blocks.STONE)
            attributes = new Pair<>(1.0,0.0);
        else
            attributes = new Pair<>(0.0,transmission);

        // apply permeation
        power *= Math.min(1,Math.pow(transmission,distance));

        // coefficients for reflection and for permeability
        double reflect  = attributes.getLeft();//*Math.min(1,distance);
        @Nullable Vec3d reflected = null;
        @Nullable Vec3d permeated = null;
        if (power > 0) {
            // if reflection / permeation -> calculate -> bounce / refract
            reflected = reflect <= 0 ? null : pseudoReflect(trajectory,step.plane());
            // use single-surface refraction here, unpredictable effects with larger objects & permeation coefficients
            permeated = pseudoReflect(trajectory, step.plane(), 1-attributes.getRight());
        }

        return new Ray(distance, position, permeated, attributes.getRight() /* <- transmission */, reflected, reflect*power);
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

    public static Vec3d blockToVec(BlockPos pos) { return new Vec3d(pos.getX(), pos.getY(), pos.getZ()); }


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

    private @Nullable Step bounce(World world, BlockState state, @NotNull BlockPos pos, Vec3d start, Vec3d vector) {
		final long posl = pos.asLong();
        Map<Long, VoxelShape> shapes = chunk.getShapes();
		VoxelShape shape = shapes.get(posl);
        // TODO evaluate actual benefit for shape cache
		if (shape == null) {
			if (pC.dRays) world.addParticle(ParticleTypes.END_ROD, false, pos.getX() + 0.5d, pos.getY() + 1d, pos.getZ() + 0.5d, 0, 0, 0);
			shape = state.getCollisionShape(world, pos);
            if (shape == null) return null;
			shapes.put(posl, shape);
		}
        if (shape == CUBE || shape == EMPTY) return null;

        BlockHitResult hit = shape.raycast(start, start.add(vector.multiply(2)), pos);
        return hit == null ? null : new Step(hit.getPos(),hit.getSide().getVector());
	}
}
