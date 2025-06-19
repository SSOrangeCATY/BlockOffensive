package com.phasetranscrystal.blockoffensive.net;

import com.phasetranscrystal.blockoffensive.client.data.CSClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CSGameSettingsS2CPacket {
    private final int cTWinnerRounds;
    private final int tWinnerRounds;
    private final int time;
    private final boolean isDebug;
    private final boolean isStart;
    private final boolean isError;
    private final boolean isPause;
    private final boolean isWaiting;
    private final boolean isWaitingWinner;

    public CSGameSettingsS2CPacket(int cTWinnerRounds,
                                   int tWinnerRounds,
                                   int time,
                                   boolean isDebug,
                                   boolean isStart,
                                   boolean isError,
                                   boolean isPause,
                                   boolean isWaiting,
                                   boolean isWaitingWinner) {
        this.cTWinnerRounds = cTWinnerRounds;
        this.tWinnerRounds = tWinnerRounds;
        this.time = time;
        this.isDebug = isDebug;
        this.isStart = isStart;
        this.isError = isError;
        this.isPause = isPause;
        this.isWaiting = isWaiting;
        this.isWaitingWinner = isWaitingWinner;
    }

    public static void encode(CSGameSettingsS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.cTWinnerRounds);
        buf.writeInt(packet.tWinnerRounds);
        buf.writeInt(packet.time);
        buf.writeBoolean(packet.isDebug);
        buf.writeBoolean(packet.isStart);
        buf.writeBoolean(packet.isError);
        buf.writeBoolean(packet.isPause);
        buf.writeBoolean(packet.isWaiting);
        buf.writeBoolean(packet.isWaitingWinner);
    }

    public static CSGameSettingsS2CPacket decode(FriendlyByteBuf buf) {
        return new CSGameSettingsS2CPacket(
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            CSClientData.cTWinnerRounds = this.cTWinnerRounds;
            CSClientData.tWinnerRounds = this.tWinnerRounds;
            CSClientData.time = this.time;
            CSClientData.isDebug = this.isDebug;
            CSClientData.isStart = this.isStart;
            CSClientData.isError = this.isError;
            CSClientData.isPause = this.isPause;
            CSClientData.isWaiting = this.isWaiting;
            CSClientData.isWaitingWinner = this.isWaitingWinner;
        });
        ctx.get().setPacketHandled(true);
    }
}