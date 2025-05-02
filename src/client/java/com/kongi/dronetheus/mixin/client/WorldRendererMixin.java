package com.kongi.dronetheus.mixin.client;

import com.kongi.dronetheus.DronetheusClient;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
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
                // This is a bit of a hack but works in a mixin context
                net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                Framebuffer framebuffer = client.getFramebuffer();

                // Use ScreenshotRecorder to take a screenshot of the current frame
                NativeImage screenshot = ScreenshotRecorder.takeScreenshot(framebuffer);

                // Get pixel array from NativeImage
                // TODO: optimize to do all this in 1 function
                int[] pixels = screenshot.copyPixelsArgb();

                int width = screenshot.getWidth();
                int height = screenshot.getHeight();

                byte[] jpegBytes = convertToJpeg(pixels, width, height);
                //This might be inefficient since it might be doing a bunch of copies of the byte array
                DronetheusClient.addFrameToQueue(jpegBytes);

                // We don't need this anymore since we've copied the data
                screenshot.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Unique
    private byte[] convertToJpeg(int[] pixels, int width, int height) throws IOException {

        // Create a BufferedImage with TYPE_INT_ARGB
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Convert ARGB pixels to RGB (remove alpha)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = pixels[y * width + x];
                // Extract RGB components and ignore alpha
                int rgb = argb & 0x00FFFFFF;
                image.setRGB(x, y, rgb);
            }
        }

        // Set the pixels to the BufferedImage
//        image.setRGB(0, 0, width, height, pixels, 0, width);

        // Create a ByteArrayOutputStream to hold the JPEG data
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ImageWriter writer = DronetheusClient.ImageWriter;

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(image);
        }

//        // Write the image as JPEG to the output stream
//        ImageIO.write(image, "jpg", baos);
        // Convert the output stream to a byte array

        return baos.toByteArray();
    }
}