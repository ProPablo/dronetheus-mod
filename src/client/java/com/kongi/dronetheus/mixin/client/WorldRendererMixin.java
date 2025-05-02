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
                convertToJpegAndQueue(screenshot);

                // We don't need this anymore since we've copied the data
                screenshot.close();
            } catch (Exception e) {
                DronetheusClient.LOGGER.error("Error capturing frame", e);
            }
        }
    }

    // Reuse these objects across frames to avoid GC pressure
    @Unique
    private static BufferedImage reusableImage = null;
    @Unique
    private static ByteArrayOutputStream reusableBaos = new ByteArrayOutputStream(65536); // Pre-allocate 64KB
    @Unique
    private static final JPEGImageWriteParam jpegParams = setupJpegParams();

    @Unique
    private static JPEGImageWriteParam setupJpegParams() {
        JPEGImageWriteParam params = new JPEGImageWriteParam(null);
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        // Adjust quality/speed tradeoff (0.5f is a good balance, lower = faster)
        params.setCompressionQuality(0.5f);
        // Use fastest DCT method
        params.setOptimizeHuffmanTables(false);
        return params;
    }

    @Unique
    private void convertToJpegAndQueue(NativeImage screenshot) throws IOException {
        int width = screenshot.getWidth();
        int height = screenshot.getHeight();

        // Initialize or reuse the BufferedImage
        if (reusableImage == null || reusableImage.getWidth() != width || reusableImage.getHeight() != height) {
            reusableImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        }

        // Get direct access to the raster data for faster pixel setting
        WritableRaster raster = reusableImage.getRaster();
        DataBufferInt dataBuffer = (DataBufferInt) raster.getDataBuffer();
        int[] imageData = dataBuffer.getData();

        // Get pixel array from NativeImage - using the copyPixelsARGB method directly
        int[] pixels = screenshot.copyPixelsArgb();

        // Bulk operation to copy and convert ARGB to RGB - much faster than nested loops
        for (int i = 0; i < pixels.length; i++) {
            // Extract RGB components (ignore alpha)
            imageData[i] = pixels[i] & 0x00FFFFFF;
        }

        // Reset the ByteArrayOutputStream for reuse
        reusableBaos.reset();

        // Get a JPEGImageWriter directly instead of using ImageIO.write
        ImageWriter writer = DronetheusClient.ImageWriter;

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(reusableBaos)) {
            writer.setOutput(ios);
            // Use the optimized JPEG params
            writer.write(null, new IIOImage(reusableImage, null, null), jpegParams);
        }

        // Add to queue directly from the ByteArrayOutputStream to avoid copy
        DronetheusClient.addFrameToQueue(reusableBaos.toByteArray());
    }

}