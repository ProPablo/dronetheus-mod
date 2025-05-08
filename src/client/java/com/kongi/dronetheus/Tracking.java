package com.kongi.dronetheus;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class Tracking {
    // Takes position data from client class of the player and stores incoming otherPlayer data from callback
    // Uses that data to process the tracking algo
    public Vec3d trackingPlayerPos;

    public void TickUpdate(MinecraftClient client) {

    }
}
