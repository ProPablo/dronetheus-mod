package com.kongi.dronetheus.mixin;

import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.kongi.dronetheus.WindManager;


@Mixin(FireBlock.class)
public class FireSimulationMixin {
    @Shadow @Final public static IntProperty AGE;

    @Inject(method = "scheduledTick", at = @At("HEAD"))
    private void onFireTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        FireBlock thisObject = (FireBlock) (Object)this;
        FireBlockAccessor accessor = (FireBlockAccessor) thisObject;
        return;

//
//        world.scheduleBlockTick(pos, thisObject, FireBlockAccessor.invokeGetFireTickDelay(world.random));
//
//
//        if (world.getGameRules().getBoolean(GameRules.DO_FIRE_TICK)) {
//            if (!state.canPlaceAt(world, pos)) {
//                world.removeBlock(pos, false);
//            }
//
//            BlockState blockState = world.getBlockState(pos.down());
//            boolean bl = blockState.isIn(world.getDimension().infiniburn());
//            int i = (Integer)state.get(AGE);
//
//            accessor.invokeTrySpreadingFire(world, pos.east(), 1 , random, i*3);
//            if (!bl && world.isRaining() && accessor.invokeIsRainingAround(world, pos) && random.nextFloat() < 0.2F + (float)i * 0.03F) {
//                world.removeBlock(pos, false);
//            } else {
//                int j = Math.min(15, i + random.nextInt(3) / 2);
//                if (i != j) {
//                    state = (BlockState)state.with(AGE, j);
//                    world.setBlockState(pos, state, 4);
//                }
//
//                if (!bl) {
//                    if (!accessor.invokeAreBlocksAroundFlammable(world, pos)) {
//                        BlockPos blockPos = pos.down();
//                        if (!world.getBlockState(blockPos).isSideSolidFullSquare(world, blockPos, Direction.UP) || i > 3) {
//                            world.removeBlock(pos, false);
//                        }
//
//                        return;
//                    }
//
//                    if (i == 15 && random.nextInt(4) == 0 && !accessor.invokeIsFlammable(world.getBlockState(pos.down()))) {
//                        world.removeBlock(pos, false);
//                        return;
//                    }
//                }
//
//                boolean bl2 = world.getBiome(pos).isIn(BiomeTags.INCREASED_FIRE_BURNOUT);
//                int k = bl2 ? -50 : 0;
//                accessor.invokeTrySpreadingFire(world, pos.east(), 1 , random, i*3);
//                accessor.invokeTrySpreadingFire(world, pos.west(), 300 + k, random, i);
//                accessor.invokeTrySpreadingFire(world, pos.down(), 250 + k, random, i);
//                accessor.invokeTrySpreadingFire(world, pos.up(), 250 + k, random, i);
//                accessor.invokeTrySpreadingFire(world, pos.north(), 300 + k, random, i);
//                accessor.invokeTrySpreadingFire(world, pos.south(), 300 + k, random, i);
//                BlockPos.Mutable mutable = new BlockPos.Mutable();
//
//                for(int l = -1; l <= 1; ++l) {
//                    for(int m = -1; m <= 1; ++m) {
//                        for(int n = -1; n <= 4; ++n) {
//                            if (l != 0 || n != 0 || m != 0) {
//                                int o = 100;
//                                if (n > 1) {
//                                    o += (n - 1) * 100;
//                                }
//
//                                mutable.set(pos, l, n, m);
//                                int p = accessor.invokeGetBurnChance(world, mutable);
//                                if (p > 0) {
//                                    int q = (p + 40 + world.getDifficulty().getId() * 7) / (i + 30);
//                                    if (bl2) {
//                                        q /= 2;
//                                    }
//
//                                    if (q > 0 && random.nextInt(o) <= q && (!world.isRaining() || !accessor.invokeIsRainingAround(world, mutable))) {
//                                        int r = Math.min(15, i + random.nextInt(5) / 4);
//                                        world.setBlockState(mutable, accessor.invokeGetStateWithAge(world, mutable, r), 3);
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//
//            }
//        }
//        return;
    }
}