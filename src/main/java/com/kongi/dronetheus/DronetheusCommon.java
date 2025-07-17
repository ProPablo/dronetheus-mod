package com.kongi.dronetheus;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

//Theres no such thing as a "server only mod", this is effectively common code, just seperating out common code to server code
//https://docs.fabricmc.net/develop/getting-started/project-structure#entrypoints
public class DronetheusCommon implements ModInitializer {

    public static final SimpleParticleType THICK_SMOKE_PARTICLE = FabricParticleTypes.simple();

    @Override
    public void onInitialize() {
        // Register the payload type
        PayloadTypeRegistry.playS2C().register(FireTruckPositionS2CPayload.ID, FireTruckPositionS2CPayload.CODEC);

        // Register our custom particle type in the mod initializer.
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of(Dronetheus.MOD_ID, "thick_smoke"), THICK_SMOKE_PARTICLE);
    }
} 