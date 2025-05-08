package com.kongi.dronetheus.mixin.client;

import com.kongi.dronetheus.DronetheusClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class HandleInputMixin
{
    @Shadow @Final public GameOptions options;

    @Shadow @Nullable public ClientPlayerEntity player;

    @Inject(at = @At("HEAD"), method = "tick")
    private void tick(CallbackInfo info)
    {
        // Only apply WASD controls if they are enabled
        if (DronetheusClient.isWASDEnabled) {
            options.forwardKey.setPressed(DronetheusClient.isMovingForward);
            options.backKey.setPressed(DronetheusClient.isMovingBackward);
            options.leftKey.setPressed(DronetheusClient.isMovingLeft);
            options.rightKey.setPressed(DronetheusClient.isMovingRight);
        }
    }

//    @Inject(at = @At("HEAD"), method = "handleInputEvents")
//    private void handleInputEvents(CallbackInfo info)
//    {
//        if (bind.isPressed() && lastPressedToggleKey <= 0) {
//            System.out.println("User has pressed the toggle key, " + (isafkon ? "disabling" : "enabling") + " afk fishing.");
//            isafkon = !isafkon;
//            lastPressedToggleKey += 30;
//        }
//    }
}
