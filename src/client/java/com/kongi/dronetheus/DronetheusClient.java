package com.kongi.dronetheus;

import io.undertow.server.HttpServerExchange;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
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

import static com.kongi.dronetheus.Dronetheus.MOD_ID;


public class DronetheusClient implements ClientModInitializer {
    private static final String BOUNDARY = "frame";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final BlockingQueue<ImageData> FRAME_QUEUE = new LinkedBlockingQueue<>(5);
    private static Undertow server;
    public static boolean isCapturing = false;
    private static final int PORT = 8080;
    private static KeyBinding toggleKeybind;

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID + "client");

    private static KeyBinding walkKeybind;
    public static boolean WalkForward = false;

    // WASD control variables
    public static boolean isMovingForward = false;
    public static boolean isMovingBackward = false;
    public static boolean isMovingLeft = false;
    public static boolean isMovingRight = false;
    public static boolean isWASDEnabled = false;  // New toggle state

    @Override
    public void onInitializeClient() {
        LOGGER.info("Starting Screen Stream Mod");


        // Register keybinding to toggle streaming
        toggleKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dronetheus.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                "category.dronetheus.general"
        ));

        walkKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dronetheus.walk",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F9,
                "category.dronetheus.general"
        ));

        // Start web server
        startServer();

        // Register client-side packet receiver for position updates
        ClientPlayNetworking.registerGlobalReceiver(FireTruckPositionS2CPayload.ID, (payload, context) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                Vec3d position = payload.fireTruckLoc();
                client.player.sendMessage(
                    Text.literal("Received position update: " + 
                        String.format("%.2f, %.2f, %.2f", position.x, position.y, position.z))
                        .formatted(Formatting.GREEN),
                    false
                );
            }
        });

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

            // Check for WASD control toggle
            while (walkKeybind.wasPressed()) {
                isWASDEnabled = !isWASDEnabled;
                if (client.player != null) {
                    client.player.sendMessage(
                            Text.literal("WASD controls " + (isWASDEnabled ? "enabled" : "disabled"))
                                    .formatted(isWASDEnabled ? Formatting.GREEN : Formatting.RED),
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

                try {
                    InputStream inputStream = getClass().getResourceAsStream("/assets/dronetheus/index.html");
                    if (inputStream == null) {
                        exchange.setStatusCode(404);
                        exchange.getResponseSender().send("404 - Index file not found");
                        return;
                    }

                    LOGGER.info("Sent index.html");
                    String response = new String(inputStream.readAllBytes());
                    exchange.getResponseSender().send(response);
                } catch (IOException e) {
                    LOGGER.error("Failed to read index.html", e);
                    exchange.setStatusCode(500);
                    exchange.getResponseSender().send("500 - Internal Server Error");
                }
            };

            // API endpoint for status
            HttpHandler statusHandler = exchange -> {
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");

                String status = String.format(
                    "{\"streaming\": %b, \"queueSize\": %d, \"wasdEnabled\": %b}",
                    isCapturing,
                    FRAME_QUEUE.size(),
                    isWASDEnabled
                );
                exchange.getResponseSender().send(status);
            };

            HttpHandler controlsHandler = exchange -> {
                if (exchange.getRequestMethod().toString().equals("POST")) {
                    exchange.getRequestReceiver().receiveFullString((ex, message) -> {
                        try {
                            // Parse the JSON message
                            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(message).getAsJsonObject();
                            
                            // Update key states
                            isMovingForward = json.get("w").getAsBoolean();
                            isMovingBackward = json.get("s").getAsBoolean();
                            isMovingLeft = json.get("a").getAsBoolean();
                            isMovingRight = json.get("d").getAsBoolean();
                            
                            ex.setStatusCode(200);
                            ex.getResponseSender().send("OK");
                        } catch (Exception e) {
                            LOGGER.error("Error processing key states", e);
                            ex.setStatusCode(400);
                            ex.getResponseSender().send("Invalid key states format");
                        }
                    });
                } else {
                    exchange.setStatusCode(405);
                    exchange.getResponseSender().send("Method not allowed");
                }
            };

            PathHandler pathHandler = new PathHandler()
                    .addExactPath("/", indexHandler)
                    .addExactPath("/stream", streamHandler)
                    .addExactPath("/api/status", statusHandler)
                    .addExactPath("/api/controls", controlsHandler);

            server = Undertow.builder()
                    .addHttpListener(PORT, "0.0.0.0")
                    .setHandler(pathHandler)
                    .build();

            server.start();
            System.out.println("Screen stream server started on port " + PORT);

        } catch (Exception e) {
            LOGGER.info("I AM THE SECOND PLAYER");
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

        ImageWriter imageWriter;
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        assert writers.hasNext();
        imageWriter = writers.next();

        try (OutputStream output = exchange.getOutputStream()) {
            while (!exchange.isComplete()) {
                try {
                    //Gives back null if timed out
                    ImageData imageData = FRAME_QUEUE.poll(1, TimeUnit.SECONDS);
                    if (imageData != null) {
                        CountFPS();
                        byte[] frameData = imageData.convertToJpeg(imageWriter);
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
    public static void addFrameToQueue(ImageData imageData) {
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