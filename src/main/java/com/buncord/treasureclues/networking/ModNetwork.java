package com.buncord.treasureclues.networking;

import com.buncord.treasureclues.TreasureCluesMod;
import com.buncord.treasureclues.networking.packet.OpenClueServerToClientPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static SimpleChannel INSTANCE;

    private static final String VERSION = "1.0";

    private static int packetId = 0;

    private static int nextId() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel channel = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(TreasureCluesMod.MOD_ID, "main"))
                .networkProtocolVersion(() -> VERSION)
                .clientAcceptedVersions(VERSION::equals)
                .serverAcceptedVersions(VERSION::equals)
                .simpleChannel();

        INSTANCE = channel;

        channel.messageBuilder(OpenClueServerToClientPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenClueServerToClientPacket::new)
                .encoder(OpenClueServerToClientPacket::toBytes)
                .consumer(OpenClueServerToClientPacket::handle)
                .add();
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
