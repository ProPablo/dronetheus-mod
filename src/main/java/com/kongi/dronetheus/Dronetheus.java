package com.kongi.dronetheus;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Dronetheus implements ModInitializer {
	public static final String MOD_ID = "dronetheus";

	public static final Logger LOGGER = LoggerFactory.getLogger("player_position_mod");

	// Define a unique identifier for our packet
	public static final Identifier POSITION_PACKET_ID = Identifier.of("mymod", "position_packet");

	// Store connected players on the server side
	private static final List<UUID> connectedPlayers = new ArrayList<>();

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Initializing Player Position Networking Mod - Server");

		// Register event for when players connect
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			LOGGER.info("Player joined: " + player.getName().getString());

			// Add player to our list
			connectedPlayers.add(player.getUuid());

			// If this is the second player, send their position to the first player
			if (connectedPlayers.size() == 2) {
				ServerPlayerEntity secondPlayer = player;

				// Get the first player
				ServerPlayerEntity firstPlayer = null;
				for (ServerPlayerEntity serverPlayer : PlayerLookup.all(server)) {
					if (serverPlayer.getUuid().equals(connectedPlayers.get(0))) {
						firstPlayer = serverPlayer;
						break;
					}
				}

				if (firstPlayer != null) {
					// Send second player's position to first player
//					sendPositionToPlayer(secondPlayer, firstPlayer);

					// Inform both players
					firstPlayer.sendMessage(Text.literal("Received position of player: " + secondPlayer.getName().getString()), false);
					secondPlayer.sendMessage(Text.literal("Your position was sent to: " + firstPlayer.getName().getString()), false);
				}
			}
		});

		// Register event for when players disconnect
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			LOGGER.info("Player left: " + player.getName().getString());
			connectedPlayers.remove(player.getUuid());
		});
	}
}