package com.kongi.dronetheus.mixin;

import com.kongi.dronetheus.Dronetheus;
import com.kongi.dronetheus.DronetheusCommon;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFireBlock.class)
public class FireParticlesMixin {

	/// This is from AbstractFireBlock because it likely runs on both SERVER and CLIENT
	/// The original function looks at all Directions, if they already are on fire too, we reduce the amount of particles,
	/// This function isnt gonna care about that
	@Inject(method = "randomDisplayTick", at = @At("HEAD"), cancellable = true)
	private void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random, CallbackInfo ci) {
		FireBlock thisObject = (FireBlock) (Object) this;
		if (thisObject != null) {
			Dronetheus.LOGGER.debug("SUPPPP");
		}
		else {
			Dronetheus.LOGGER.debug("Bruuh");
		}

		double d = pos.getX() + random.nextDouble();
		double e = pos.getY() + random.nextDouble() * 0.5 + 0.5;
		double f = pos.getZ() + random.nextDouble();
		world.addParticle(DronetheusCommon.THICK_SMOKE_PARTICLE, d, e, f, 0.0, 0.0, 0.0);
		ci.cancel();
	}
}