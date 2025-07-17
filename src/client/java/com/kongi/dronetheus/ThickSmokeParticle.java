package com.kongi.dronetheus;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

//Make this from the most base class, no need to fall in the inheritance trap
public class ThickSmokeParticle extends SpriteBillboardParticle {
    private final SpriteProvider spriteProvider;

    protected ThickSmokeParticle(ClientWorld world, double x, double y, double z,
                                 double velocityX, double velocityY, double velocityZ,
                                 SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.spriteProvider = spriteProvider;
        this.gravityStrength = -0.02f; // Negative for upward buoyancy
        this.scale = 0.3f + random.nextFloat() * 0.7f; // Random initial size
        this.scale = 15;
        this.maxAge = 100 + random.nextInt(50); // 5-7.5 seconds
        this.alpha = 0.8f + random.nextFloat() * 0.2f; // Semi-transparent
//        this.windResistance = 0.95f + random.nextFloat() * 0.04f; // Wind effect
//        this.buoyancy = 0.001f + random.nextFloat() * 0.002f; // Upward drift
//        this.textureAge = 0;

        // Darker smoke color with slight brown tint
        this.red = 0.1f + random.nextFloat() * 0.2f;
        this.green = 0.1f + random.nextFloat() * 0.15f;
        this.blue = 0.1f + random.nextFloat() * 0.1f;

        //We can use spriteProvider to set sprite to random too
        this.setSpriteForAge(spriteProvider);
    }

    //Use windsystem to influence the tick
    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE;
    }

    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider spriteProvider;

        //The factory gets provided a spriteprovider based on the registry and the json file with the same name as the key
        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientWorld world,
                                       double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            return new ThickSmokeParticle(world, x, y, z, velocityX, velocityY, velocityZ,
                    this.spriteProvider);
        }
    }
}
