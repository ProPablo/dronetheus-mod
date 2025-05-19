package com.kongi.dronetheus;

public class WindManager {
    private static WindManager instance;
    private WindParameters currentWind;

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

    public void updateWind(double strength, double directionX, double directionZ) {
        // Normalize direction vector
        double length = Math.sqrt(directionX * directionX + directionZ * directionZ);
        if (length > 0) {
            directionX /= length;
            directionZ /= length;
        }
        
        // Clamp wind strength between 0 and 1
        strength = Math.max(0, Math.min(1, strength));
        
        currentWind = new WindParameters(strength, directionX, directionZ);
    }

    public WindParameters getWindParameters() {
        return currentWind;
    }

    // Record to store wind parameters
    public record WindParameters(double strength, double directionX, double directionZ) {}
}