package com.kongi.dronetheus.mixin;

import com.kongi.dronetheus.Dronetheus;
import com.kongi.dronetheus.WindManager;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.random.Random;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.joml.Vector2d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(FireBlock.class)
public class FireSimulationMixin {
    //For accessing static properties
    @Shadow
    @Final
    public static IntProperty AGE;

    //    This ensures NOTHING runs after this, even other mods
    @Inject(method = "scheduledTick", at = @At("HEAD"), cancellable = true)
    private void onFireTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        //For class methods
        FireBlock thisObject = (FireBlock) (Object) this;
        //Call all class methods from the accessor
        FireBlockAccessor accessor = (FireBlockAccessor) thisObject;


        world.scheduleBlockTick(pos, thisObject, FireBlockAccessor.invokeGetFireTickDelay(world.random));

        if (world.getGameRules().getBoolean(GameRules.DO_FIRE_TICK)) {
            if (!state.canPlaceAt(world, pos)) {
                world.removeBlock(pos, false);
            }

            BlockState blockState = world.getBlockState(pos.down());
            boolean bl = blockState.isIn(world.getDimension().infiniburn());
            int age = (Integer) state.get(AGE);

            if (!bl && world.isRaining() && accessor.invokeIsRainingAround(world, pos) && random.nextFloat() < 0.2F + (float) age * 0.03F) {
                world.removeBlock(pos, false);
            } else {
                int j = Math.min(15, age + random.nextInt(3) / 2);
                if (age != j) {
                    state = (BlockState) state.with(AGE, j);
                    world.setBlockState(pos, state, 4);
                }

                if (!bl) {
                    if (!accessor.invokeAreBlocksAroundFlammable(world, pos)) {
                        BlockPos blockPos = pos.down();
                        if (!world.getBlockState(blockPos).isSideSolidFullSquare(world, blockPos, Direction.UP) || age > 3) {
                            world.removeBlock(pos, false);
                        }

                        return;
                    }

                    if (age == 15 && random.nextInt(4) == 0 && !accessor.invokeIsFlammable(world.getBlockState(pos.down()))) {
                        world.removeBlock(pos, false);
                        return;
                    }
                }

                boolean isBiomeFlamabble = world.getBiome(pos).isIn(BiomeTags.INCREASED_FIRE_BURNOUT);
                int k = isBiomeFlamabble ? -50 : 0;

                //This helps with deleting and replacing nearby blocks with fire blocks, DOES NOT SPREAD
                accessor.invokeTrySpreadingFire(world, pos.east(), 300 + k, random, age);
                accessor.invokeTrySpreadingFire(world, pos.west(), 300 + k, random, age);
                accessor.invokeTrySpreadingFire(world, pos.down(), 250 + k, random, age);
                accessor.invokeTrySpreadingFire(world, pos.up(), 250 + k, random, age);
                accessor.invokeTrySpreadingFire(world, pos.north(), 300 + k, random, age);
                accessor.invokeTrySpreadingFire(world, pos.south(), 300 + k, random, age);
                BlockPos.Mutable mutable = new BlockPos.Mutable();

//                This is what actually SPREADS fire
                for (int x = -1; x <= 1; ++x) {
                    for (int z = -1; z <= 1; ++z) {
                        for (int height = -1; height <= 4; ++height) {
                            if (x != 0 || height != 0 || z != 0) {
                                int heightModifier = 100;
                                if (height > 1) {
                                    heightModifier += (height - 1) * 100;
                                }

                                mutable.set(pos, x, height, z);

                                //Checks if this position has nearby flammable blocks
                                int posBurnChance = accessor.invokeGetBurnChance(world, mutable);
                                if (posBurnChance > 0) {
                                    int burnChanceModifier = (posBurnChance + 40 + world.getDifficulty().getId() * 7) / (age + 30);
                                    if (isBiomeFlamabble) {
                                        burnChanceModifier /= 2;
                                    }

                                    //region Custom Burn code
                                    var currentWind = WindManager.getInstance().currentWind;
                                    Vec2f currentWindVec = new Vec2f((float) (currentWind.directionX()),(float) (currentWind.directionZ()));
                                    var currentVec = new Vec2f((float) x, (float) z);
                                    var dot = currentWindVec.dot(currentVec);
                                    //Normalize the dot product to another range:

                                    var burnChanceMult = MathHelper.map(dot, -1, 1, -currentWind.strength(), currentWind.strength());
                                    burnChanceModifier += burnChanceMult;
//                                    if (x > 0) {
//                                        burnChanceModifier *= 100;
//                                    }
                                    //endregion

                                    //Usually : brunChanceModifier ~=1, heightModifier ~=100 (when single block of fire on a lot of wood)
                                    if (burnChanceModifier > 0 && random.nextInt(heightModifier) <= burnChanceModifier && (!world.isRaining() || !accessor.invokeIsRainingAround(world, mutable))) {
                                        int r = Math.min(15, age + random.nextInt(5) / 4);
                                        world.setBlockState(mutable, accessor.invokeGetStateWithAge(world, mutable, r), 3);
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
        ci.cancel();
    }



}