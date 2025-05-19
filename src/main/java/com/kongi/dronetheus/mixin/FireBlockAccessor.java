package com.kongi.dronetheus.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FireBlock.class)
public interface FireBlockAccessor {

    @Invoker
    static int invokeGetFireTickDelay(Random random) {
        throw new AssertionError();
    }

    @Invoker
    boolean invokeIsRainingAround(World world, BlockPos pos);

    @Invoker
    boolean invokeAreBlocksAroundFlammable(BlockView world, BlockPos pos);

    @Invoker
    boolean invokeIsFlammable(BlockState state);

    @Invoker
    void invokeTrySpreadingFire(World world, BlockPos pos, int spreadFactor, Random random, int currentAge);

    @Invoker
    int invokeGetBurnChance(WorldView world, BlockPos pos);

    @Invoker
    BlockState invokeGetStateWithAge(WorldView world, BlockPos pos, int age);
}
