package com.kongi.dronetheus.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.kongi.dronetheus.WindManager;

@Mixin(FireBlock.class)
public class FireSimulationMixin {
    
    @Inject(method = "scheduledTick", at = @At("HEAD"))
    private void onFireTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        // Get current wind parameters
        WindManager.WindParameters wind = WindManager.getInstance().getWindParameters();
        
        // Apply wind effects to fire spread
        if (wind.strength() > 0) {
            // Calculate wind-affected positions
            BlockPos windwardPos = pos.add(
                (int) (wind.directionX() * wind.strength()),
                0,
                (int) (wind.directionZ() * wind.strength())
            );
            
            // Increase fire spread chance in wind direction
            if (random.nextFloat() < wind.strength() * 0.2f) {
                BlockState targetState = world.getBlockState(windwardPos);
                if (targetState.isAir()) {
                    world.setBlockState(windwardPos, state);
                }
            }
            
            // Modify fire height based on wind strength
            if (random.nextFloat() < wind.strength() * 0.1f) {
                BlockPos upPos = pos.up();
                BlockState upState = world.getBlockState(upPos);
                if (upState.isAir()) {
                    world.setBlockState(upPos, state);
                }
            }
        }
    }
}