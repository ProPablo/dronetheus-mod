package com.kongi.dronetheus.mixin.client;

import com.kongi.dronetheus.DronetheusClient;

import com.kongi.dronetheus.ImageData;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    private static final AtomicInteger frameCounter = new AtomicInteger(0);
    private static final int FRAME_INTERVAL = 3; // Capture every 3 frames (20 FPS if game runs at 60 FPS)

    @Inject(
            method = "render",
            at = @At(value = "TAIL")
    )
    private void captureFrameAfterRender(CallbackInfo info) {
        // Only capture periodically to maintain performance
        if (DronetheusClient.isCapturing && frameCounter.incrementAndGet() >= FRAME_INTERVAL) {
            frameCounter.set(0);

            try {
                // Get client instance via MinecraftClient.getInstance()
                net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                Framebuffer framebuffer = client.getFramebuffer();

                // Use ScreenshotRecorder to take a screenshot of the current frame
                NativeImage screenshot = ScreenshotRecorder.takeScreenshot(framebuffer);

                // Convert to JPEG and add to queue in one step
                ImageData newData = new ImageData(screenshot);
                DronetheusClient.addFrameToQueue(newData);

                // We don't need this anymore since we've copied the data
                screenshot.close();
            } catch (Exception e) {
                DronetheusClient.LOGGER.error("Error capturing frame", e);
            }
        }
    }
}