package com.kongi.dronetheus;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

//Theres no such thing as a "server only mod", this is effectively common code, just seperating out common code to server code
//https://docs.fabricmc.net/develop/getting-started/project-structure#entrypoints
public class DronetheusCommon implements ModInitializer {
    @Override
    public void onInitialize() {
        // Register the payload type
        PayloadTypeRegistry.playS2C().register(FireTruckPositionS2CPayload.ID, FireTruckPositionS2CPayload.CODEC);
    }
} 