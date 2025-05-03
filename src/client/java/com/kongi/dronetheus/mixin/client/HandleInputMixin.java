package com.kongi.dronetheus.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
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

    @Inject(at = @At("HEAD"), method = "tick")
    private void tick(CallbackInfo info)
    {
        options.forwardKey.setPressed(true);
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
