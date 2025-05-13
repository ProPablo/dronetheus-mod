package com.kongi.dronetheus;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Tracking {
    // PID controller constants
    private static final double Kp = 0.5; // Proportional gain
    private static final double Ki = 0.1; // Integral gain
    private static final double Kd = 0.2; // Derivative gain

    // Movement limits
    private static final double MAX_VELOCITY = 0.2; // Maximum velocity in blocks per tick
    private static final double MAX_INTEGRAL = 2.0; // Maximum integral term to prevent windup
    
    // PID controller state
    private Vec3d integralError = new Vec3d(0, 0, 0);
    private Vec3d lastError = new Vec3d(0, 0, 0);
    
    // Target offset from the player we're tracking (only X and Y)
    private Vec3d targetOffset = new Vec3d(2, 2, 0); // Example offset: 2 blocks right and up
    
    // Current positions
    public Vec3d trackingPlayerPos = null;
    private Vec3d currentPos;
    private Vec3d targetPos;

    public void TickUpdate(MinecraftClient client) {
        if (trackingPlayerPos == null) return;
        
        // Get current position
        currentPos = client.player.getPos();
        
        // Calculate target position based on tracking player and offset (only X and Y)
        targetPos = new Vec3d(
            trackingPlayerPos.x + targetOffset.x,
            trackingPlayerPos.y + targetOffset.y,
            trackingPlayerPos.z + targetOffset.z
        );
        
        // Calculate error (difference between current and target position)
        Vec3d error = targetPos.subtract(currentPos);
        
        // Update integral error with clamping
        Vec3d newIntegral = integralError.add(error.multiply(0.05)); // 0.05 is the time step
        integralError = new Vec3d(
            MathHelper.clamp(newIntegral.x, -MAX_INTEGRAL, MAX_INTEGRAL),
            MathHelper.clamp(newIntegral.y, -MAX_INTEGRAL, MAX_INTEGRAL),
            MathHelper.clamp(newIntegral.z, -MAX_INTEGRAL, MAX_INTEGRAL)
        );
        
        // Calculate derivative error
        Vec3d derivativeError = error.subtract(lastError).multiply(20); // Multiply by 20 to get rate of change per second
        
        // Calculate PID output
        Vec3d pidOutput = error.multiply(Kp)
                .add(integralError.multiply(Ki))
                .add(derivativeError.multiply(Kd));
        
        // Convert PID output to WASD movement
//        boolean moveForward = pidOutput.z > 0.1;
//        boolean moveBackward = pidOutput.z < -0.1;
//        boolean moveLeft = pidOutput.x < -0.1;
//        boolean moveRight = pidOutput.x > 0.1;
//        client.options.forwardKey.setPressed(moveForward);
//        client.options.backKey.setPressed(moveBackward);
//        client.options.leftKey.setPressed(moveLeft);
//        client.options.rightKey.setPressed(moveRight);


        // Set player velocity based on PID output
        if (client.player != null) {
            // Clamp velocity to maximum allowed value
            double speed = Math.sqrt(pidOutput.x * pidOutput.x + pidOutput.y * pidOutput.y + pidOutput.z * pidOutput.z);
            if (speed > MAX_VELOCITY) {
                pidOutput = pidOutput.multiply(MAX_VELOCITY / speed);
            }
            
            // Set the player's velocity
            client.player.setVelocity(pidOutput.x, pidOutput.y, pidOutput.z);
        }

        // Calculate and set camera angles to look at the other player
        updateCameraAngles(client);
        
        // Store current error for next tick
        lastError = error;
    }

    private void updateCameraAngles(MinecraftClient client) {
        if (client.player == null || trackingPlayerPos == null) return;

        // Get the direction vector from player to target
        Vec3d direction = trackingPlayerPos.subtract(client.player.getPos());
        
        // Calculate yaw (horizontal angle)
        double yaw = MathHelper.atan2(direction.z, direction.x) * 180.0D / Math.PI - 90.0D;
        
        // Calculate pitch (vertical angle)
        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        double pitch = -MathHelper.atan2(direction.y, horizontalDistance) * 180.0D / Math.PI;
        
        // Set the player's rotation
        client.player.setYaw((float) yaw);
        client.player.setPitch((float) pitch);
    }

    public void NewOtherPlayerPos(Vec3d playerPos) {
        trackingPlayerPos = playerPos;
    }
    
    // Method to set the target offset (only X and Y will be used)
    public void setTargetOffset(Vec3d offset) {
        this.targetOffset = new Vec3d(offset.x, offset.y, 0);
    }
}
