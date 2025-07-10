package com.kongi.dronetheus;

import net.minecraft.util.math.Vec2f;

public class WindManager {
    private static WindManager instance;
    public WindParameters currentWind;

    private WindManager() {
        // Initialize with no wind
        currentWind = new WindParameters(0, 0, 0);
    }

    public static WindManager getInstance() {
        if (instance == null) {
            instance = new WindManager();
        }
        return instance;
    }
    public record WindParameters(double strength, double directionX, double directionZ) {}
}