package pl.peterwolf.apollo11.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import pl.peterwolf.apollo11.Apollo11;

public record FlightStartPayload() implements CustomPacketPayload {
    public static final Type<FlightStartPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Apollo11.MOD_ID, "flight_start"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlightStartPayload> CODEC =
            StreamCodec.unit(new FlightStartPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
