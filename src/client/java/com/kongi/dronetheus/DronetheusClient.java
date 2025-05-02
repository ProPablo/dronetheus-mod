package com.kongi.dronetheus;

import io.undertow.server.HttpServerExchange;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.PlayerInput;
import org.lwjgl.glfw.GLFW;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.Headers;

import java.io.*;
import java.util.Iterator;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;

public class DronetheusClient implements ClientModInitializer {
    private static final String BOUNDARY = "frame";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final BlockingQueue<byte[]> FRAME_QUEUE = new LinkedBlockingQueue<>(5);
    private static Undertow server;
    public static boolean isCapturing = false;
    private static final int PORT = 8080;
    private static KeyBinding toggleKeybind;

    public static final String MOD_ID = "dronetheus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ImageWriter ImageWriter;


    private static KeyBinding walkKeybind;
    public static boolean WalkForward = false;


    @Override
    public void onInitializeClient() {
        LOGGER.info("Starting Screen Stream Mod");

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        assert writers.hasNext();

        ImageWriter = writers.next();

        // Register keybinding to toggle streaming
        toggleKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.screenstream.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                "category.screenstream.general"
        ));

        walkKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dronetheus.walk",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F9,
                "category.dronetheus.general"
        ));

        // Start web server
        startServer();

        // Set up screen capture on client tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Check for keybinding press
            while (toggleKeybind.wasPressed()) {
                isCapturing = !isCapturing;
                String status = isCapturing ? "enabled" : "disabled";
                if (client.player != null) {
                    client.player.sendMessage(
                            Text.literal("Screen streaming " + status)
                                    .formatted(isCapturing ? Formatting.GREEN : Formatting.RED),
                            false
                    );
                }
            }
        });


    }

    private void startServer() {
        try {
            HttpHandler streamHandler = exchange -> {
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "multipart/x-mixed-replace; boundary=" + BOUNDARY);
                exchange.getResponseHeaders().put(Headers.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
                exchange.getResponseHeaders().put(Headers.PRAGMA, "no-cache");
                exchange.getResponseHeaders().put(Headers.EXPIRES, "0");

                // Detach from the I/O thread and run on a worker thread instead
                if (exchange.isInIoThread()) {
                    exchange.dispatch(DronetheusClient::streamFrames);
                } else {
                    streamFrames(exchange);
                }
            };

            HttpHandler indexHandler = exchange -> {
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");

                String response = "<!DOCTYPE html>\n" +
                        "<html>\n" +
                        "<head>\n" +
                        "    <title>Minecraft Screen Stream</title>\n" +
                        "    <style>\n" +
                        "        body { font-family: Arial, sans-serif; text-align: center; background-color: #333; color: #fff; }\n" +
                        "        .container { max-width: 800px; margin: 0 auto; padding: 20px; }\n" +
                        "        img { max-width: 100%; border: 2px solid #555; }\n" +
                        "        .status { padding: 10px; margin-bottom: 20px; background-color: #444; border-radius: 5px; }\n" +
                        "    </style>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "    <div class=\"container\">\n" +
                        "        <h1>Minecraft Screen Stream</h1>\n" +
                        "        <div class=\"status\" id=\"status\">Status: Connecting...</div>\n" +
                        "        <img src=\"/stream\" alt=\"Minecraft Screen Stream\" id=\"stream\" />\n" +
                        "    </div>\n" +
                        "    <script>\n" +
                        "        document.getElementById('stream').onerror = function() {\n" +
                        "            document.getElementById('status').innerHTML = 'Status: No stream available. Make sure streaming is enabled in-game (press F8)';\n" +
                        "            setTimeout(function() {\n" +
                        "                document.getElementById('stream').src = '/stream?' + new Date().getTime();\n" +
                        "            }, 5000);\n" +
                        "        };\n" +
                        "        document.getElementById('stream').onload = function() {\n" +
                        "            document.getElementById('status').innerHTML = 'Status: Connected';\n" +
                        "        };\n" +
                        "    </script>\n" +
                        "</body>\n" +
                        "</html>";

                exchange.getResponseSender().send(response);
            };

            // API endpoint for status
            HttpHandler statusHandler = exchange -> {
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");

                String status = "{\"streaming\": " + isCapturing + ", \"queueSize\": " + FRAME_QUEUE.size() + "}";
                exchange.getResponseSender().send(status);
            };

            HttpHandler walkHandler = exchange -> {

//                exchange.getResponseSender().send();
            };

            PathHandler pathHandler = new PathHandler()
                    .addExactPath("/", indexHandler)
                    .addExactPath("/stream", streamHandler)
                    .addExactPath("/api/status", statusHandler);

            server = Undertow.builder()
                    .addHttpListener(PORT, "0.0.0.0")
                    .setHandler(pathHandler)
                    .build();

            server.start();
            System.out.println("Screen stream server started on port " + PORT);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static long lastTime = System.currentTimeMillis();
    static int fpsCount = 0;

    public static void CountFPS() {
        fpsCount++;
        long now = System.currentTimeMillis();
        long elapsed = now - lastTime;
        if (elapsed > 1000) {
            LOGGER.info("FPS: {}", fpsCount);
            fpsCount = 0;
            lastTime = now;
        }
    }

    private static void streamFrames(HttpServerExchange exchange) {
        LOGGER.info("Started client stream from {}", exchange.getRequestPath());
        exchange.startBlocking();
        try (OutputStream output = exchange.getOutputStream()) {
            while (!exchange.isComplete()) {
                try {
                    //Gives back null if timed out
                    byte[] frameData = FRAME_QUEUE.poll(1, TimeUnit.SECONDS);
                    if (frameData != null) {
                        CountFPS();

                        output.write(("--" + BOUNDARY + "\r\n").getBytes());
                        output.write("Content-Type: image/jpeg\r\n".getBytes());
                        output.write(("Content-Length: " + frameData.length + "\r\n\r\n").getBytes());
                        output.write(frameData);
                        output.write("\r\n".getBytes());
                        output.flush();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    // Connection likely closed by the client
                    break;
                }
            }
            LOGGER.info("Client {} disconnected", exchange.getRequestPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Additional cleanup methods for when the game closes
    public static void shutdown() {
        isCapturing = false;
        EXECUTOR.shutdown();
        if (server != null) {
            server.stop();
        }
    }

    /**
     * Method called by the mixin to add a frame to the queue
     */
    public static void addFrameToQueue(byte[] imageData) {
        if (isCapturing) {
            // If queue is full, remove oldest frame
            if (FRAME_QUEUE.remainingCapacity() == 0) {
                FRAME_QUEUE.poll();
            }

            if (!FRAME_QUEUE.offer(imageData)) {
                throw new IllegalStateException("Frame queue is full");
            }
        }
    }

}