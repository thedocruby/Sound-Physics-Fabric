package dev.thedocruby.resounding.raycast;

import dev.thedocruby.resounding.Cache;
import dev.thedocruby.resounding.toolbox.ChunkChain;
import dev.thedocruby.resounding.toolbox.MaterialData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
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
import static dev.thedocruby.resounding.Engine.LOGGER;
import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;

@Environment(EnvType.CLIENT)
public class Cast {

    public @NotNull World world;

    public @Nullable ChunkChain chunk = null;
    public @Nullable Branch tree = null;

    public @Nullable Ray reflected = null;
    public @Nullable Ray permeated = null;

    public @Nullable Step stood = null;

    public static MaterialData air = Cache.getProperties(Blocks.AIR.getDefaultState());

    public Cast(@NotNull World world, @Nullable Branch tree, @Nullable ChunkChain chunk) {
        this.world = world;
        this.tree = tree;
        this.chunk = chunk;
    }
    //* raycast {
    public void raycast(@NotNull Vec3d position, @NotNull Vec3d angle) {
        // TODO: settings.rayStrength & volume -> amplitude
        raycast(position, angle, 128);
    }
    public void raycast(@NotNull Vec3d position, @NotNull Vec3d vector, double power) {
        //* access branch {
        // assert ray.vector() != null; // the power check above will catch this
        final Vec3d normalized = normalize(position,vector);
        chunk = chunk.access((int) normalized.x >> 4, (int) normalized.z >> 4);
        if (chunk != null) tree = chunk.getBranch((int) normalized.y >> 4);
        /*if (chunk != null && tree != null && tree.state == null) {
            chunk.layer(tree);
        }*/
        Branch branch = getBlock(normalized);
        if (branch == null) {
            blank(position);
            return;
        }
        // } */
        // prepare variables
        Step step, rstep;
        Vec3d  pposition, rposition;
        double pdistance, rdistance;

        //* true voxel handling {
        /* use step algorithm
         * (A) start    of branch
         * (B) size     of branch
         * (C) position of ray
         * (D) direction (minus magnitude)
         */
        step = getStep(blockToVec(branch.start), branch.size, position, vector);
        pdistance = step.step().length();
        pposition = position.add(step.step());

        // truncate to 5 decimals -> to prevent floating-point rounding errors
        // wish this didn't have to be done. Yet, this solves critical errors
        pposition = new Vec3d(
                ((long) (pposition.x * 1e5)) / 1e5,
                ((long) (pposition.y * 1e5)) / 1e5,
                ((long) (pposition.z * 1e5)) / 1e5);
        // } */
        // defaults
        rstep = stood != null ? stood : step;
        rdistance = 0;
        rposition = position;
        //* reflection w/ sub-voxel geometry (irony) {
        if (branch.size == 1) {
            Step next = bounce(branch,position,vector);
            if (next != null) {
                rstep = next;
                rdistance = rstep.step().subtract(position).length();
                rposition = rstep.step();
            }
        }
        // } */
        //* amplitude and vector {
        // material properties
        @Nullable MaterialData material = Cache.getProperties(branch.state);
        double reflectivity = material.reflectivity();
        double permeability = Math.pow(material.permeability(),pdistance);

        // if reflection / permeation -> calculate -> bounce / refract
        @Nullable Vec3d reflected = reflectivity > 0 ? pseudoReflect(vector,rstep.plane()) : null;
        // use single-surface refraction here, unpredictable effects with larger objects & permeation coefficients
        @Nullable Vec3d permeated = pseudoReflect(vector, step.plane(), (1-material.permeability()) / 5 /* TODO: make non-arbitrary */);
        // } */
        // apply movement
        reflect (reflectivity*power, rposition, reflected, rdistance);
        permeate(permeability*power, pposition, permeated, pdistance);
        stood = step; // TODO ?
    }
    // } */

    //* fetch {
    public static Vec3d normalize(Vec3d pos, Vec3d vector) {
        //return pos;
        return new Vec3d(
                vector.x < 0 ? Math.ceil(pos.x) - 1 : Math.floor(pos.x),
                vector.y < 0 ? Math.ceil(pos.y) - 1 : Math.floor(pos.y),
                vector.z < 0 ? Math.ceil(pos.z) - 1 : Math.floor(pos.z));
        // */
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
    // } */
    //* physics {
    private static Step getStep(Vec3d base, int size, Vec3d position, Vec3d vector) {
        /* return a new position, based on which bounding wall will be hit first
         * this is for path tracing using an octree

         ** base     = diquad start position
         ** size     = diquad size
         ** position = ray position
         ** vector   = ray trajectory
         */

        // normalize magnitude to closest wall
        double coefficient = boundAxis(base.x, position.x, size, vector.x);
        double ystep       = boundAxis(base.y, position.y, size, vector.y);
        double zstep       = boundAxis(base.z, position.z, size, vector.z);

        Vec3i planarIndex = new Vec3i(-Math.signum(vector.x),0,0);

        // branch hint: 1/3 probability -> NO
        // same as min(x,min(y,z)) + planar index
        if (ystep < coefficient) {
            coefficient = ystep;
            planarIndex = new Vec3i(0,-Math.signum(vector.y),0);
        }
        if (zstep < coefficient) {
            coefficient = zstep;
            planarIndex = new Vec3i(0,0,-Math.signum(vector.z));
        }
        if (coefficient == Double.POSITIVE_INFINITY) {
            LOGGER.warn("invalid coefficient");
            // coefficient = epsilon;
        }
        // closest wall -> magnitude
        return new Step(vector.multiply(coefficient),planarIndex);
    }
    private static double boundAxis(double base, double pos, double size, double dir) {
        // normalize position, determine distance, apply direction
        double value = (base - pos  +  (dir > 0 ? size : 0)) / dir;
        // theoretically zeroes/negatives shouldn't ever happen, but they did extensively during debugging
        // (and were promptly fixed!) But you can't ever be too sure.
        if (value <= 0 || Double.isNaN(value)) value = Double.POSITIVE_INFINITY; // endless loops -> always bigger
        return value;
        /*     (dist + (   size   )) / vector = magnitude
         *     (   1 + (16  * 0   )) / -2     = -1/2
         *     (   7 + (16  * 1   )) /  2     = 14/2
         */
    }
    @Contract("_, _ -> new")
    public static @NotNull Vec3d pseudoReflect(Vec3d ray, @NotNull Vec3i plane) { return pseudoReflect(ray,plane,2); }
    @Contract("_, _, _ -> new")
    public static @NotNull Vec3d pseudoReflect(Vec3d ray, @NotNull Vec3i plane, double fresnel) {
        // Fresnels on a 1-30 scale
        // TODO account for http://hyperphysics.phy-astr.gsu.edu/hbase/Tables/indrf.html

        final Vec3d planeD = new Vec3d(
                plane.getX(),
                plane.getY(),
                plane.getZ()
        );
        // ( ray - plane * (normal/air) * dot(ray,plane) ) / air * normal
        // https://blog.demofox.org/2017/01/09/raytracing-reflection-refraction-fresnel-total-internal-reflection-and-beers-law/
        // adjusted for refraction approximation
        // for a visualization, see: https://www.math3d.org/UYUQRza8n
        // assert fresnel != 0;
        // return ray.multiply(planeD.multiply(-1));
        //*
        return ray.subtract(
                planeD.multiply
                        (ray.multiply(planeD).multiply(fresnel))
        );
        // */
    }
    private @Nullable Step bounce(Branch branch, Vec3d start, Vec3d vector) {
        final long posl = branch.start.asLong();
        Map<Long, VoxelShape> shapes = chunk.getShapes();
        VoxelShape shape = shapes.get(posl);
        // TODO evaluate actual benefit for shape cache
        if (shape == null) {
            if (pC.dRays) world.addParticle(ParticleTypes.END_ROD, false, branch.start.getX() + 0.5d, branch.start.getY() + 1d, branch.start.getZ() + 0.5d, 0, 0, 0);
            shape = branch.state.getCollisionShape(world, branch.start);
            if (shape == null) return null;
            shapes.put(posl, shape);
        }
        if (shape == CUBE || shape == EMPTY) return null;

        BlockHitResult hit = shape.raycast(start, start.add(vector.multiply(2)), branch.start);
        return hit == null ? null : new Step(hit.getPos(),hit.getSide().getVector());
    }
    // } */
    //* mutate {
    public void blank(Vec3d position) {
        this.reflected = new Ray(0, position, null, 0);
        this.permeated = new Ray(0, position, null, 0);
    }
    private void reflect(/*MaterialData material,*/ double power, Vec3d position, Vec3d angle, double distance) {
        this.reflected = new Ray(power, position, angle, distance);
    }
    private void permeate(/*MaterialData material,*/ double power, Vec3d position, Vec3d angle, double distance) {
        this.permeated = new Ray(power, position, angle, distance);
    }
    // } */
}
