package com.xuebi1145.xuplus_client.integrity;

import com.xuebi1145.xuplus_client.XUPlusClient;
import com.xuebi1145.xuplus_client.hud.BombItemS2CPacket;
import com.xuebi1145.xuplus_client.hud.BombTimerS2CPacket;
import com.xuebi1145.xuplus_client.hud.WeaponItemIdS2CPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 完整性校验网络通道
 */
public class IntegrityNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(XUPlusClient.MODID, "integrity"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static final AtomicInteger ID = new AtomicInteger(0);

    public static void register() {
        // C2S: 模组列表报告（加入后立即发送）
        CHANNEL.registerMessage(
            ID.getAndIncrement(),
            ModListReportC2SPacket.class,
            ModListReportC2SPacket::encode,
            ModListReportC2SPacket::new,
            ModListReportC2SPacket::handle
        );
        // C2S: 周期性完整性报告
        CHANNEL.registerMessage(
            ID.getAndIncrement(),
            IntegrityReportC2SPacket.class,
            IntegrityReportC2SPacket::encode,
            IntegrityReportC2SPacket::new,
            IntegrityReportC2SPacket::handle
        );
        // S2C: 握手包（告知客户端服务端已安装）
        CHANNEL.registerMessage(
            ID.getAndIncrement(),
            IntegrityHandshakeS2CPacket.class,
            IntegrityHandshakeS2CPacket::encode,
            IntegrityHandshakeS2CPacket::new,
            IntegrityHandshakeS2CPacket::handle
        );
        CHANNEL.registerMessage(
            ID.getAndIncrement(),
            WeaponItemIdS2CPacket.class,
            WeaponItemIdS2CPacket::encode,
            WeaponItemIdS2CPacket::decode,
            WeaponItemIdS2CPacket::handle
        );
        CHANNEL.registerMessage(
            ID.getAndIncrement(),
            BombItemS2CPacket.class,
            BombItemS2CPacket::encode,
            BombItemS2CPacket::decode,
            BombItemS2CPacket::handle
        );
        CHANNEL.registerMessage(
            ID.getAndIncrement(),
            BombTimerS2CPacket.class,
            BombTimerS2CPacket::encode,
            BombTimerS2CPacket::decode,
            BombTimerS2CPacket::handle
        );
    }

    /**
     * 客户端发送完整性报告到服务端
     */
    public static void sendToServer(IntegrityReportC2SPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    /**
     * 客户端发送模组列表报告到服务端
     */
    public static void sendToServer(ModListReportC2SPacket packet) {
        CHANNEL.sendToServer(packet);
    }
}
