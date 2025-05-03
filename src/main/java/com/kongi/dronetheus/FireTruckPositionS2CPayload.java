package com.kongi.dronetheus;//https://docs.fabricmc.net/develop/networking


//public record SummonLightningS2CPayload(BlockPos pos) implements CustomPayload {
//    public static final Identifier SUMMON_LIGHTNING_PAYLOAD_ID = Identifier.of(FabricDocsReference.MOD_ID, "summon_lightning");
//    public static final CustomPayload.Id<SummonLightningS2CPayload> ID = new CustomPayload.Id<>(SUMMON_LIGHTNING_PAYLOAD_ID);
//    public static final PacketCodec<RegistryByteBuf, SummonLightningS2CPayload> CODEC = PacketCodec.tuple(BlockPos.PACKET_CODEC, SummonLightningS2CPayload::pos, SummonLightningS2CPayload::new);
//
//    @Override
//    public Id<? extends CustomPayload> getId() {
//        return ID;
//    }
//}

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

// A record sets all the ctor params as parameters
public record FireTruckPositionS2CPayload(Vec3d fireTruckLoc) implements CustomPayload {

    public static final Identifier SUMMON_LIGHTNING_PAYLOAD_ID = Identifier.of(Dronetheus.MOD_ID, "firetruck_position");
    public static final CustomPayload.Id<FireTruckPositionS2CPayload> ID = new CustomPayload.Id<>(SUMMON_LIGHTNING_PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, FireTruckPositionS2CPayload> CODEC = PacketCodec.tuple(Vec3d.PACKET_CODEC, FireTruckPositionS2CPayload::fireTruckLoc, FireTruckPositionS2CPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
