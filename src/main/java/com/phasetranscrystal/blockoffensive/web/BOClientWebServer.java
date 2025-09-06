package com.phasetranscrystal.blockoffensive.web;

import com.google.gson.Gson;
import com.mojang.datafixers.util.Pair;
import com.phasetranscrystal.blockoffensive.BOConfig;
import com.phasetranscrystal.blockoffensive.client.data.CSClientData;
import com.phasetranscrystal.blockoffensive.client.data.WeaponData;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.common.client.data.FPSMClientGlobalData;
import com.phasetranscrystal.fpsmatch.core.data.PlayerData;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BOClientWebServer {
    private static HttpServer server;
    private static final Gson gson = new Gson();

    //TODO Server side logic

    public static void start() {
        // 检查端口是否被占用
        if (server != null) return;

        int port = BOConfig.common.webServerPort.get();
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/api/data", new CSDataHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("BO Web Server started on port " + port);
        } catch (IOException e) {
            System.out.println("BO Web Server failed to start on port " + port + " " + e.getMessage());
            server = null;
        }
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            System.out.println("FPSM Web Server stopped");
        }
    }

    static class CSDataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            FPSMClientGlobalData globalData = FPSMClient.getGlobalData();

            Map<String, Object> response = new HashMap<>();

            response.put("currentMapSupportShop", CSClientData.currentMapSupportShop);
            response.put("cTWinnerRounds", CSClientData.cTWinnerRounds);
            response.put("tWinnerRounds", CSClientData.tWinnerRounds);
            response.put("time", CSClientData.time);
            response.put("isDebug", CSClientData.isDebug);
            response.put("isStart", CSClientData.isStart);
            response.put("isError", CSClientData.isError);
            response.put("isPause", CSClientData.isPause);
            response.put("isWaiting", CSClientData.isWaiting);
            response.put("isWarmTime", CSClientData.isWarmTime);
            response.put("isWaitingWinner", CSClientData.isWaitingWinner);
            response.put("canOpenShop", CSClientData.canOpenShop);
            response.put("shopCloseTime", CSClientData.shopCloseTime);
            response.put("nextRoundMoney", CSClientData.nextRoundMoney);
            response.put("dismantleBombProgress", CSClientData.dismantleBombProgress);

            Map<String, Object> tabData = new HashMap<>();
            for (UUID uuid : globalData.tabData.keySet()) {
                Pair<String, PlayerData> pair = globalData.tabData.get(uuid);
                String team = pair.getFirst();
                if(team.equals("spectator")) continue;
                PlayerData data = pair.getSecond();
                Map<String, Object> playerData = new HashMap<>();
                playerData.put("name", data.name().getString());
                playerData.put("team", team);
                playerData.put("data", data.mappedInfo());
                playerData.put("living", data.isLivingNoOnlineCheck());
                playerData.put("money", globalData.getPlayerMoney(uuid));
                WeaponData weaponData = CSClientData.getWeaponData(uuid);
                playerData.put("items", weaponData.weaponData());
                playerData.put("bpAttributeHasHelmet", weaponData.bpAttributeHasHelmet());
                playerData.put("bpAttributeDurability", weaponData.bpAttributeDurability());
                tabData.put(uuid.toString(), playerData);
            }
            response.put("tabData", tabData);

            sendResponse(exchange, response);
        }
    }

    private static void sendResponse(HttpExchange exchange, Map<String, Object> response) throws IOException {
        String jsonResponse = gson.toJson(response);
        byte[] responseBytes = jsonResponse.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

}