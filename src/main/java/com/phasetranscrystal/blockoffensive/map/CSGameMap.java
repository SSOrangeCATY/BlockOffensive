package com.phasetranscrystal.blockoffensive.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.phasetranscrystal.blockoffensive.BOConfig;
import com.phasetranscrystal.blockoffensive.BlockOffensive;
import com.phasetranscrystal.blockoffensive.client.data.WeaponData;
import com.phasetranscrystal.blockoffensive.compat.BOImpl;
import com.phasetranscrystal.blockoffensive.data.DeathMessage;
import com.phasetranscrystal.blockoffensive.data.MvpReason;
import com.phasetranscrystal.blockoffensive.entity.CompositionC4Entity;
import com.phasetranscrystal.blockoffensive.event.CSGamePlayerGetMvpEvent;
import com.phasetranscrystal.blockoffensive.event.CSGamePlayerJoinEvent;
import com.phasetranscrystal.blockoffensive.event.CSGameRoundEndEvent;
import com.phasetranscrystal.blockoffensive.item.BOItemRegister;
import com.phasetranscrystal.blockoffensive.item.BombDisposalKit;
import com.phasetranscrystal.blockoffensive.item.CompositionC4;
import com.phasetranscrystal.blockoffensive.net.*;
import com.phasetranscrystal.blockoffensive.net.bomb.BombDemolitionProgressS2CPacket;
import com.phasetranscrystal.blockoffensive.net.mvp.MvpHUDCloseS2CPacket;
import com.phasetranscrystal.blockoffensive.net.mvp.MvpMessageS2CPacket;
import com.phasetranscrystal.blockoffensive.net.shop.ShopStatesS2CPacket;
import com.phasetranscrystal.blockoffensive.net.spec.BombFuseS2CPacket;
import com.phasetranscrystal.blockoffensive.net.spec.CSGameWeaponDataS2CPacket;
import com.phasetranscrystal.blockoffensive.sound.BOSoundRegister;
import com.phasetranscrystal.blockoffensive.sound.MVPMusicManager;
import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.common.attributes.ammo.BulletproofArmorAttribute;
import com.phasetranscrystal.fpsmatch.common.capability.map.DemolitionModeCapability;
import com.phasetranscrystal.fpsmatch.common.capability.map.GameEndTeleportCapability;
import com.phasetranscrystal.fpsmatch.common.capability.team.*;
import com.phasetranscrystal.fpsmatch.common.entity.drop.DropType;
import com.phasetranscrystal.fpsmatch.common.entity.drop.MatchDropEntity;
import com.phasetranscrystal.fpsmatch.common.packet.*;
import com.phasetranscrystal.fpsmatch.compat.CounterStrikeGrenadesCompat;
import com.phasetranscrystal.fpsmatch.compat.LrtacticalCompat;
import com.phasetranscrystal.fpsmatch.compat.impl.FPSMImpl;
import com.phasetranscrystal.fpsmatch.core.*;
import com.phasetranscrystal.fpsmatch.core.capability.CapabilityMap;
import com.phasetranscrystal.fpsmatch.core.capability.map.MapCapability;
import com.phasetranscrystal.fpsmatch.core.capability.team.TeamCapability;
import com.phasetranscrystal.fpsmatch.core.data.AreaData;
import com.phasetranscrystal.fpsmatch.core.data.PlayerData;
import com.phasetranscrystal.fpsmatch.core.data.SpawnPointData;
import com.phasetranscrystal.fpsmatch.core.data.Setting;
import com.phasetranscrystal.fpsmatch.core.entity.BlastBombEntity;
import com.phasetranscrystal.fpsmatch.core.event.FPSMapEvent;
import com.phasetranscrystal.fpsmatch.core.map.*;
import com.phasetranscrystal.fpsmatch.core.persistence.FPSMDataManager;
import com.phasetranscrystal.fpsmatch.core.shop.FPSMShop;
import com.phasetranscrystal.fpsmatch.core.shop.ShopData;
import com.phasetranscrystal.fpsmatch.core.team.BaseTeam;
import com.phasetranscrystal.fpsmatch.core.team.ServerTeam;
import com.phasetranscrystal.fpsmatch.core.team.MapTeams;
import com.phasetranscrystal.fpsmatch.core.team.TeamData;
import com.phasetranscrystal.fpsmatch.util.FPSMUtil;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * 反恐精英（CS）模式地图核心逻辑类
 * 管理回合制战斗、炸弹逻辑、商店系统、队伍经济、玩家装备等核心机制
 */
@Mod.EventBusSubscriber(modid = BlockOffensive.MODID,bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CSGameMap extends BaseMap implements IConfigureMap<CSGameMap>{
    /**
     * Codec序列化配置（用于地图数据保存/加载）
     */
    public static final Codec<CSGameMap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            // 基础地图数据
            Codec.STRING.fieldOf("mapName").forGetter(CSGameMap::getMapName),
            AreaData.CODEC.fieldOf("mapArea").forGetter(CSGameMap::getMapArea),
            ResourceLocation.CODEC.fieldOf("serverLevel").forGetter(map -> map.getServerLevel().dimension().location()),
            CapabilityMap.CapabilityMapWrapper.DATA_CODEC.fieldOf("capabilities").forGetter(csGameMap -> csGameMap.getCapabilityMap().getData().data()),
            // 队伍数据
            Codec.unboundedMap(
                    Codec.STRING,
                    CapabilityMap.CapabilityMapWrapper.CODEC
            ).fieldOf("teams").forGetter(csGameMap -> csGameMap.getMapTeams().getData())
    ).apply(instance, (mapName, mapArea, serverLevel, capability, teamsData) -> {
        // 创建新的CSGameMap实例
        CSGameMap gameMap = new CSGameMap(
                FPSMCore.getInstance().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,serverLevel)),
                mapName,
                mapArea
        );

        gameMap.getCapabilityMap().write(capability);
        gameMap.getMapTeams().writeData(teamsData);

        return gameMap;
    }));

    private static final Vector3f T_COLOR = new Vector3f(1, 0.75f, 0.25f);
    private static final Vector3f CT_COLOR = new Vector3f(0.25f, 0.55f, 1);
    private static final Map<String, BiConsumer<CSGameMap,ServerPlayer>> COMMANDS = registerCommands();
    private final ArrayList<Setting<?>> settings = new ArrayList<>();
    private final Setting<Boolean> canAutoStart = this.addSetting("autoStart",true);
    private final Setting<Integer> autoStartTime = this.addSetting("autoStartTime",6000);
    private final Setting<Integer> winnerRound = this.addSetting("winnerRound",13); // 13回合
    private final Setting<Integer> overtimeRound = this.addSetting("overtimeRound",3); // 3回合
    private final Setting<Integer> pauseTime = this.addSetting("pauseTime",1200); // 60秒
    private final Setting<Integer> winnerWaitingTime = this.addSetting("winnerWaitingTime",160);
    private final Setting<Integer> warmUpTime = this.addSetting("warmUpTime",1200);
    private final Setting<Integer> waitingTime = this.addSetting("waitingTime",300);
    private final Setting<Integer> roundTimeLimit = this.addSetting("roundTimeLimit",2300);
    private final Setting<Integer> startMoney = this.addSetting("startMoney",800);
    private final Setting<Integer> defaultLoserEconomy = this.addSetting("defaultLoserEconomy",1400);
    private final Setting<Integer> defuseBonus = this.addSetting("defuseBonus",600);
    private final Setting<Integer> compensationBase = this.addSetting("compensationBase",500);
    private final Setting<Integer> tDeathRewardPer = this.addSetting("tDeathRewardPer",50);
    private final Setting<Integer> closeShopTime = this.addSetting("closeShopTime",200);
    private final Setting<Boolean> knifeSelection = this.addSetting("knifeSelection",false);
    private final Setting<Integer> ctLimit = this.addSetting("ctLimit",5);
    private final Setting<Integer> tLimit = this.addSetting("tLimit",5);
    private final Setting<Boolean> allowFriendlyFire = this.addSetting("allowFriendlyFire",false);
    private static final List<Class<? extends MapCapability>> MAP_CAPABILITIES = List.of(DemolitionModeCapability.class, GameEndTeleportCapability.class);
    private static final List<Class<? extends TeamCapability>> TEAM_CAPABILITIES = List.of(PauseCapability.class, CompensationCapability.class, TeamSwitchRestrictionCapability.class, ShopCapability.class, StartKitsCapability.class);
    private final ServerTeam ctTeam;
    private final ServerTeam tTeam;
    private int currentPauseTime = 0;
    private int currentRoundTime = 0;
    private boolean isError = false;
    private boolean isPause = false;
    private boolean isWaiting = false;
    private boolean isWarmTime = false;
    private boolean isWaitingWinner = false;
    private boolean isShopLocked = false;
    private boolean isKnifeSelected = false;
    private boolean isOvertime = false;
    private int overCount = 0;
    private boolean isWaitingOverTimeVote = false;
    private VoteObj voteObj = null;
    private SpawnPointData matchEndTeleportPoint = null;
    private int autoStartTimer = 0;
    private boolean autoStartFirstMessageFlag = false;

    private final Map<UUID,Integer> knifeCache = new HashMap<>();

    /**
     * 构造函数：创建CS地图实例
     * @param serverLevel 服务器世界实例
     * @param mapName 地图名称
     * @param areaData 地图区域数据
     * @see #addTeam(TeamData) 初始化时自动添加CT和T阵营
     */
    public CSGameMap(ServerLevel serverLevel, String mapName, AreaData areaData) {
        super(serverLevel,mapName,areaData, MAP_CAPABILITIES);
        this.loadConfig();
        this.ctTeam = this.addTeam(TeamData.of("ct",ctLimit.get(), TEAM_CAPABILITIES));
        this.ctTeam.setColor(CT_COLOR);
        this.tTeam = this.addTeam(TeamData.of("t",tLimit.get(), TEAM_CAPABILITIES));
        this.tTeam.setColor(T_COLOR);
        CapabilityMap.getMapCapability(this, DemolitionModeCapability.class).ifPresent(cap -> cap.setDemolitionTeam(this.tTeam));
    }
    /**
     * 切换两个队伍的阵营
     */
    private void switchTeams() {
        MapTeams.switchAttackAndDefend(this,getTTeam(),getCTTeam());
    }

    /**
     * 刀局胜利处理
     */
    private void knifeRoundVictory(ServerTeam winnerTeam) {
        Component translation = Component.translatable("blockoffensive.cs.knife.selection");
        Component message = Component.translatable("blockoffensive.map.vote.message","System",translation);
        this.isPause = true;
        this.isKnifeSelected = true;
        VoteObj knife = new VoteObj(
                "knife",
                message,
                20,
                0.6F,
                ()->{
                    this.switchTeams();
                    this.setUnPauseState();
                    this.start();
                    },
                ()-> {
                    this.setUnPauseState();
                    this.start();
                },
                winnerTeam.getPlayerList()
        );
        this.startVote(knife);
    }

    public boolean isKnifeSelectingVote(){
        return this.voteObj != null && this.voteObj.getVoteTitle().equals("knife");
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event){
        Optional<BaseMap> optional = FPSMCore.getInstance().getMapByPlayer(event.getPlayer());
        if(optional.isEmpty()) return;
        BaseMap map = optional.get();
        if(map instanceof CSGameMap csGameMap){
            String[] m = event.getMessage().getString().split("\\.");
            if(m.length > 1){
                csGameMap.handleChatCommand(m[1],event.getPlayer());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOutEvent(PlayerEvent.PlayerLoggedOutEvent event){
        if(event.getEntity() instanceof ServerPlayer player){
            Optional<BaseMap> optional = FPSMCore.getInstance().getMapByPlayer(player);
            if(optional.isEmpty()) return;
            BaseMap map = optional.get();
            if(map instanceof CSGameMap){
                dropC4(player);
                player.getInventory().clearContent();
            }
        }
    }

    private static void dropC4(ServerPlayer player) {
        int im = player.getInventory().clearOrCountMatchingItems((i) -> i.getItem() instanceof CompositionC4, -1, player.inventoryMenu.getCraftSlots());
        if (im > 0) {
            player.drop(new ItemStack(BOItemRegister.C4.get(), 1), false, false).setGlowingTag(true);
            player.getInventory().setChanged();
        }
    }

    @SubscribeEvent
    public static void onPlayerDropItem(ItemTossEvent event){
        if(event.getEntity().level().isClientSide) return;
        ItemStack itemStack = event.getEntity().getItem();
        Optional<BaseMap> optional = FPSMCore.getInstance().getMapByPlayer(event.getPlayer());
        if(optional.isEmpty()) return;
        BaseMap map = optional.get();
        if (map instanceof CSGameMap csGameMap){
            if(itemStack.getItem() instanceof CompositionC4){
                event.getEntity().setGlowingTag(true);
            }

            if(itemStack.getItem() instanceof BombDisposalKit){
                event.setCanceled(true);
                event.getPlayer().displayClientMessage(Component.translatable("blockoffensive.item.bomb_disposal_kit.drop.message").withStyle(ChatFormatting.RED),true);
                event.getPlayer().getInventory().add(new ItemStack(BOItemRegister.BOMB_DISPOSAL_KIT.get(),1));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerKilledByGun(EntityKillByGunEvent event){
        if(event.getLogicalSide() == LogicalSide.SERVER){
            if (event.getKilledEntity() instanceof ServerPlayer player) {
                Optional<BaseMap> optional = FPSMCore.getInstance().getMapByPlayer(player);
                if(optional.isEmpty()) return;
                BaseMap map = optional.get();
                if (map instanceof CSGameMap cs && map.checkGameHasPlayer(player)) {
                    if(event.getAttacker() instanceof ServerPlayer attacker){
                        Optional<BaseMap> optionalFromMap = FPSMCore.getInstance().getMapByPlayer(player);
                        if(optionalFromMap.isEmpty()) return;
                        BaseMap fromMap = optionalFromMap.get();
                        if (fromMap instanceof CSGameMap csGameMap && csGameMap.equals(map)) {
                            if(IGun.mainHandHoldGun(attacker)) {
                                csGameMap.giveEco(player,attacker,attacker.getMainHandItem());
                                csGameMap.getMapTeams().getTeamByPlayer(attacker).ifPresent(team->{
                                    if(event.isHeadShot()){
                                        team.getPlayerData(attacker.getUUID()).ifPresent(PlayerData::addHeadshotKill);
                                    }
                                });

                                DeathMessage.Builder builder = new DeathMessage.Builder(attacker, player, attacker.getMainHandItem()).setHeadShot(event.isHeadShot());
                                Map<UUID, Float> hurtDataMap = cs.getMapTeams().getDamageMap().get(player.getUUID());
                                if (hurtDataMap != null && !hurtDataMap.isEmpty()) {
                                    hurtDataMap.entrySet().stream()
                                            .filter(entry -> entry.getValue() > player.getMaxHealth() / 4)
                                            .sorted(Map.Entry.<UUID, Float>comparingByValue().reversed())
                                            .limit(1)
                                            .findAny()
                                            .flatMap(entry -> cs.getMapTeams().getTeamByPlayer(entry.getKey())
                                                    .flatMap(team -> team.getPlayerData(entry.getKey()))).ifPresent(playerData -> {
                                                if (!attacker.getUUID().equals(playerData.getOwner())){
                                                    builder.setAssist(playerData.name(), playerData.getOwner());
                                                }
                                            });
                                }
                                DeathMessageS2CPacket killMessageS2CPacket = new DeathMessageS2CPacket(builder.build());
                                csGameMap.sendPacketToAllPlayer(killMessageS2CPacket);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 玩家死亡事件处理
     * @see #handlePlayerDeath(ServerPlayer,Entity) 处理死亡逻辑
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDeathEvent(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Optional<BaseMap> optional = FPSMCore.getInstance().getMapByPlayer(player);
            if (optional.isPresent() && optional.get() instanceof CSGameMap csGameMap) {
                if(BOImpl.isPhysicsModLoaded()){
                    csGameMap.sendPacketToAllPlayer(new PxDeathCompatS2CPacket(player.getId()));
                }
                csGameMap.handlePlayerDeathMessage(player,event.getSource());
                csGameMap.handlePlayerDeath(player,event.getSource().getEntity());
                csGameMap.sendPacketToJoinedPlayer(player,new FPSMatchRespawnS2CPacket(),true);
                event.setCanceled(true);
            }
        }
    }

    public static Map<String, BiConsumer<CSGameMap,ServerPlayer>> registerCommands(){
        Map<String, BiConsumer<CSGameMap,ServerPlayer>> commands = new HashMap<>();
        commands.put("p", CSGameMap::setPauseState);
        commands.put("pause", CSGameMap::setPauseState);
        commands.put("unpause", CSGameMap::startUnpauseVote);
        commands.put("up", CSGameMap::startUnpauseVote);
        commands.put("agree", CSGameMap::handleAgreeCommand);
        commands.put("a", CSGameMap::handleAgreeCommand);
        commands.put("disagree", CSGameMap::handleDisagreeCommand);
        commands.put("da", CSGameMap::handleDisagreeCommand);
        commands.put("d", CSGameMap::handleDropKnifeCommand);
        commands.put("drop", CSGameMap::handleDropKnifeCommand);
        return commands;
    }

    private void handleDropKnifeCommand(ServerPlayer player) {
        List<ItemStack> list = FPSMUtil.searchInventoryForType(player.getInventory(),DropType.THIRD_WEAPON);
        int currentKnives = knifeCache.getOrDefault(player.getUUID(), 0);
        if(!list.isEmpty() && currentKnives < 5){
            knifeCache.put(player.getUUID(), currentKnives + 1);
            FPSMUtil.playerDropMatchItem(player,list.get(0).copy());
        }
    }

    public static void write(FPSMDataManager manager){
        FPSMCore.getInstance().getMapByClass(CSGameMap.class)
                .forEach((map -> {
                    map.saveConfig();
                    manager.saveData(map,map.getMapName(),false);
                }));
    }

    /**
     * 添加队伍并初始化商店系统
     * @param data 队伍数据
     * @see FPSMShop 每个队伍拥有独立商店实例
     */
    @Override
    public ServerTeam addTeam(TeamData data){
        ServerTeam team = super.addTeam(data);
        PlayerTeam playerTeam = team.getPlayerTeam();
        playerTeam.setNameTagVisibility(Team.Visibility.HIDE_FOR_OTHER_TEAMS);
        playerTeam.setAllowFriendlyFire(allowFriendlyFire.get());
        playerTeam.setSeeFriendlyInvisibles(false);
        playerTeam.setDeathMessageVisibility(Team.Visibility.NEVER);

        CapabilityMap.getTeamCapability(this,ShopCapability.class).forEach((t,opt)->{
                opt.ifPresent(cap -> cap.initialize("cs",startMoney.get()));
        });

        return team;
    }


    public void startVote(VoteObj voteObj){
        if(this.voteObj == null){
            this.voteObj = voteObj;
            this.sendVoteMessage(false,
                    voteObj.getMessage(),
                    Component.translatable("blockoffensive.map.vote.help").withStyle(ChatFormatting.GREEN));
        }
    }

    public void sendVoteMessage(boolean actionBar,Component... messages){
        if(this.voteObj != null){
            for (UUID uuid : this.voteObj.getEligiblePlayers()){
                this.getPlayerByUUID(uuid).ifPresent(player -> {
                    for (Component message : messages){
                        player.displayClientMessage(message,actionBar);
                    }
                });
            }
        }
    }

    public void syncShopInfo(boolean enable, int closeTime){
        for (ServerTeam team : this.getMapTeams().getNormalTeams()){
            int next = this.getNextRoundMinMoney(team);
            var packet = new ShopStatesS2CPacket(enable,next,closeTime);
            this.sendPacketToTeamLivingPlayer(team,packet);
        }
    }

    public int getNextRoundMinMoney(ServerTeam team){
        int defaultEconomy = 1400;
        int compensation = 500;
        int compensationFactor = Math.min(4, getCompensationFactor(team) + 1);
        // 计算失败补偿
        return defaultEconomy + compensation * compensationFactor;
    }

    public static int getCompensationFactor(ServerTeam team){
        return team.getCapabilityMap().get(CompensationCapability.class).map(CompensationCapability::getCompensationFactor).orElse(0);
    }

    /**
     * 游戏主循环逻辑（每tick执行）
     * 管理暂停状态、回合时间、胜利条件检查等核心流程
     * @see #checkRoundVictory() 检查回合胜利条件
     * @see #checkBlastingVictory() 检查炸弹爆炸胜利
     * @see #startNewRound() 启动新回合
     */
    @Override
    public void tick() {
        if(isStart && !checkPauseTime()){
            // 暂停 / 热身 / 回合开始前的等待时间
            if (!checkWarmUpTime() & !checkWaitingTime()) {
                if(!isRoundTimeEnd()){
                    if(!this.isDebug()){
                        boolean flag = this.getMapTeams().getJoinedPlayers().size() != 1;
                        switch (this.blastState()){
                            case TICKING : this.checkBlastingVictory(); break;
                            case EXPLODED : this.roundVictory(this.getTTeam(),WinnerReason.DETONATE_BOMB); break;
                            case DEFUSED : this.roundVictory(this.getCTTeam(),WinnerReason.DEFUSE_BOMB); break;
                            case NONE : if(flag) this.checkRoundVictory(); break;
                        }

                        // 回合结束等待时间
                        if(this.isWaitingWinner){
                            checkWinnerTime();

                            if(this.currentPauseTime >= winnerWaitingTime.get()){
                                this.startNewRound();
                            }
                        }
                    }
                }else{
                    if(!checkWinnerTime()){
                        this.roundVictory(this.getCTTeam(),WinnerReason.TIME_OUT);
                    }else if(this.currentPauseTime >= winnerWaitingTime.get()){
                        this.startNewRound();
                    }
                }
            }
        }
        this.voteLogic();
        this.autoStartLogic();
    }

    private void autoStartLogic() {
        // 检查自动开始功能是否启用
        if (!canAutoStart.get()) return;

        // 如果游戏已经开始，重置状态
        if (isStart) {
            resetAutoStartState();
            return;
        }

        // 检查两队是否都有玩家
        boolean bothTeamsHavePlayers = !getCTTeam().getOnlinePlayers().isEmpty() && !getTTeam().getOnlinePlayers().isEmpty();

        if (bothTeamsHavePlayers) {
            handleActiveCountdown();
        } else {
            resetAutoStartState();
        }
    }

    private void resetAutoStartState() {
        if(autoStartTimer != 0 || autoStartFirstMessageFlag){
            autoStartTimer = 0;
            autoStartFirstMessageFlag = false;
            clearOfflinePlayers();
        }
    }

    private void clearOfflinePlayers(){
        for(ServerTeam team : this.getMapTeams().getNormalTeams()){
            for (UUID offline : team.getOfflinePlayers()) {
                this.getMapTeams().leaveTeam(offline);
            }
        }
    }

    private void handleActiveCountdown() {
        autoStartTimer++;
        int totalTicks = autoStartTime.get();
        int secondsLeft = (totalTicks - autoStartTimer) / 20;

        // 发送初始提示消息（仅一次）
        if (!autoStartFirstMessageFlag) {
            sendAutoStartMessage(secondsLeft);
            autoStartFirstMessageFlag = true;
        }

        // 处理倒计时结束
        if (autoStartTimer >= totalTicks) {
            startGameWithAnnouncement();
            return;
        }

        // 发送周期性提示
        if (shouldSendTitleNotification(totalTicks)) {
            sendTitleNotification(secondsLeft);
        } else if (shouldSendActionbar()) {
            sendActionbarMessage(secondsLeft);
        }
    }

    private boolean shouldSendTitleNotification(int totalTicks) {
        // 最后30秒：每10秒发送一次
        if (autoStartTimer >= (totalTicks - 600) && autoStartTimer % 200 == 0) {
            return true;
        }
        // 最后10秒：每秒发送一次
        return autoStartTimer >= (totalTicks - 200) && autoStartTimer % 20 == 0;
    }

    private boolean shouldSendActionbar() {
        return autoStartTimer % 20 == 0 && this.voteObj == null;
    }

    private void sendAutoStartMessage(int seconds) {
        Component message = Component.translatable("blockoffensive.map.cs.auto.start.message", seconds)
                .withStyle(ChatFormatting.YELLOW);
        this.sendAllPlayerMessage(message, false);
    }

    private void sendTitleNotification(int seconds) {
        Component title = Component.translatable("blockoffensive.map.cs.auto.start.title", seconds)
                .withStyle(ChatFormatting.YELLOW);
        Component subtitle = Component.translatable("blockoffensive.map.cs.auto.start.subtitle")
                .withStyle(ChatFormatting.YELLOW);

        sendTitleToAllPlayers(title, subtitle);
    }

    private void sendActionbarMessage(int seconds) {
        Component message = Component.translatable("blockoffensive.map.cs.auto.start.actionbar", seconds)
                .withStyle(ChatFormatting.YELLOW);
        this.sendAllPlayerMessage(message, true);
    }

    private void startGameWithAnnouncement() {
        Component title = Component.translatable("blockoffensive.map.cs.auto.started")
                .withStyle(ChatFormatting.YELLOW);
        sendTitleToAllPlayers(title);
        resetAutoStartState();
        this.start();
    }

    private void sendTitleToAllPlayers(Component title) {
        this.sendTitleToAllPlayers(title, Component.empty());
    }

    private void sendTitleToAllPlayers(Component title, Component subtitle) {
        this.getMapTeams().getJoinedPlayers().forEach(data ->
                data.getPlayer().ifPresent(player -> {
                    player.connection.send(new ClientboundSetTitleTextPacket(title));
                    player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
                })
        );
    }

    private void setBystander(ServerPlayer player) {
        List<UUID> uuids = this.getMapTeams().getSameTeamPlayerUUIDs(player);
        Entity entity = null;
        if (uuids.size() > 1) {
            Random random = new Random();
            entity = this.getServerLevel().getEntity(uuids.get(random.nextInt(0, uuids.size())));
        } else if (!uuids.isEmpty()) {
            entity = this.getServerLevel().getEntity(uuids.get(0));
        }
        if (entity != null) {
            player.teleportTo(entity.getX(), entity.getY() + 1, entity.getZ());
            player.setCamera(entity);
        }
    }

    @Override
    public void join(String teamName, ServerPlayer player) {
        FPSMCore.checkAndLeaveTeam(player);
        MapTeams mapTeams = this.getMapTeams();
        mapTeams.joinTeam(teamName, player);
        mapTeams.getTeamByPlayer(player).ifPresent(team -> {
            MinecraftForge.EVENT_BUS.post(new CSGamePlayerJoinEvent(this,team,player));
            // 同步游戏类型和地图信息
            this.pullGameInfo(player);

            // 如果游戏已经开始，设置玩家为旁观者
            if(this.isStart){
                player.setGameMode(GameType.SPECTATOR);
                team.getPlayerData(player.getUUID()).ifPresent(data -> data.setLiving(false));
                setBystander(player);
            }
        });
    }

    @Override
    public void leave(ServerPlayer player) {
        this.sendPacketToAllPlayer(new CSTabRemovalS2CPacket(player.getUUID()));
        super.leave(player);
    }

    @Override
    public String getGameType() {
        return "cs";
    }

    private void voteLogic() {
        if (this.voteObj != null) {
            // 调用 tick 方法自动判断投票状态（会触发回调函数）
            boolean voteEnded = this.voteObj.tick();

            if (!voteEnded) {
                // 投票仍在进行中，显示剩余时间
                this.sendAllPlayerMessage(
                        Component.translatable("blockoffensive.map.vote.timer", this.voteObj.getRemainingTime())
                                .withStyle(ChatFormatting.DARK_AQUA),
                        true
                );
            } else {
                // 投票已结束，清理投票对象
                this.voteObj = null;
            }
        }
    }

    /**
     * 开始新游戏（初始化所有玩家状态）
     * 核心优化：拆分职责、复用Setting配置、消除魔法值、简化嵌套逻辑
     * @see #giveAllPlayersKits() 发放初始装备
     * @see #giveBlastTeamBomb() 给爆破方分配C4
     */
    public boolean start() {
        if (!super.start()) {
            return false;
        }

        // 2. 提取重复依赖（减少多次get调用，提升性能）
        MapTeams mapTeams = getMapTeams();
        ServerLevel serverLevel = getServerLevel();

        // 3. 基础配置与状态初始化
        setTeamNameColors();
        if (this.isError) return false;

        configureGameRules(serverLevel); // 配置游戏规则
        resetGameCoreState();

        //出生点校验
        if (!validateAndSetSpawnPoints()) {
            this.reset();
            return false;
        }

        // 5. 地图与队伍初始化
        cleanupMap(); // 复用已优化的地图清理方法
        mapTeams.startNewRound();
        mapTeams.resetLivingPlayers();
        initializeTeams(mapTeams); // 拆分队伍初始化逻辑

        // 6. 玩家状态初始化（拆分独立处理）
        initializeAllJoinedPlayers(mapTeams);

        // 7. 刀具选择阶段处理（复用Setting配置，拆分逻辑）
        handleKnifeSelectionPhase();

        // 8. 同步游戏启动消息
        syncNormalRoundStartMessage();

        return true;
    }

    /**
     * 设置队伍名称颜色（CT蓝、T黄）
     */
    private void setTeamNameColors() {
        MapTeams mapTeams = getMapTeams();
        mapTeams.setTeamNameColor(this, "ct", ChatFormatting.BLUE);
        mapTeams.setTeamNameColor(this, "t", ChatFormatting.YELLOW);
    }

    /**
     * 配置游戏规则（基于BOConfig配置）
     */
    private void configureGameRules(ServerLevel serverLevel) {
        GameRules gameRules = serverLevel.getGameRules();
        gameRules.getRule(GameRules.RULE_KEEPINVENTORY).set(BOConfig.common.keepInventory.get(), null);
        gameRules.getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(BOConfig.common.immediateRespawn.get(), null);
        gameRules.getRule(GameRules.RULE_DAYLIGHT).set(BOConfig.common.daylightCycle.get(), null);
        gameRules.getRule(GameRules.RULE_WEATHER_CYCLE).set(BOConfig.common.weatherCycle.get(), null);
        gameRules.getRule(GameRules.RULE_DOMOBSPAWNING).set(BOConfig.common.mobSpawning.get(), null);
        gameRules.getRule(GameRules.RULE_NATURAL_REGENERATION).set(BOConfig.common.naturalRegeneration.get(), null);

        if (BOConfig.common.hardDifficulty.get()) {
            serverLevel.getServer().setDifficulty(Difficulty.HARD, true);
        }
    }


    /**
     * 重置游戏核心状态（加时、计数、等待状态等）
     */
    private void resetGameCoreState() {
        this.isOvertime = false;
        this.overCount = 0;
        this.isWaitingOverTimeVote = false;
        this.isStart = true;
        this.isWaiting = true;
        this.isWaitingWinner = false;
        this.currentPauseTime = 0;
        this.isShopLocked = false;
        this.isKnifeSelected = false;
    }

    /**
     * 校验并设置队伍出生点
     * @return 出生点设置是否成功
     */
    private boolean validateAndSetSpawnPoints() {
        return this.setTeamSpawnPoints();
    }

    /**
     * 初始化队伍状态（分数、补偿因子、玩家数据重置）
     */
    private void initializeTeams(MapTeams mapTeams) {
        mapTeams.getNormalTeams().forEach(team -> {
            team.setScores(0);
            setCompensationFactor(team, 0); // 重置补偿因子
            // 重置队伍内所有玩家数据
            team.getPlayers().forEach((uuid, data) -> data.reset());
        });
    }

    /**
     * 初始化所有已加入队伍的玩家状态
     */
    private void initializeAllJoinedPlayers(MapTeams mapTeams) {
        mapTeams.getJoinedPlayers().forEach(this::initializeSinglePlayer);
    }

    /**
     * 初始化单个玩家状态
     */
    private void initializeSinglePlayer(PlayerData data) {
        data.reset();
        data.getPlayer().ifPresent(player -> {
            player.removeAllEffects();
            player.addEffect(new MobEffectInstance(
                    MobEffects.SATURATION,
                    -1,
                    2,
                    false,
                    false,
                    false
            ));
            player.heal(player.getMaxHealth());
            player.setGameMode(GameType.ADVENTURE);
            this.clearPlayerInventory(player);
            this.teleportPlayerToReSpawnPoint(player);
        });
    }

    /**
     * 处理刀具选择阶段逻辑（商店锁定、装备发放、C4分配、金钱设置）
     */
    private void handleKnifeSelectionPhase() {
        // 刀具选择阶段判断（复用Setting配置，语义化命名）
        boolean isKnifeSelectionPhase = knifeSelection.get() && !isKnifeSelected;
        this.isShopLocked = isKnifeSelectionPhase;

        // 同步商店信息（关闭时间复用Setting配置）
        int shopCloseTime = this.closeShopTime.get();
        syncShopInfo(!isKnifeSelectionPhase, shopCloseTime);

        // 发放初始装备
        this.giveAllPlayersKits();

        // 非刀具选择阶段：分配C4、设置初始金钱并同步商店数据
        if (!isKnifeSelectionPhase) {
            this.giveBlastTeamBomb();
            // 使用配置的初始金钱（消除魔法值800）
            ShopCapability.setPlayerMoney(this, this.startMoney.get());
            ShopCapability.syncShopData(this);
        }
    }

    public void setCompensationFactor(ServerTeam team, int factor){
        team.getCapabilityMap().get(CompensationCapability.class).ifPresent(cap->cap.setCompensationFactor(factor));
    }

    public boolean canRestTime(){
        return !this.isPause && !this.isWarmTime && !this.isWaiting && !this.isWaitingWinner;
    }

    public boolean checkPauseTime(){
        if(this.isPause && currentPauseTime < pauseTime.get()){
            this.currentPauseTime++;
        }else{
            if(this.isPause) {
                currentPauseTime = 0;
                if(this.voteObj != null && this.voteObj.getVoteTitle().equals("unpause")){
                    this.voteObj = null;
                }
                this.sendAllPlayerMessage(Component.translatable("blockoffensive.map.cs.pause.done").withStyle(ChatFormatting.GOLD),false);
            }
            isPause = false;
        }
        return this.isPause;
    }

    public boolean checkWarmUpTime(){
        if(this.isWarmTime && currentPauseTime < warmUpTime.get()){
            this.currentPauseTime++;
        }else {
            if(this.canRestTime()) {
                currentPauseTime = 0;
            }
            isWarmTime = false;
        }
        return this.isWarmTime;
    }

    public boolean checkWaitingTime(){
        if(this.isWaiting && currentPauseTime < waitingTime.get()){
            // 最后三秒 向全体玩家发送无来源声音
            if(waitingTime.get() - currentPauseTime <= 60 && currentPauseTime % 20 == 0){
                this.sendPacketToAllPlayer(new FPSMusicPlayS2CPacket(SoundEvents.NOTE_BLOCK_BELL.value().getLocation()));
            }

            this.currentPauseTime++;
            boolean b = false;
            Iterator<ServerTeam> teams = new ArrayList<>(this.getMapTeams().getNormalTeams()).iterator();
            while (teams.hasNext()){
                ServerTeam team = teams.next();
                PauseCapability cap = team.getCapabilityMap().get(PauseCapability.class).orElse(null);
                if(cap != null){
                    if(!b){
                        b = cap.needPause();
                        if(b){
                            cap.setNeedPause(false);
                        }
                    }else{
                        cap.resetPauseIfNeed();
                    }
                }
                teams.remove();
            }

            if(b){
                this.sendAllPlayerMessage(Component.translatable("blockoffensive.map.cs.pause.now").withStyle(ChatFormatting.GOLD),false);
                this.isPause = true;
                this.currentPauseTime = 0;
                this.isWaiting = true;
            }
        }else {
            if(this.canRestTime()) currentPauseTime = 0;
            isWaiting = false;
        }
        return this.isWaiting;
    }

    public boolean checkWinnerTime(){
        if(this.isWaitingWinner && currentPauseTime < winnerWaitingTime.get()){
            if(!isKnifeSelectingVote()) this.currentPauseTime++;
        }else{
            if(this.canRestTime()) currentPauseTime = 0;
        }
        return this.isWaitingWinner;
    }

    public void checkRoundVictory(){
        if(isWaitingWinner) return;
        Map<ServerTeam, List<UUID>> teamsLiving = this.getMapTeams().getTeamsLiving();
        if(teamsLiving.size() == 1){
            ServerTeam winnerTeam = teamsLiving.keySet().stream().findFirst().get();
            this.roundVictory(winnerTeam, WinnerReason.ACED);
        }

        if(teamsLiving.isEmpty()){
            this.roundVictory(this.getCTTeam(),WinnerReason.ACED);
        }
    }

    public void checkBlastingVictory(){
        if(isWaitingWinner) return;
        Map<ServerTeam, List<UUID>> teamsLiving = this.getMapTeams().getTeamsLiving();
        if(teamsLiving.size() == 1){
            ServerTeam winnerTeam = teamsLiving.keySet().stream().findFirst().get();
            boolean flag = this.checkCanPlacingBombs(winnerTeam.getFixedName());
            if(flag){
                this.roundVictory(winnerTeam,WinnerReason.ACED);
            }
        }else if(teamsLiving.isEmpty()){
            this.roundVictory(this.getTTeam(),WinnerReason.ACED);
        }
    }

    public boolean isRoundTimeEnd(){
        if(this.blastState() != BlastBombState.NONE){
            this.currentRoundTime = -1;
            return false;
        }
        if(this.currentRoundTime < this.roundTimeLimit.get()){
            this.currentRoundTime++;
        }
        if(this.isClosedShop()){
            this.isShopLocked = true;
            this.syncShopInfo(false,0);
        }
        return this.currentRoundTime >= this.roundTimeLimit.get();
    }

    public boolean isClosedShop(){
        return (this.currentRoundTime >= this.closeShopTime.get() || this.currentRoundTime == -1 ) && !this.isShopLocked;
    }

    public int getShopCloseTime(){
        if(knifeSelection.get() && !isKnifeSelected) return 0;

        int closeTime = (this.closeShopTime.get() - this.currentRoundTime);
        if (closeTime < 0) return 0;
        if(this.isWaiting){
            closeTime += this.waitingTime.get() - this.currentPauseTime;
        }
        return closeTime / 20;
    }

    /**
     * 向所有玩家发送标题消息
     * @param title 主标题内容
     * @param subtitle 副标题内容（可选）
     * @see ClientboundSetTitleTextPacket Mojang网络协议包
     */
    public void sendAllPlayerTitle(Component title,@Nullable Component subtitle){
        this.getMapTeams().getJoinedPlayers().forEach((data -> data.getPlayer().ifPresent(player -> {
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            if(subtitle != null){
                player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            }
        })));
    }

    /**
     * 安全获取玩家商店数据（避免重复的Optional链式调用）
     */
    private Optional<ShopData<?>> getShopDataSafely(UUID playerUuid) {
        return ShopCapability.getPlayerShopData(this, playerUuid);
    }

    /**
     * 统一发送金钱奖励消息（避免重复构建Component）
     */
    private void sendMoneyRewardMessage(ServerPlayer player, int amount, String reasonDesc) {
        player.sendSystemMessage(Component.translatable(
                "blockoffensive.map.cs.reward.money",
                amount,
                reasonDesc
        ));
    }

    /**
     * 安全获取补偿系数（避免team为null时的NPE）
     */
    private int getCompensationFactorSafely(ServerTeam team) {
        return team != null ? getCompensationFactor(team) : 0;
    }


    /**
     * 处理回合胜利逻辑
     * @param winnerTeam 获胜队伍
     * @param reason 胜利原因（如炸弹拆除/爆炸）
     * @see #checkLoseStreaks(ServerTeam) 计算经济奖励
     * @see #checkMatchPoint() 检查赛点状态
     * @see MVPMusicManager MVP音乐播放逻辑
     */
    private void roundVictory(@NotNull ServerTeam winnerTeam, @NotNull WinnerReason reason) {
        //刀局特殊处理（提前返回，减少嵌套）
        if (handleKnifeRoundSpecialCase(winnerTeam)) {
            return;
        }

        //状态检查：已在等待胜利者则直接返回
        if (isWaitingWinner) {
            return;
        }

        MapTeams mapTeams = getMapTeams();
        isWaitingWinner = true;

        //处理MVP逻辑
        MvpReason mvpReason = processMvpLogic(winnerTeam, mapTeams);

        //发送MVP数据包/发布回合结束事件
        sendPacketToAllPlayer(new MvpMessageS2CPacket(mvpReason));
        MinecraftForge.EVENT_BUS.post(new CSGameRoundEndEvent(this, winnerTeam, reason));

        //处理比分更新和加时投票
        processRoundScoreAndOvertimeVote(winnerTeam, mapTeams);

        //处理所有经济奖励（拆分胜利方、失败方、CT额外奖励）
        processEconomicRewards(winnerTeam, reason, mapTeams);

        //同步商店金钱数据
        ShopCapability.syncShopData(this);
    }

    /**
     * 处理刀局特殊情况
     * @return 是否触发刀局逻辑（是则返回true，中断主流程）
     */
    private boolean handleKnifeRoundSpecialCase(@NotNull ServerTeam winnerTeam) {
        if (knifeSelection.get() && !isKnifeSelected) {
            knifeRoundVictory(winnerTeam);
            this.isWaitingWinner = true;
            return true;
        }
        return false;
    }

    /**
     * 处理MVP相关逻辑（提取MVP数据、发布事件、播放MVP音乐）
     * @return 处理后的MVP原因（为空时返回默认实例）
     */
    private MvpReason processMvpLogic(@NotNull ServerTeam winnerTeam, @NotNull MapTeams mapTeams) {
        MapTeams.RawMVPData mvpData = mapTeams.getRoundMvpPlayer(winnerTeam);
        if (mvpData == null) {
            return new MvpReason.Builder(UUID.nameUUIDFromBytes(winnerTeam.name.getBytes()))
                    .setTeamName(Component.literal(winnerTeam.name.toUpperCase(Locale.ROOT)))
                    .build();
        }

        Optional<ServerPlayer> mvpPlayer = getPlayerByUUID(mvpData.uuid());
        if (mvpPlayer.isEmpty()) {
            return new MvpReason.Builder(UUID.randomUUID())
                    .setTeamName(Component.literal(winnerTeam.name.toUpperCase(Locale.ROOT)))
                    .build();
        }

        // 构建MVP事件并发布
        MvpReason originalMvpReason = new MvpReason.Builder(mvpData.uuid())
                .setMvpReason(Component.literal(mvpData.reason()))
                .setPlayerName(mapTeams.playerName.get(mvpData.uuid()))
                .setTeamName(Component.literal(winnerTeam.name.toUpperCase(Locale.ROOT)))
                .build();

        CSGamePlayerGetMvpEvent mvpEvent = new CSGamePlayerGetMvpEvent(mvpPlayer.get(), this, originalMvpReason);
        MinecraftForge.EVENT_BUS.post(mvpEvent);

        // 播放MVP专属音乐
        String mvpPlayerUuid = mvpData.uuid().toString();
        if (MVPMusicManager.getInstance().playerHasMvpMusic(mvpPlayerUuid)) {
            sendPacketToAllPlayer(new FPSMusicPlayS2CPacket(
                    MVPMusicManager.getInstance().getMvpMusic(mvpPlayerUuid)
            ));
        }

        return mvpEvent.getReason();
    }

    /**
     * 处理比分更新和加时投票逻辑
     */
    private void processRoundScoreAndOvertimeVote(@NotNull ServerTeam winnerTeam, @NotNull MapTeams mapTeams) {
        int currentScore = winnerTeam.getScores();
        int newScore = currentScore + 1;
        int targetForOvertime = winnerRound.get() - 1;
        winnerTeam.setScores(newScore);

        // 仅当双方比分均为targetForOvertime且未进入加时时，触发加时投票
        List<ServerTeam> normalTeams = mapTeams.getNormalTeams();
        if (newScore == targetForOvertime && !isOvertime) {
            List<ServerTeam> otherTeams = normalTeams.stream()
                    .filter(team -> !team.equals(winnerTeam))
                    .toList();

            if (!otherTeams.isEmpty() && otherTeams.get(0).getScores() == targetForOvertime) {
                this.isWaitingOverTimeVote = true;
            }
        }
    }

    /**
     * 统一处理所有经济奖励（胜利方 + 失败方 + CT额外奖励）
     */
    private void processEconomicRewards(@NotNull ServerTeam winnerTeam, @NotNull WinnerReason reason, @NotNull MapTeams mapTeams) {
        List<ServerTeam> normalTeams = mapTeams.getNormalTeams();
        List<ServerTeam> loserTeams = normalTeams.stream()
                .filter(team -> !team.equals(winnerTeam))
                .toList();

        // 检查连胜情况
        checkLoseStreaks(winnerTeam);

        //胜利方经济奖励
        processWinnerEconomicReward(winnerTeam, reason);

        //失败方经济奖励
        loserTeams.forEach(loserTeam -> processLoserEconomicReward(loserTeam, reason));

        //CT队伍额外奖励（基于死亡的T数量）
        processCTTeamExtraReward();
    }

    /**
     * 处理胜利方经济奖励
     */
    private void processWinnerEconomicReward(@NotNull ServerTeam winnerTeam,WinnerReason reason) {
        winnerTeam.getPlayerList().forEach(uuid -> {
            // 安全获取商店数据（封装工具方法，减少重复）
            getShopDataSafely(uuid).ifPresent(shopData -> {
                shopData.addMoney(reason.winMoney);
            });

            // 发送奖励消息（封装工具方法，统一格式）
            getPlayerByUUID(uuid).ifPresent(player ->
                    sendMoneyRewardMessage(player, reason.winMoney, reason.name())
            );
        });
    }

    /**
     * 处理失败方经济奖励（含连败补偿计算）
     */
    private void processLoserEconomicReward(@NotNull ServerTeam loserTeam, @NotNull WinnerReason reason) {
        int compensationFactor = getCompensationFactorSafely(loserTeam);
        boolean isDefuseBonusApplicable = checkCanPlacingBombs(loserTeam.getFixedName())
                && reason == WinnerReason.DEFUSE_BOMB;

        // 基础经济 + 拆弹额外奖励（如有）
        int baseEconomy = defaultLoserEconomy.get() + (isDefuseBonusApplicable ? defuseBonus.get() : 0);
        // 总失败补偿 = 基础经济 + 连败补偿
        int totalLossCompensation = baseEconomy + (compensationBase.get() * compensationFactor);
        String rewardDesc = baseEconomy + " + " + compensationBase.get() + " * " + compensationFactor;

        loserTeam.getPlayerList().forEach(uuid -> {
            // 仅在符合条件时发放补偿
            boolean shouldGiveCompensation = shouldLoserGetCompensation(loserTeam, uuid, reason);
            int finalReward = shouldGiveCompensation ? totalLossCompensation : 0;
            String finalDesc = shouldGiveCompensation ? rewardDesc : "timeout living";

            // 更新商店数据
            getShopDataSafely(uuid).ifPresent(shopData -> {
                shopData.addMoney(finalReward);
            });

            // 发送奖励消息
            getPlayerByUUID(uuid).ifPresent(player ->
                    sendMoneyRewardMessage(player, finalReward, finalDesc)
            );
        });
    }

    /**
     * 检查失败方玩家是否符合补偿发放条件
     */
    private boolean shouldLoserGetCompensation(@NotNull ServerTeam loserTeam, UUID uuid, @NotNull WinnerReason reason) {
        // 非超时情况：直接发放
        if (reason != WinnerReason.TIME_OUT) {
            return true;
        }

        // 超时情况：仅给死亡玩家发放
        return loserTeam.getPlayerData(uuid)
                .map(data -> !data.isLiving())
                .orElse(false); // 无玩家数据时不发放
    }

    /**
     * 处理CT队伍额外奖励（每死亡1个T奖励50）
     */
    private void processCTTeamExtraReward() {
        ServerTeam tTeam = getTTeam();
        ServerTeam ctTeam = getCTTeam();

        // 统计死亡的T数量（在线且非存活）
        long deadTCount = tTeam.getPlayersData().stream()
                .filter(data -> data.isOnline() && !data.isLiving())
                .count();

        int extraReward = (int) deadTCount * tDeathRewardPer.get();
        if (extraReward <= 0) {
            return; // 无奖励时直接跳过
        }

        ctTeam.getPlayerList().forEach(uuid -> {
            // 更新商店数据
            getShopDataSafely(uuid).ifPresent(shopData -> {
                shopData.addMoney(extraReward);
            });

            // 发送团队奖励消息
            ctTeam.sendMessage(Component.translatable(
                    "blockoffensive.map.cs.reward.team",
                    extraReward,
                    deadTCount
            ));
        });
    }

    private void checkLoseStreaks(ServerTeam winnerTeam) {
        // 遍历所有队伍，检查连败情况
        this.getMapTeams().getNormalTeams().forEach(team -> {
            team.getCapabilityMap().get(CompensationCapability.class).ifPresent(cap -> {
                int compensationFactor = cap.getCompensationFactor();
                if (team.equals(winnerTeam)) {
                    cap.setCompensationFactor(compensationFactor - 2);
                } else {
                    cap.setCompensationFactor(compensationFactor + 1);
                }
            });
        });
    }

    public void startNewRound() {
        boolean check = this.setTeamSpawnPoints();
        if(!check){
            this.reset();
        }else{
            this.isStart = true;
            this.isWaiting = true;
            this.isWaitingWinner = false;
            this.cleanupMap();
            this.getMapTeams().startNewRound();
            this.getMapTeams().getJoinedPlayers().forEach((data -> data.getPlayer().ifPresent(player->{
                player.removeAllEffects();
                player.addEffect(new MobEffectInstance(MobEffects.SATURATION,-1,2,false,false,false));
                this.teleportPlayerToReSpawnPoint(player);
            })));

            if(knifeSelection.get() && !isKnifeSelected){
                syncShopInfo(false,getShopCloseTime());
            }else {
                syncShopInfo(true,getShopCloseTime());
                this.giveBlastTeamBomb();
                ShopCapability.syncShopData(this);
                this.checkMatchPoint();
            }
            syncNormalRoundStartMessage();
        }
    }

    public void checkMatchPoint(){
        int ctScore = this.getCTTeam().getScores();
        int tScore = this.getTTeam().getScores();
        if(this.isOvertime){
            int check = winnerRound.get() - 1 - 6 * this.overCount + 4;

            if(ctScore - check == 1 || tScore - check == 1){
                this.sendAllPlayerTitle(Component.translatable("blockoffensive.map.cs.match.point").withStyle(ChatFormatting.RED),null);
                this.sendPacketToAllPlayer(new FPSMSoundPlayS2CPacket(BOSoundRegister.MATCH_POINT.get().getLocation()));
            }
        }else{
            if(ctScore == winnerRound.get() - 1 || tScore == winnerRound.get() - 1){
                this.sendAllPlayerTitle(Component.translatable("blockoffensive.map.cs.match.point").withStyle(ChatFormatting.RED),null);
                this.sendPacketToAllPlayer(new FPSMSoundPlayS2CPacket(BOSoundRegister.MATCH_POINT.get().getLocation()));
            }
        }
    }

    private void syncNormalRoundStartMessage() {
        this.sendPacketToAllPlayer(new MvpHUDCloseS2CPacket());
        this.sendPacketToAllPlayer(new FPSMusicStopS2CPacket());
        this.sendPacketToAllPlayer(new BombDemolitionProgressS2CPacket(0));
        this.getMapTeams().getJoinedPlayersWithSpec().forEach((uuid -> this.getPlayerByUUID(uuid).ifPresent(this::syncInventory)));
    }

    private void syncInventory(ServerPlayer player) {
        player.inventoryMenu.slotsChanged(player.getInventory());
        player.inventoryMenu.broadcastChanges();
    }

    @Override
    public void victory() {
        reset();
    }

    @Override
    public boolean victoryGoal() {
        AtomicBoolean isVictory = new AtomicBoolean(false);
        if(this.isWaitingOverTimeVote){
            return false;
        }
        this.getMapTeams().getNormalTeams().forEach((team) -> {
            int overTimeRound = this.overtimeRound.get();
            if (team.getScores() >= (isOvertime ? winnerRound.get() - 1 + (this.overCount * overTimeRound) + overTimeRound + 1 : winnerRound.get())) {
                isVictory.set(true);
                MinecraftForge.EVENT_BUS.post(new FPSMapEvent.VictoryEvent(this));
                this.sendAllPlayerTitle(Component.translatable("blockoffensive.map.cs.winner." + team.name + ".title").withStyle(team.name.equals("ct") ? ChatFormatting.DARK_AQUA : ChatFormatting.YELLOW),null);
            }
        });
        return isVictory.get() && !this.isDebug();
    }

    public void startOvertimeVote() {
        Component translation = Component.translatable("blockoffensive.cs.overtime");
        Component message = Component.translatable("blockoffensive.map.vote.message","System",translation);
        VoteObj overtime = new VoteObj(
                "overtime",
                message,
                20,
                0.6F,
                this::startOvertime,
                this::reset,
                this.getMapTeams().getJoinedUUID()
        );

        this.startVote(overtime);
    }

    public void startOvertime() {
        this.isOvertime = true;
        this.isWaitingOverTimeVote = false;
        this.isPause = false;
        this.currentPauseTime = 0;

        this.getMapTeams().getNormalTeams().forEach(team-> {

            team.getPlayers().forEach((uuid, playerData) -> {
                playerData.setLiving(false);
            });

            team.getCapabilityMap().get(ShopCapability.class)
                    .flatMap(ShopCapability::getShopSafe).ifPresent(shop -> {
                        shop.setStartMoney(10000);
                        shop.resetPlayerData();
            });
        });
        this.startNewRound();
    }

    /**
     * 地图清理核心方法优化：拆分职责、提取常量、简化逻辑、提升可维护性
     */
    public boolean cleanupMap() {
        // 1. 执行父类清理逻辑（前置校验）
        if (!super.cleanupMap()) {
            return false;
        }

        // 2. 初始化核心依赖
        AreaData areaData = getMapArea();
        ServerLevel serverLevel = getServerLevel();
        MapTeams mapTeams = getMapTeams(); // 提取重复调用的对象，提升性能
        int ctScore = getCTTeam().getScores();
        int tScore = getTTeam().getScores();

        // 3. 拆分独立职责：发送物理模组兼容包
        sendPhysicsModCompatPacket();
        // 4. 拆分独立职责：清理特定实体（物品、C4、掉落物）
        cleanupSpecificEntities(serverLevel, areaData);
        // 5. 拆分独立职责：通知观察者玩家炸弹引信状态
        notifySpectatorsOfBombFuse();

        // 6. 拆分独立职责：处理加时赛与队伍切换逻辑
        boolean shouldSwitchTeams = handleOvertimeAndTeamSwitch(ctScore, tScore, mapTeams);

        // 7. 拆分独立职责：重置地图基础状态
        resetMapBaseState();

        // 8. 拆分独立职责：重置玩家状态（回血、模式、背包等）
        resetAllJoinedPlayersState(mapTeams, shouldSwitchTeams);

        // 9. 拆分独立职责：清理刀具缓存与同步商店数据
        clearKnifeCacheAndSyncShopData();

        return true;
    }

    /**
     * 发送物理模组兼容包（仅当模组加载时）
     */
    private void sendPhysicsModCompatPacket() {
        if (ModList.get().isLoaded("physicsmod")) {
            sendPacketToAllPlayer(new PxResetCompatS2CPacket());
        }
    }

    /**
     * 清理指定区域内的特定实体（物品、C4、比赛掉落物）
     */
    private void cleanupSpecificEntities(ServerLevel serverLevel, AreaData areaData) {
        // 过滤条件提取为方法引用，提升可读性
        serverLevel.getEntitiesOfClass(Entity.class, areaData.getAABB())
                .stream()
                .filter(this::shouldDiscardEntity)
                .forEach(Entity::discard);
    }

    /**
     * 判断实体是否需要被清理
     */
    private boolean shouldDiscardEntity(Entity entity) {
        return entity instanceof ItemEntity
                || entity instanceof CompositionC4Entity
                || entity instanceof MatchDropEntity;
    }

    /**
     * 通知所有观察者玩家炸弹引信状态
     */
    private void notifySpectatorsOfBombFuse() {
        int initialFuse = 0;
        int maxFuseTime = BOConfig.common.fuseTime.get();
        BombFuseS2CPacket fusePacket = new BombFuseS2CPacket(initialFuse, maxFuseTime);

        getMapTeams().getSpecPlayers().forEach(pUUID ->
                FPSMCore.getInstance().getPlayerByUUID(pUUID)
                        .ifPresent(player -> BlockOffensive.INSTANCE.send(
                                PacketDistributor.PLAYER.with(() -> player),
                                fusePacket
                        ))
        );
    }

    /**
     * 处理加时赛投票与队伍切换逻辑
     * @return 是否需要切换队伍
     */
    private boolean handleOvertimeAndTeamSwitch(int ctScore, int tScore, MapTeams mapTeams) {
        // 非加时赛逻辑
        int maxNormalScore = winnerRound.get() - 1;
        int overtimeBaseScore = maxNormalScore * 2;
        int overtimeRound = this.overtimeRound.get();
        if (!isOvertime) {
            // 双方均达12分，发起加时投票
            if (ctScore == maxNormalScore && tScore == maxNormalScore) {
                startOvertimeVote();
                setBombEntity(null);
                currentRoundTime = 0;
                isPause = true;
                currentPauseTime = 0;
                return false;
            }

            // 计算正常队伍总分数，判断是否需要换边
            int totalNormalTeamScores = mapTeams.getNormalTeams().stream()
                    .mapToInt(BaseTeam::getScores)
                    .sum();

            boolean shouldSwitch = totalNormalTeamScores == maxNormalScore;
            if (shouldSwitch) {
                switchTeams();
            }
            currentPauseTime = 0;
            return shouldSwitch;
        }

        // 加时赛逻辑：每3局换边
        int totalRounds = ctScore + tScore;
        int roundOffset = totalRounds - overtimeBaseScore - (overtimeRound * overCount);
        boolean shouldSwitch = roundOffset > 0 && roundOffset % overtimeRound == 0;

        if (shouldSwitch) {
            switchTeams();
            // 满足条件时递增加时赛计数
            if (roundOffset == overtimeRound) {
                int currentOvertimeScoreCap = maxNormalScore + (overtimeRound * overCount) + (maxNormalScore + 1);
                if (ctScore < currentOvertimeScoreCap && tScore < currentOvertimeScoreCap) {
                    overCount++;
                }
            }
        }
        currentPauseTime = 0;
        return shouldSwitch;
    }

    /**
     * 重置地图基础状态（炸弹、回合时间、商店锁定）
     */
    private void resetMapBaseState() {
        setBombEntity(null);
        currentRoundTime = 0;
        isShopLocked = false;
    }

    /**
     * 重置所有已加入队伍的玩家状态
     */
    private void resetAllJoinedPlayersState(MapTeams mapTeams, boolean shouldSwitchTeams) {
        // 提前创建团队切换提示文本（避免重复创建对象）
        Component teamSwitchTitle = Component.translatable("blockoffensive.map.cs.team.switch")
                .withStyle(ChatFormatting.GREEN);

        mapTeams.getJoinedPlayers().forEach(data ->
                data.getPlayer().ifPresent(player -> processJoinedPlayer(data, player, shouldSwitchTeams, teamSwitchTitle))
        );
    }

    /**
     * 处理单个已加入队伍的玩家状态重置
     */
    private void processJoinedPlayer(PlayerData data, ServerPlayer player, boolean shouldSwitchTeams, Component teamSwitchTitle) {
        // 基础状态重置：回血 + 冒险模式
        player.heal(player.getMaxHealth());
        player.setGameMode(GameType.ADVENTURE);

        // 队伍切换时：清空背包 + 发放工具包 + 发送提示
        if (shouldSwitchTeams) {
            clearPlayerInventory(player);
            givePlayerKits(player);
            sendPacketToJoinedPlayer(player, new ClientboundSetTitleTextPacket(teamSwitchTitle), true);
            return;
        }

        // 非切换时：死亡玩家重置背包，存活玩家重置弹药
        if (!data.isLiving()) {
            clearPlayerInventory(player);
            givePlayerKits(player);
        } else {
            resetGunAmmon(); // 注：原拼写Ammon可能为笔误（应为Ammo），保留原有逻辑
        }

        // 锁定商店槽位
        ShopCapability.getPlayerShopData(this, player.getUUID())
                .ifPresent(shopData -> shopData.lockShopSlots(player));
    }

    /**
     * 清理刀具缓存并同步商店数据
     */
    private void clearKnifeCacheAndSyncShopData() {
        knifeCache.clear();
        ShopCapability.syncShopData(this);
    }

    private void givePlayerKits(ServerPlayer player) {
        CapabilityMap.getTeamCapability(this,StartKitsCapability.class)
                .forEach((team, opt) -> {
                    if(team.hasPlayer(player.getUUID())){
                        opt.ifPresent(cap -> cap.givePlayerKits(player));
                    }
        });
    }

    public void teleportPlayerToMatchEndPoint(){
        if (this.matchEndTeleportPoint == null ) return;
        SpawnPointData data = this.matchEndTeleportPoint;
        this.getMapTeams().getJoinedPlayersWithSpec().forEach((uuid -> this.getPlayerByUUID(uuid).ifPresent(player->{
            teleportToPoint(player, data);
            player.setGameMode(GameType.ADVENTURE);
        })));
    }

    /**
     * 为爆破方随机分配C4炸弹给一名在线玩家
     * @see CompositionC4 C4物品实体类
     * @see #cleanupMap() 回合结束清理残留C4
     */
    public void giveBlastTeamBomb() {
        this.getCapabilityMap().get(DemolitionModeCapability.class)
                .flatMap(cap -> this.getMapTeams().getTeamByFixedName(cap.getDemolitionTeam())).ifPresent((team) -> {
            List<UUID> onlinePlayers = team.getOnlinePlayers();
            if (onlinePlayers.isEmpty()) {
                return;
            }
            // 清理队伍所有成员的C4
            team.getPlayerList().forEach(uuid ->
                    clearPlayerInventory(uuid, itemStack -> itemStack.getItem() instanceof CompositionC4)
            );

            // 随机选择一名在线玩家
            UUID selectedUuid = onlinePlayers.get(new Random().nextInt(onlinePlayers.size()));
            this.getPlayerByUUID(selectedUuid).ifPresent(player -> {
                // 添加C4并更新库存
                player.getInventory().add(BOItemRegister.C4.get().getDefaultInstance());
                team.sendMessage(Component.translatable("blockoffensive.map.cs.team.giveBomb", player.getDisplayName()).withStyle(ChatFormatting.GREEN));
                FPSMUtil.sortPlayerInventory(player);
                this.syncInventory(player);
            });
        });


    }

    public void giveAllPlayersKits() {
        CapabilityMap.getTeamCapability(this,StartKitsCapability.class)
                .forEach((team, opt) -> opt.ifPresent(cap -> {
                    team.getPlayersData().forEach(playerData -> {
                        playerData.getPlayer().ifPresent(player -> {
                            ArrayList<ItemStack> items = cap.getTeamKits();
                            player.getInventory().clearContent();
                            for (ItemStack item : items) {
                                ItemStack copy = item.copy();
                                DropType type = DropType.getItemDropType(copy);
                                if(knifeSelection.get() && !isKnifeSelected && type != DropType.THIRD_WEAPON){
                                    continue;
                                }
                                if(copy.getItem() instanceof ArmorItem armorItem){
                                    player.setItemSlot(armorItem.getEquipmentSlot(),copy);
                                }else{
                                    player.getInventory().add(copy);
                                }
                            }
                            FPSMUtil.sortPlayerInventory(player);
                        });
                    });
                }));
    }

    public void setPauseState(ServerPlayer player){
        if(!this.isStart) return;
        this.getMapTeams().getTeamByPlayer(player).flatMap(team -> team.getCapabilityMap().get(PauseCapability.class)).ifPresent(cap -> {
            if (cap.canPause() && !this.isPause) {
                cap.addPause();
                if (!this.isWaiting) {
                    this.sendAllPlayerMessage(Component.translatable("blockoffensive.map.cs.pause.nextRound.success").withStyle(ChatFormatting.GOLD), false);
                } else {
                    this.sendAllPlayerMessage(Component.translatable("blockoffensive.map.cs.pause.success").withStyle(ChatFormatting.GOLD), false);
                }
            } else {
                player.displayClientMessage(Component.translatable("blockoffensive.map.cs.pause.fail").withStyle(ChatFormatting.RED), false);
            }
        });
    }

    public void setUnPauseState(){
        this.isPause = false;
        this.currentPauseTime = 0;
    }

    private void startUnpauseVote(ServerPlayer serverPlayer) {
        if(this.voteObj == null){
            Component translation = Component.translatable("blockoffensive.cs.unpause");
            VoteObj unpause = new VoteObj(
                    "unpause",
                    Component.translatable("blockoffensive.map.vote.message",serverPlayer.getDisplayName(),translation),
                    15,
                    1F,
                    this::setUnPauseState,
                    ()->{},
                    this.getMapTeams().getJoinedUUID()
            );
            this.startVote(unpause);
            this.voteObj.processVote(serverPlayer,true);
        }else{
            Component translation = Component.translatable("blockoffensive.cs." + this.voteObj.getVoteTitle());
            serverPlayer.displayClientMessage(Component.translatable("blockoffensive.map.vote.fail.alreadyHasVote", translation).withStyle(ChatFormatting.RED),false);
        }
    }

    public void handleAgreeCommand(ServerPlayer serverPlayer){
        if(this.voteObj != null && this.voteObj.processVote(serverPlayer, true)){
            this.sendAllPlayerMessage(Component.translatable("blockoffensive.map.vote.agree",serverPlayer.getDisplayName()).withStyle(ChatFormatting.GREEN),false);
        }
    }

    private void handleDisagreeCommand(ServerPlayer serverPlayer) {
        if(this.voteObj != null && this.voteObj.processVote(serverPlayer, false)){
            this.sendAllPlayerMessage(Component.translatable("blockoffensive.map.vote.disagree",serverPlayer.getDisplayName()).withStyle(ChatFormatting.RED),false);
        }
    }

    public void sendAllPlayerMessage(Component message,boolean actionBar){
        this.getMapTeams().getJoinedPlayers().forEach(data -> data.getPlayer().ifPresent(player -> player.displayClientMessage(message,actionBar)));
    }

    @Override
    public void reset() {
        super.reset();
        this.isOvertime = false;
        this.isWaitingOverTimeVote = false;
        this.overCount = 0;
        this.isShopLocked = false;
        this.cleanupMap();
        this.getMapTeams().getJoinedPlayersWithSpec().forEach((uuid -> this.getPlayerByUUID(uuid).ifPresent(player->{
            this.getServerLevel().getServer().getScoreboard().removePlayerFromTeam(player.getScoreboardName());
            player.getInventory().clearContent();
            player.removeAllEffects();
        })));
        this.teleportPlayerToMatchEndPoint();
        this.sendPacketToAllPlayer(new FPSMatchStatsResetS2CPacket());
        this.isShopLocked = false;
        this.isError = false;
        this.isStart = false;
        this.isWaiting = false;
        this.isWaitingWinner = false;
        this.isWarmTime = false;
        this.isPause = false;
        this.currentRoundTime = 0;
        this.currentPauseTime = 0;
        this.isKnifeSelected = false;
        this.getMapTeams().getJoinedPlayers().forEach(data-> data.getPlayer().ifPresent(this::resetPlayerClientData));
        this.getMapTeams().reset();
    }

    public boolean checkCanPlacingBombs(String team){
        return CapabilityMap.getMapCapability(this,DemolitionModeCapability.class)
                .map(cap -> cap.getDemolitionTeam().equals(team))
                .orElse(false);
    }

    public void setBombEntity(@Nullable BlastBombEntity bomb) {
        CapabilityMap.getMapCapability(this,DemolitionModeCapability.class)
                .ifPresent(cap -> cap.setBombEntity(bomb));
    }

    public BlastBombState blastState() {
        return CapabilityMap.getMapCapability(this,DemolitionModeCapability.class)
                .map(DemolitionModeCapability::blastState)
                .orElse(BlastBombState.NONE);
    }

    public int getClientTime(){
        int time;
        if(this.isPause){
            time = pauseTime.get() - this.currentPauseTime;
        }else {
            if (this.isWaiting) {
                time = waitingTime.get() - this.currentPauseTime;
            } else if (this.isWarmTime) {
                time = warmUpTime.get() - this.currentPauseTime;
            } else if (this.isWaitingWinner) {
                time = winnerWaitingTime.get() - this.currentPauseTime;
            } else if(this.blastState() != BlastBombState.NONE){
                return -1;
            }else if (this.isStart) {
                time = roundTimeLimit.get() - this.currentRoundTime;
            }else {
                time = 0;
            }
        }
        return time;
    }

    /**
     * 同步游戏设置到客户端（比分/时间等）
     * @see CSGameSettingsS2CPacket
     */
    public void syncToClient() {
        ServerTeam ct = this.getCTTeam();
        ServerTeam t = this.getTTeam();
        CSGameSettingsS2CPacket packet = new CSGameSettingsS2CPacket(
                ct.getScores(),t.getScores(),
                this.getClientTime(),
                CSGameSettingsS2CPacket.GameFlags.of(
                        this.isDebug(),
                        this.isStart,
                        this.isError,
                        this.isPause,
                        this.isWaiting,
                        this.isWaitingWinner
                )
        );
        for(UUID uuid : this.getMapTeams().getJoinedPlayersWithSpec()){
            this.getPlayerByUUID(uuid).ifPresent(player-> {
                this.sendPacketToJoinedPlayer(player, packet, true);
                for (ServerTeam team : this.getMapTeams().getTeamsWithSpec()) {
                    for (UUID existingPlayerId : team.getPlayers().keySet()) {
                        team.getPlayerData(existingPlayerId).ifPresent(playerData -> {
                            var p1 = new GameTabStatsS2CPacket(existingPlayerId, playerData, team.name);
                            this.sendPacketToJoinedPlayer(player, p1, true);
                        });
                    }
                }
            });
        }

        Map<UUID, WeaponData> weaponDataMap = new HashMap<>();

        for (PlayerData data : this.getMapTeams().getJoinedPlayers()){
            Optional<ServerPlayer> optional = data.getPlayer();
            if(optional.isEmpty()) continue;
            ServerPlayer player = optional.get();
            Map<String, List<String>> weaponData = new HashMap<>();

            List<List<ItemStack>> items = new ArrayList<>();
            items.add(player.getInventory().items);
            items.add(player.getInventory().armor);
            items.add(player.getInventory().offhand);
            for (List<ItemStack> itemStacks : items) {
                for(ItemStack itemStack : itemStacks){
                    if(itemStack.isEmpty()) continue;
                    for (DropType dropType : DropType.values()) {
                        if(dropType.itemMatch().test(itemStack)){
                            weaponData.computeIfAbsent(dropType.name(), k -> new ArrayList<>()).add(itemStack.getHoverName().getString());
                            break;
                        }
                    }
                }
            }
            // carried
            weaponData.computeIfAbsent("CARRIED", k -> new ArrayList<>()).add(player.getMainHandItem().getHoverName().getString());

            boolean hasHelmet;
            int durability;
            Optional<BulletproofArmorAttribute> attribute = BulletproofArmorAttribute.getInstance(player);
            if(attribute.isPresent()){
                hasHelmet = attribute.get().hasHelmet();
                durability = attribute.get().getDurability();
            }else{
                hasHelmet = false;
                durability = 0;
            }
            weaponDataMap.put(player.getUUID(),new WeaponData(weaponData,hasHelmet,durability));
        }

        CSGameWeaponDataS2CPacket weaponDataS2CPacket = new CSGameWeaponDataS2CPacket(weaponDataMap);
        for (UUID uuid : this.getMapTeams().getSpecPlayers()){
            this.getPlayerByUUID(uuid).ifPresent(player-> {
                this.sendPacketToJoinedPlayer(player, weaponDataS2CPacket, true);
            });
        }

        if(!isShopLocked){
            this.syncShopInfo(true,getShopCloseTime());
        }
    }

    public void resetPlayerClientData(ServerPlayer serverPlayer){
        FPSMatchStatsResetS2CPacket packet = new FPSMatchStatsResetS2CPacket();
        FPSMatch.INSTANCE.send(PacketDistributor.PLAYER.with(()-> serverPlayer), packet);
    }

    public void resetGunAmmon(){
        this.getMapTeams().getJoinedPlayers().forEach((data)-> data.getPlayer().ifPresent(FPSMUtil::resetAllGunAmmo));
    }

    @Nullable
    public SpawnPointData getMatchEndTeleportPoint() {
        return matchEndTeleportPoint;
    }

    public void setMatchEndTeleportPoint(SpawnPointData matchEndTeleportPoint) {
        this.matchEndTeleportPoint = matchEndTeleportPoint;
    }

    public void handlePlayerDeathMessage(ServerPlayer player, DamageSource source) {
        Player attacker;
        if (source.getEntity() instanceof Player p) {
            attacker = p;
        } else if (source.getEntity() instanceof ThrowableItemProjectile throwable
                && throwable.getOwner() instanceof Player p) {
            attacker = p;
        } else {
            attacker = null;
        }

        if (attacker == null) return;

        ItemStack itemStack;
        if (source.getDirectEntity() instanceof ThrowableItemProjectile projectile) {
            itemStack = projectile.getItem();
        } else if (FPSMImpl.findCounterStrikeGrenadesMod()) {
            itemStack = CounterStrikeGrenadesCompat.getItemFromDamageSource(source);
            if (itemStack.isEmpty()) {
                itemStack = attacker.getMainHandItem();
            }
        } else {
            itemStack = attacker.getMainHandItem();
        }

        if (itemStack.getItem() instanceof IGun) return;

        giveEco(player, attacker, itemStack);

        DeathMessage.Builder builder = new DeathMessage.Builder(attacker, player, itemStack);
        Map<UUID, Float> hurtDataMap = this.getMapTeams().getDamageMap().get(player.getUUID());

        if (hurtDataMap != null && !hurtDataMap.isEmpty()) {
            hurtDataMap.entrySet().stream()
                    .filter(entry -> entry.getValue() > player.getMaxHealth() / 4)
                    .max(Map.Entry.comparingByValue())
                    .flatMap(entry -> this.getMapTeams().getTeamByPlayer(entry.getKey())
                            .flatMap(team -> team.getPlayerData(entry.getKey())))
                    .ifPresent(playerData -> {
                        if (!attacker.getUUID().equals(playerData.getOwner())) {
                            builder.setAssist(playerData.name(), playerData.getOwner());
                        }
                    });
        }

        DeathMessageS2CPacket killMessageS2CPacket = new DeathMessageS2CPacket(builder.build());
        this.sendPacketToAllPlayer(killMessageS2CPacket);
    }

    public void giveEco(ServerPlayer player, Player attacker, ItemStack itemStack) {
        if (knifeSelection.get() && !isKnifeSelected) return;

        ServerTeam killerTeam = this.getMapTeams().getTeamByPlayer(attacker).orElse(null);
        ServerTeam deadTeam = this.getMapTeams().getTeamByPlayer(player).orElse(null);
        if(killerTeam == null || deadTeam == null) {
            FPSMatch.LOGGER.error("CSGameMap {} -> killerTeam or deadTeam are null! : killer {} , dead {}",this.getMapName(),attacker.getDisplayName(),player.getDisplayName());
            return;
        }

        if (killerTeam.getFixedName().equals(deadTeam.getFixedName())){
            ShopCapability.getPlayerShopData(this,player.getUUID()).ifPresent(shopData -> {
                shopData.reduceMoney(300);
                attacker.displayClientMessage(Component.translatable("blockoffensive.kill.message.teammate",300),false);
            });
        }else{
            int reward = getRewardByItem(itemStack);
            ShopCapability.getPlayerShopData(this,player.getUUID()).ifPresent(shopData -> {
                shopData.addMoney(reward);
                attacker.displayClientMessage(Component.translatable("blockoffensive.kill.message.enemy",reward),false);
            });
        }
    }

    public void handlePlayerDeath(ServerPlayer player, @Nullable Entity fromEntity) {
        ServerPlayer from;
        if (fromEntity instanceof ServerPlayer fromPlayer) {
            Optional<BaseMap> optionalFromMap = FPSMCore.getInstance().getMapByPlayer(fromPlayer);
            if (optionalFromMap.isPresent() && optionalFromMap.get().equals(this)) {
                from = fromPlayer;
            } else {
                from = null;
            }
        } else {
            from = null;
        }

        if(this.isStart) {
            MapTeams teams = this.getMapTeams();
            teams.getTeamByPlayer(player).ifPresent(deadPlayerTeam->{
                CapabilityMap.getTeamCapability(deadPlayerTeam, ShopCapability.class)
                        .flatMap(ShopCapability::getShopSafe).ifPresent(shop -> {
                            shop.getDefaultAndPutData(player.getUUID());
                });

                this.sendPacketToJoinedPlayer(player,new ShopStatesS2CPacket(false,0,0),true);
                deadPlayerTeam.getPlayerData(player.getUUID()).ifPresent(data->{
                    data.addDeaths();
                    data.setLiving(false);
                    // 清除c4,并掉落c4
                    dropC4(player);
                    // 清除玩家所属子弹
                    this.getServerLevel().getEntitiesOfClass(EntityKineticBullet.class,mapArea.getAABB())
                            .stream()
                            .filter(entityKineticBullet -> entityKineticBullet.getOwner() != null && entityKineticBullet.getOwner().getUUID().equals(player.getUUID()))
                            .toList()
                            .forEach(Entity::discard);
                    // 清除拆弹工具,并掉落拆弹工具
                    int ik = player.getInventory().clearOrCountMatchingItems((i) -> i.getItem() instanceof BombDisposalKit, -1, player.inventoryMenu.getCraftSlots());
                    if (ik > 0) {
                        player.drop(new ItemStack(BOItemRegister.BOMB_DISPOSAL_KIT.get(), 1), false, false).setGlowingTag(true);
                    }
                    FPSMUtil.playerDeadDropWeapon(player,true);
                    player.getInventory().clearContent();
                    player.heal(player.getMaxHealth());
                    player.setGameMode(GameType.SPECTATOR);
                    player.setRespawnPosition(player.level().dimension(),player.getOnPos().above(),0,true,false);
                    this.setBystander(player);
                    this.syncInventory(player);
                });
            });

            Map<UUID, Float> hurtDataMap = teams.getDamageMap().get(player.getUUID());
            if (hurtDataMap != null && !hurtDataMap.isEmpty()) {
                hurtDataMap.entrySet().stream()
                        .filter(entry -> entry.getValue() > player.getMaxHealth() / 4)
                        .sorted(Map.Entry.<UUID, Float>comparingByValue().reversed())
                        .limit(1)
                        .findAny().ifPresent(assist->{
                            UUID assistId = assist.getKey();
                            teams.getTeamByPlayer(assistId)
                                    .flatMap(assistPlayerTeam -> assistPlayerTeam.getPlayerData(assistId))
                                    .ifPresent(assistData -> {
                                        if (from != null && from.getUUID().equals(assistId)) return;
                                        assistData.addAssist();
                                    });
                        });

            }

            if(from == null) return;
            teams.getTeamByPlayer(from)
                    .flatMap(killerPlayerTeam -> killerPlayerTeam.getPlayerData(from.getUUID()))
                    .ifPresent(PlayerData::addKills);
        }
    }

    public void handleChatCommand(String rawText,ServerPlayer player){
        String command = rawText.toLowerCase(Locale.US);
        COMMANDS.forEach((k,v)->{
            if (command.equals(k) && rawText.length() == k.length()){
                v.accept(this,player);
            }
        });
    }

    @Override
    public CSGameMap getMap() {
        return this;
    }

    public @NotNull ServerTeam getTTeam(){
        return this.tTeam;
    }

    public @NotNull ServerTeam getCTTeam(){
        return this.ctTeam;
    }

    @Override
    public boolean reload(){
        if (!super.reload()) return false;
        reset();
        loadConfig();
        return true;
    }

    @Override
    public Collection<Setting<?>> settings() {
        return settings;
    }

    @Override
    public <I> Setting<I> addSetting(Setting<I> setting) {
        settings.add(setting);
        return setting;
    }

    public void read() {
        FPSMCore.getInstance().registerMap(this.getGameType(),this);
    }

    public static int getRewardByItem(ItemStack itemStack){
        if(FPSMImpl.findEquipmentMod() && LrtacticalCompat.isKnife(itemStack)){
            return 1500;
        }else{
            if(itemStack.getItem() instanceof IGun iGun){
                return gerRewardByGunId(iGun.getGunId(itemStack));
            }else{
                return 300;
            }
        }
    }

    public static int gerRewardByGunId(ResourceLocation gunId){
        Optional<GunTabType> optional = FPSMUtil.getGunTypeByGunId(gunId);
        if(optional.isPresent()){
            switch(optional.get()){
                case SHOTGUN -> {
                    return 900;
                }
                case SMG -> {
                    return 600;
                }
                case SNIPER -> {
                    return 100;
                }
                default -> {
                    return 300;
                }
            }
        }else{
            return 300;
        }
    }

    public Map<String,List<SpawnPointData>> getAllSpawnPoints(){
        Map<String,List<SpawnPointData>> allSpawnPoints = new HashMap<>();

        for (ServerTeam team : this.getMapTeams().getNormalTeams()){
            team.getCapabilityMap().get(SpawnPointCapability.class).ifPresent(cap->{
                allSpawnPoints.put(team.name,cap.getSpawnPointsData());
            });
        }

        return allSpawnPoints;
    }

    public boolean setTeamSpawnPoints(){
        for (ServerTeam team : this.getMapTeams().getNormalTeams()){
            Optional<SpawnPointCapability> capability = team.getCapabilityMap().get(SpawnPointCapability.class);
            if(capability.isPresent()){
                SpawnPointCapability cap = capability.get();
                if(!cap.randomSpawnPoints()){
                    return false;
                }
            }
        }
        return true;
    }

    public List<AreaData> getBombAreaData() {
        return this.getCapabilityMap().get(DemolitionModeCapability.class).map(DemolitionModeCapability::getBombAreaData).orElse(Collections.emptyList());
    }

    public boolean checkPlayerIsInBombArea(@NotNull Player player) {
        return this.getCapabilityMap().get(DemolitionModeCapability.class).map(cap->cap.checkPlayerIsInBombArea(player)).orElse(false);
    }


    public enum WinnerReason{
        TIME_OUT(3250),
        ACED(3250),
        DEFUSE_BOMB(3500),
        DETONATE_BOMB(3500);
        public final int winMoney;

        WinnerReason(int winMoney) {
            this.winMoney = winMoney;
        }
    }

}
