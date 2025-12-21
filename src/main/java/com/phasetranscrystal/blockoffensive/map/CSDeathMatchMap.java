package com.phasetranscrystal.blockoffensive.map;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.phasetranscrystal.blockoffensive.BlockOffensive;
import com.phasetranscrystal.fpsmatch.common.capability.map.GameEndTeleportCapability;
import com.phasetranscrystal.fpsmatch.common.capability.team.ShopCapability;
import com.phasetranscrystal.fpsmatch.common.capability.team.StartKitsCapability;
import com.phasetranscrystal.fpsmatch.common.capability.team.SpawnPointCapability;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import com.phasetranscrystal.fpsmatch.core.capability.CapabilityMap;
import com.phasetranscrystal.fpsmatch.core.capability.map.MapCapability;
import com.phasetranscrystal.fpsmatch.core.capability.team.TeamCapability;
import com.phasetranscrystal.fpsmatch.core.data.AreaData;
import com.phasetranscrystal.fpsmatch.core.data.PlayerData;
import com.phasetranscrystal.fpsmatch.core.data.Setting;
import com.phasetranscrystal.fpsmatch.core.data.SpawnPointData;
import com.phasetranscrystal.fpsmatch.core.persistence.FPSMDataManager;
import com.phasetranscrystal.fpsmatch.core.team.MapTeams;
import com.phasetranscrystal.fpsmatch.core.team.ServerTeam;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Team;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = BlockOffensive.MODID,bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CSDeathMatchMap extends CSMap {
    /**
     * Codec序列化配置（用于地图数据保存/加载）
     */
    public static final Codec<CSDeathMatchMap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            // 基础地图数据
            Codec.STRING.fieldOf("mapName").forGetter(CSDeathMatchMap::getMapName),
            AreaData.CODEC.fieldOf("mapArea").forGetter(CSDeathMatchMap::getMapArea),
            ResourceLocation.CODEC.fieldOf("serverLevel").forGetter(map -> map.getServerLevel().dimension().location()),
            CapabilityMap.Wrapper.DATA_CODEC.fieldOf("capabilities").forGetter(csGameMap -> csGameMap.getCapabilityMap().getData().data()),
            // 队伍数据
            Codec.unboundedMap(
                    Codec.STRING,
                    CapabilityMap.Wrapper.CODEC
            ).fieldOf("teams").forGetter(csGameMap -> csGameMap.getMapTeams().getData())
    ).apply(instance, CSDeathMatchMap::new));

    public static final String TYPE = "csdm";
    private static final List<Class<? extends MapCapability>> MAP_CAPABILITIES = List.of(GameEndTeleportCapability.class);
    private static final List<Class<? extends TeamCapability>> TEAM_CAPABILITIES = List.of(ShopCapability.class, StartKitsCapability.class, SpawnPointCapability.class);
    
    // 死斗模式设置
    private final Setting<Boolean> isTDM = this.addSetting("isTDM", false);
    private final Setting<Integer> matchTimeLimit = this.addSetting("matchTimeLimit", 18000); // 15 minutes in ticks
    private final Setting<Integer> spawnProtectionTime = this.addSetting("spawnProtectionTime", 100);
    
    // 游戏状态
    private int currentMatchTime = 0;

    // 重生保护映射
    private final Map<UUID, DMPlayerData> playerData = new HashMap<>();
    private final List<SpawnPointData> spawnPoints = new ArrayList<>();

    private boolean isError = false;

    /**
     * 构造函数：创建CS死斗地图实例
     * @param serverLevel 服务器世界实例
     * @param mapName 地图名称
     * @param areaData 地图区域数据
     */
    public CSDeathMatchMap(ServerLevel serverLevel, String mapName, AreaData areaData) {
        super(serverLevel, mapName, areaData, MAP_CAPABILITIES, TEAM_CAPABILITIES);
    }

    private CSDeathMatchMap(String mapName, AreaData areaData, ResourceLocation serverLevel, Map<String, JsonElement> capabilities, Map<String, CapabilityMap.Wrapper> teams) {
        this(FPSMCore.getInstance().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,serverLevel)), mapName, areaData);
        this.getCapabilityMap().write(capabilities);
        this.getMapTeams().writeData(teams);
    }

    @Override
    public void syncToClient() {
        super.syncToClient(false);
    }

    @Override
    public void setup() {

    }
    
    @Override
    public String getGameType() {
        return TYPE;
    }

    @Override
    public void join(String teamName, ServerPlayer player){
        super.join(teamName,player);
        getMapTeams().getTeamByPlayer(player).ifPresent(team -> {
            this.playerData.put(player.getUUID(), new DMPlayerData(player.getUUID()));
            if(isStart){
                respawnPlayer(player);
            }
        });
    }
    
    @Override
    public boolean start() {
        if (!super.start()) {
            return false;
        }
        
        MapTeams mapTeams = getMapTeams();

        this.isStart = true;
        this.currentMatchTime = 0;
        this.resetAllPlayerData();
        
        if (!this.setTeamSpawnPoints()) {
            this.reset();
            return false;
        }
        
        cleanupMap();
        mapTeams.startNewRound();
        mapTeams.resetLivingPlayers();

        initializePlayers(mapTeams);
        
        return true;
    }

    public void resetAllPlayerData(){
        this.playerData.values().forEach(DMPlayerData::reset);
    }

    private void initializePlayers(MapTeams mapTeams) {
        mapTeams.getJoinedPlayersMap().forEach(this::initializePlayer);
    }
    
    private void initializePlayer(ServerTeam team,List<PlayerData> players) {
        team.resetCapabilities();

        players.forEach(data -> {
            data.reset();
            data.getPlayer().ifPresent(player -> {
                player.removeAllEffects();
                player.heal(player.getMaxHealth());
                player.setGameMode(GameType.ADVENTURE);
                this.clearInventory(player);
                this.givePlayerKits(player);
        });
    });
    }

    @Override
    public void tick() {
        super.tick();
        
        if (isStart) {
            updateMatchTime();
        }
    }
    
    private void updateMatchTime() {
        this.currentMatchTime++;
    }
    
    @Override
    public boolean victoryGoal() {
        // 死斗模式胜利条件：时间结束
        return currentMatchTime >= matchTimeLimit.get();
    }
    
    @Override
    public void handleDeath(ServerPlayer dead) {
        // 立即重生玩家
        respawnPlayer(dead);
    }
    
    private void respawnPlayer(ServerPlayer player) {
        // 重置玩家状态
        player.heal(player.getMaxHealth());
        player.removeAllEffects();
        player.setGameMode(GameType.ADVENTURE);
        
        // 随机选择重生点
        SpawnPointData spawnPoint = getRandomSpawnPoint();
        if (spawnPoint != null) {
            teleportToPoint(player, spawnPoint);
        }

        givePlayerKits(player);
        
        // 给予重生保护
        getDMPlayerData(player.getUUID()).ifPresent(DMPlayerData::respawn);
    }
    
    private SpawnPointData getRandomSpawnPoint() {
        if (spawnPoints.isEmpty()) {
            return null;
        }
        
        // 计算附近玩家数量，选择安全的重生点
        Map<SpawnPointData, Double> weightMap = new HashMap<>();
        for (SpawnPointData spawnPoint : spawnPoints) {
            double weight = calculateSpawnPointWeight(spawnPoint);
            weightMap.put(spawnPoint, weight);
        }
        
        // 基于权重选择重生点
        return selectWeightedRandomSpawnPoint(weightMap);
    }
    
    private double calculateSpawnPointWeight(SpawnPointData spawnPoint) {
        double baseWeight = 1.0;
        double playerDistanceFactor = 10.0;

        List<ServerPlayer> onlinePlayers = this.getMapTeams().getOnline();
        
        for (Player player : onlinePlayers) {
            if (player.isSpectator() || player.isDeadOrDying()) {
                continue;
            }
            
            double distance = player.distanceToSqr(spawnPoint.getPosition().getX(), spawnPoint.getPosition().getY(), spawnPoint.getPosition().getZ());

            baseWeight += playerDistanceFactor / (distance + 1.0);
        }
        
        return baseWeight;
    }
    
    private SpawnPointData selectWeightedRandomSpawnPoint(Map<SpawnPointData, Double> weightMap) {
        double totalWeight = weightMap.values().stream().mapToDouble(Double::doubleValue).sum();
        double randomValue = new Random().nextDouble() * totalWeight;
        
        double currentWeight = 0.0;
        for (Map.Entry<SpawnPointData, Double> entry : weightMap.entrySet()) {
            currentWeight += entry.getValue();
            if (currentWeight >= randomValue) {
                return entry.getKey();
            }
        }

        return weightMap.keySet().iterator().next();
    }
    
    @Override
    public void onPlayerDeathEvent(ServerPlayer player, DamageSource source) {
        super.onPlayerDeathEvent(player, source);
        
        // 立即重生
        respawnPlayer(player);
    }
    
    @Override
    public void givePlayerKits(ServerPlayer player) {
        CapabilityMap.getTeamCapability(this, StartKitsCapability.class)
                .forEach((team, opt) -> {
                    if (team.hasPlayer(player.getUUID())) {
                        opt.ifPresent(cap -> cap.givePlayerKits(player));
                    }
                });    }
    
    @Override
    public Team.Visibility nameTagVisibility() {
        // 死斗模式下，显示所有玩家的名称标签
        return Team.Visibility.ALWAYS;
    }
    
    @Override
    public boolean isError() {
        return this.isError;
    }
    
    @Override
    public boolean isPause() {
        return false;
    }
    
    @Override
    public boolean isWaiting() {
        return false;
    }
    
    @Override
    public boolean isWaitingWinner() {
        return false;
    }
    
    @Override
    public boolean canGiveEconomy() {
        return false;
    }
    
    @Override
    public int getClientTime() {
        return this.currentMatchTime / 20; // 返回秒数
    }
    
    @Override
    public boolean setTeamSpawnPoints() {
        spawnPoints.clear();
        for (ServerTeam team : this.getMapTeams().getNormalTeams()) {
            Optional<SpawnPointCapability> spawnCapOpt = team.getCapabilityMap().get(SpawnPointCapability.class);
            spawnCapOpt.ifPresent(cap -> {
                spawnPoints.addAll(cap.getSpawnPointsData());
            });
        }

        if(spawnPoints.isEmpty()) return false;

        for (ServerTeam team : this.getMapTeams().getNormalTeams()) {
            for (ServerPlayer player : team.getOnline()) {
                SpawnPointData spawnPoint = getRandomSpawnPoint();
                if (spawnPoint != null) {
                    team.getPlayerData(player.getUUID()).ifPresent(playerData -> playerData.setSpawnPointsData(spawnPoint));
                    teleportToPoint(player, spawnPoint);
                }else{
                    return false;
                }
            }
        }
        return true;
    }
    
    @Override
    public Function<ServerPlayer, Boolean> getPlayerCanOpenShop() {
        return player -> getDMPlayerData(player.getUUID()).map(DMPlayerData::canOpenShop).orElse(false);
    }
    
    @Override
    public int getNextRoundMinMoney(ServerTeam team) {
        return 16000; // 死斗模式下，总是给足钱
    }
    
    @Override
    public int getShopCloseTime() {
        // 死斗模式下，商店关闭时间为0，因为玩家可以随时购买
        return 0;
    }
    
    @Override
    public void startNewRound() {
        // 死斗模式下，新回合直接开始
        this.start();
    }

    public Optional<DMPlayerData> getDMPlayerData(UUID uuid){
        return Optional.ofNullable(playerData.getOrDefault(uuid, null));
    }
    
    /**
     * 检查玩家是否处于重生保护状态
     */
    public boolean isInSpawnProtection(UUID uuid) {
        return this.getDMPlayerData(uuid).map(d -> System.currentTimeMillis() - d.lastProtectionTime < (spawnProtectionTime.get() * 1000L)).orElse(false);
    }
    
    /**
     * 处理玩家开枪事件，取消重生保护
     */
    public void handlePlayerFire(UUID uuid) {
        this.getDMPlayerData(uuid).ifPresent(DMPlayerData::setFired);
    }
    
    /**
     * 处理玩家移动事件，取消重生保护
     */
    public void handlePlayerMove(UUID uuid) {
        this.getDMPlayerData(uuid).ifPresent(DMPlayerData::setMoved);
    }
    
    /**
     * 写入地图数据到数据管理器
     */
    public static void write(FPSMDataManager manager) {
        FPSMCore.getInstance().getMapByClass(CSDeathMatchMap.class)
                .forEach((map -> {
                    map.saveConfig();
                    manager.saveData(map, map.getMapName(), false);
                }));
    }

    @Override
    public CSDeathMatchMap getMap() {
        return this;
    }

     public static class DMPlayerData{
        UUID owner;
        boolean needRespawnProtection = false;
        long lastProtectionTime = 0;

        boolean isMoved = false;
        boolean isFired = false;

        private DMPlayerData(UUID owner){
            this.owner = owner;
        }

        public boolean canOpenShop(){
            return !isFired && !isMoved;
        }

        public void setFired(){
            isFired = true;
        }

        public void setMoved(){
            isMoved = true;
        }

        public void reset(){
            isFired = false;
            isMoved = false;
            needRespawnProtection = false;
            lastProtectionTime = 0;
        }

        public void respawn(){
            reset();
            needRespawnProtection = true;
            lastProtectionTime = System.currentTimeMillis();
        }
    }
}