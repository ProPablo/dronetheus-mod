package com.kongi.dronetheus;

import net.minecraft.client.texture.NativeImage;
import org.spongepowered.asm.mixin.Unique;

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

public class ImageData {
    public int width;
    public int height;
    public int[] pixels;

    public ImageData(NativeImage screenshot) {
        width = screenshot.getWidth();
        height = screenshot.getHeight();
        pixels = screenshot.copyPixelsArgb();
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
    public byte[] convertToJpeg() throws IOException {

        // Initialize or reuse the BufferedImage
        if (reusableImage == null || reusableImage.getWidth() != width || reusableImage.getHeight() != height) {
            reusableImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        }

        // Get direct access to the raster data for faster pixel setting
        WritableRaster raster = reusableImage.getRaster();
        DataBufferInt dataBuffer = (DataBufferInt) raster.getDataBuffer();
        int[] imageData = dataBuffer.getData();

        // Get pixel array from NativeImage - using the copyPixelsARGB method directly

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
        return reusableBaos.toByteArray();
    }
}
