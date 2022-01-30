package dev.thedocruby.resounding.performance;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;

@Environment(EnvType.CLIENT)
public class RaycastFix {

    private RaycastFix() {}

    public static long lastUpd = 0;
    public static Map<Long, Shapes> shapeCache = new ConcurrentHashMap<>(2048);
    // reset every tick, usually up to 2200
        // {pos, (block state, block, fluid) }

    public static int maxY;
    public static int minY;
    public static int maxX;
    public static int minX;
    public static int maxZ;
    public static int minZ;

    private final static VoxelShape EMPTY = VoxelShapes.empty();
    private final static VoxelShape CUBE = VoxelShapes.fullCube();

    // ===copied and modified===

    public static SPHitResult fixedRaycast(Vec3d start, Vec3d end, World world, @Nullable BlockPos ignore, @Nullable WorldChunk chunk) {
        LiquidStorage currentNotAirStorage = chunk == null ? null : ((WorldChunkAccess)chunk).getNotAirLiquidStorage();
        int currentX = chunk == null ? ((int) Math.floor(start.x)) >> 4 : chunk.getPos().x;
        int currentZ = chunk == null ? ((int) Math.floor(start.z)) >> 4 : chunk.getPos().z;
        boolean[] currentSlice = currentNotAirStorage == null ? null :currentNotAirStorage.getSection((int)Math.floor(start.y));
        int currentY = (int) start.y;
        // <editor-fold desc="end = clamp(context.getEnd(), start);">

        if (start.x > maxX || start.y > maxY || start.z > maxZ || start.x < minX || start.y < minY || start.z < minZ) {
            return SPHitResult.createMissed(start, null, new BlockPos(start), chunk);
        }
        Vec3d delta = end.subtract(start);
        if (!(end.x <= maxX && end.y <= maxY && end.z <= maxZ && end.x >= minX && end.y >= minY && end.z >= minZ)) {
            double fx = delta.x == 0 ? Double.MAX_VALUE : (((delta.x > 0 ? maxX : minX) - start.x) / delta.x);
            double fy = delta.y == 0 ? Double.MAX_VALUE : (((delta.y > 0 ? maxY : minY) - start.y) / delta.y);
            double fz = delta.z == 0 ? Double.MAX_VALUE : (((delta.z > 0 ? maxZ : minZ) - start.z) / delta.z);
            double factor = Math.min(Math.min(fx, fy), fz);
            delta = delta.multiply(factor);
            end = start.add(delta);
        }//</editor-fold>


        if (start.equals(end)) {
            return SPHitResult.createMissed(end, null, new BlockPos(end), chunk);
        } else {
            double xe1 = MathHelper.lerp(-1.0E-7D, end.x, start.x); // x end v1.1
            double ye1 = MathHelper.lerp(-1.0E-7D, end.y, start.y);
            double ze1 = MathHelper.lerp(-1.0E-7D, end.z, start.z);
            double xs1 = MathHelper.lerp(-1.0E-7D, start.x, end.x); // x start v1.1
            double ys1 = MathHelper.lerp(-1.0E-7D, start.y, end.y);
            double zs1 = MathHelper.lerp(-1.0E-7D, start.z, end.z);
            int xbs = MathHelper.floor(xs1); // x blockPos start v1.1
            int ybs = MathHelper.floor(ys1);
            int zbs = MathHelper.floor(zs1);
            BlockPos.Mutable blockPosStart1 = new BlockPos.Mutable(xbs, ybs, zbs);
            //////////////////////
            int xx = xbs >> 4; int zz = zbs >> 4;
            if (currentX != xx || currentZ != zz) {
                if (currentNotAirStorage != null) {
                    int ddx = currentX - xx; int ddz = currentZ - zz;
                    if (ddz == 0) {
                        if (ddx == -1) chunk = currentNotAirStorage.xm;
                        else if (ddx == 1) chunk = currentNotAirStorage.xp;
                    } else if (ddx == 0) {
                        if (ddz == -1) chunk = currentNotAirStorage.zm;
                        else if (ddz == 1) chunk = currentNotAirStorage.zp;
                    } else chunk = (WorldChunk) world.getChunk(xx, zz, ChunkStatus.FULL, false);
                } else chunk = (WorldChunk) world.getChunk(xx, zz, ChunkStatus.FULL, false);
                currentX = xx; currentZ = zz; currentY = ybs;
                currentNotAirStorage = chunk == null ? null : ((WorldChunkAccess)chunk).getNotAirLiquidStorage();
                currentSlice = currentNotAirStorage == null ? null : currentNotAirStorage.getSection(ybs);
            } else if (ybs != currentY) {
                currentSlice = currentNotAirStorage == null ? null : currentNotAirStorage.getSection(ybs);
                currentY = ybs;
            }
            /////////////////////
            final SPHitResult hitResultStart;
            if (currentSlice == null || !currentSlice[(xbs & 15) + ((zbs & 15) << 4)] || blockPosStart1.equals(ignore))
                hitResultStart = null;
            else {
                BlockState bs1 = chunk.getBlockState(blockPosStart1);
                if (bs1.isAir() || bs1.getBlock().equals(Blocks.MOVING_PISTON)) hitResultStart = null;
                else hitResultStart = finalRaycast(world, bs1, blockPosStart1, start, end, chunk, (short)4);
            }

            if (hitResultStart != null) {
                return hitResultStart;
            } else {
                double dx = xe1 - xs1;
                double dy = ye1 - ys1;
                double dz = ze1 - zs1;
                int dirx = MathHelper.sign(dx);
                int diry = MathHelper.sign(dy);
                int dirz = MathHelper.sign(dz);
                double rdx = dirx == 0 ? 1.7976931348623157E300D : (double)dirx / dx; // 1/dx
                double rdy = diry == 0 ? 1.7976931348623157E300D : (double)diry / dy;
                double rdz = dirz == 0 ? 1.7976931348623157E300D : (double)dirz / dz;
                double tx = rdx * (dirx > 0 ? 1.0D - MathHelper.fractionalPart(xs1) : MathHelper.fractionalPart(xs1)); // relative to blockPos start
                double ty = rdy * (diry > 0 ? 1.0D - MathHelper.fractionalPart(ys1) : MathHelper.fractionalPart(ys1));
                double tz = rdz * (dirz > 0 ? 1.0D - MathHelper.fractionalPart(zs1) : MathHelper.fractionalPart(zs1));

                SPHitResult object2 = null;
                do {
                    if (tx > 1.0D && ty > 1.0D && tz > 1.0D) {
                        return SPHitResult.createMissed(end, null, new BlockPos(end), chunk);
                    }

                    short side;

                    if (tx < ty) {
                        if (tx < tz) {
                            xbs += dirx;
                            tx += rdx;
                            side = 1;
                        } else {
                            zbs += dirz;
                            tz += rdz;
                            side = 3;
                        }
                    } else if (ty < tz) {
                        ybs += diry;
                        ty += rdy;
                        side = 2;
                    } else {
                        zbs += dirz;
                        tz += rdz;
                        side = 3;
                    }

                    /////////////////////
                    int x = xbs >> 4; int z = zbs >> 4;
                    if (currentX != x || currentZ != z) {
                        if (currentNotAirStorage != null) {
                            int ddx = currentX - x; int ddz = currentZ - z;
                            if (ddz == 0) {
                                if (ddx == -1) chunk = currentNotAirStorage.xm;
                                else if (ddx == 1) chunk = currentNotAirStorage.xp;
                            } else if (ddx == 0) {
                                if (ddz == -1) chunk = currentNotAirStorage.zm;
                                else if (ddz == 1) chunk = currentNotAirStorage.zp;
                            } else chunk = (WorldChunk) world.getChunk(x, z, ChunkStatus.FULL, false);
                        } else chunk = (WorldChunk) world.getChunk(x, z, ChunkStatus.FULL, false);
                        currentX = x; currentZ = z; currentY = ybs;
                        currentNotAirStorage = chunk == null ? null : ((WorldChunkAccess)chunk).getNotAirLiquidStorage();
                        currentSlice = currentNotAirStorage == null ? null : currentNotAirStorage.getSection(ybs);
                    } else if (ybs != currentY) {
                        currentSlice = currentNotAirStorage == null ? null : currentNotAirStorage.getSection(ybs);
                        currentY = ybs;
                    }
                    /////////////////////
                    blockPosStart1.set(xbs, ybs, zbs);
                    //Resounding.t1();
                    //final long t = System.nanoTime();// rm
                    //Resounding.tt.addAndGet(System.nanoTime()-t);// rm

                    if (currentSlice != null) {
                        if (currentSlice[(xbs & 15) + ((zbs & 15) << 4)] && !blockPosStart1.equals(ignore)) {
                            BlockState bs = chunk.getBlockState(blockPosStart1);

                            if (!bs.isAir() && !bs.getBlock().equals(Blocks.MOVING_PISTON)) {
                                Vec3d start1;
                                double f;
                                if (side == 1) {
                                    f = (((-dirx * 0.499 + xbs + 0.5) - start.x) * rdx * dirx);
                                } else if (side == 2) {
                                    f = (((-diry * 0.499 + ybs + 0.5) - start.y) * rdy * diry);
                                } else {
                                    f = (((-dirz * 0.499 + zbs + 0.5) - start.z) * rdz * dirz);
                                }
                                start1 = start.add(delta.multiply(f));
                                Vec3d end1;
                                double fx = (((dirx * 0.5001 + xbs + 0.5) - start.x) * rdx * dirx);
                                double fy = (((diry * 0.5001 + ybs + 0.5) - start.y) * rdy * diry);
                                double fz = (((dirz * 0.5001 + zbs + 0.5) - start.z) * rdz * dirz);
                                end1 = start.add(delta.multiply(Math.min(Math.min(fx, fy), fz)));

                                object2 = finalRaycast(world, bs, blockPosStart1, start1, end1, chunk, side);
                            }
                        }
                    } else {
                        int dx1 = ((dirx * 15 + (x<<5) + 15)>>1) - xbs; int dz1 = ((dirz * 15 + (z<<5) + 15)>>1) - zbs;
                        double dtx1 = dx1 * rdx * dirx; double dtz1 = dz1 * rdz * dirz;
                        if (currentNotAirStorage == null || currentNotAirStorage.isEmpty()
                                || (diry == 1 && ybs > currentNotAirStorage.top) || (diry == -1 && ybs < currentNotAirStorage.bottom)
                                || (dtx1+tx < ty && dtz1+tz < ty)) {

                            if (dtx1 > dtz1){
                                zbs+=dz1; tz+=dtz1;
                            } else {
                                xbs+=dx1; tx+=dtx1;
                            }
                        } else { while (dx1 > 0 && dz1 > 0) {
                            if (tx < ty) {
                                if (tx < tz) {
                                    xbs += dirx;
                                    tx += rdx;
                                    dx1-=dirx;
                                } else {
                                    zbs += dirz;
                                    tz += rdz;
                                    dz1-=dirz;
                                }
                            } else if (ty < tz) {
                                break;
                            } else {
                                zbs += dirz;
                                tz += rdz;
                                dz1-=dirz;
                            }
                        } }
                    }
                } while(object2 == null);

                return object2;
            }
        }
    }

    private static SPHitResult finalRaycast(World world, BlockState bs, BlockPos pos, Vec3d start, Vec3d end, WorldChunk c, Short side) {
        long posl = pos.asLong();
        Shapes shapes;
        shapes = shapeCache.get(posl);
        if (shapes == null) {
            if (pC.dRays) world.addParticle(ParticleTypes.END_ROD, false, pos.getX() + 0.5d, pos.getY() + 1d, pos.getZ() + 0.5d, 0, 0, 0);
            VoxelShape collisionShape = bs.getCollisionShape(world, pos);
            shapes =  new Shapes(collisionShape == EMPTY ? null : collisionShape, null);
            shapeCache.put(posl, shapes);
        }

        VoxelShape voxelShape = shapes.getSolid();//BlockShape
        if (voxelShape == CUBE) {
            Direction direction =
                    side == 1 ? Direction.EAST :
                    side == 2 ? Direction.UP :
                    side == 3 ? Direction.NORTH :
                    Direction.getFacing(start.x-pos.getX()-0.5, start.y-pos.getY()-0.5, start.z-pos.getZ()-0.5);
            return new SPHitResult(false, start, direction, pos, bs, c);
        }
        return voxelShape == null ? null : SPHitResult.get(voxelShape.raycast(start, end, pos), bs, c);
    }
}
