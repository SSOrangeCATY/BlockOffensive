package com.phasetranscrystal.blockoffensive.map;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.phasetranscrystal.blockoffensive.BOConfig;
import com.phasetranscrystal.blockoffensive.BlockOffensive;
import com.phasetranscrystal.blockoffensive.data.MvpReason;
import com.phasetranscrystal.blockoffensive.event.CSGamePlayerGetMvpEvent;
import com.phasetranscrystal.blockoffensive.event.CSGameRoundEndEvent;
import com.phasetranscrystal.blockoffensive.item.BOItemRegister;
import com.phasetranscrystal.blockoffensive.item.BombDisposalKit;
import com.phasetranscrystal.blockoffensive.item.CompositionC4;
import com.phasetranscrystal.blockoffensive.map.team.capability.ColoredPlayerCapability;
import com.phasetranscrystal.blockoffensive.net.*;
import com.phasetranscrystal.blockoffensive.net.bomb.BombDemolitionProgressS2CPacket;
import com.phasetranscrystal.blockoffensive.net.mvp.MvpHUDCloseS2CPacket;
import com.phasetranscrystal.blockoffensive.net.mvp.MvpMessageS2CPacket;
import com.phasetranscrystal.blockoffensive.net.shop.ShopStatesS2CPacket;
import com.phasetranscrystal.blockoffensive.net.spec.BombFuseS2CPacket;
import com.phasetranscrystal.blockoffensive.sound.BOSoundRegister;
import com.phasetranscrystal.blockoffensive.sound.MVPMusicManager;
import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.common.capability.map.DemolitionModeCapability;
import com.phasetranscrystal.fpsmatch.common.capability.map.GameEndTeleportCapability;
import com.phasetranscrystal.fpsmatch.common.capability.team.*;
import com.phasetranscrystal.fpsmatch.common.drop.DropType;
import com.phasetranscrystal.fpsmatch.common.packet.*;
import com.phasetranscrystal.fpsmatch.core.*;
import com.phasetranscrystal.fpsmatch.core.capability.CapabilityMap;
import com.phasetranscrystal.fpsmatch.core.capability.map.MapCapability;
import com.phasetranscrystal.fpsmatch.core.capability.team.TeamCapability;
import com.phasetranscrystal.fpsmatch.core.data.AreaData;
import com.phasetranscrystal.fpsmatch.core.data.PlayerData;
import com.phasetranscrystal.fpsmatch.core.data.Setting;
import com.phasetranscrystal.fpsmatch.core.entity.BlastBombEntity;
import com.phasetranscrystal.fpsmatch.core.map.*;
import com.phasetranscrystal.fpsmatch.core.persistence.FPSMDataManager;
import com.phasetranscrystal.fpsmatch.core.shop.FPSMShop;
import com.phasetranscrystal.fpsmatch.core.team.BaseTeam;
import com.phasetranscrystal.fpsmatch.core.team.ServerTeam;
import com.phasetranscrystal.fpsmatch.core.team.MapTeams;
import com.phasetranscrystal.fpsmatch.core.team.TeamData;
import com.phasetranscrystal.fpsmatch.util.FPSMUtil;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Team;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * 反恐精英（CS）模式地图核心逻辑类
 * 管理回合制战斗、炸弹逻辑、商店系统、队伍经济、玩家装备等核心机制
 */
@Mod.EventBusSubscriber(modid = BlockOffensive.MODID,bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CSGameMap extends CSMap{
    public static final String TYPE = "cs";
    /**
     * Codec序列化配置（用于地图数据保存/加载）
     */
    public static final Codec<CSGameMap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            // 基础地图数据
            Codec.STRING.fieldOf("mapName").forGetter(CSGameMap::getMapName),
            AreaData.CODEC.fieldOf("mapArea").forGetter(CSGameMap::getMapArea),
            ResourceLocation.CODEC.fieldOf("serverLevel").forGetter(map -> map.getServerLevel().dimension().location()),
            CapabilityMap.Wrapper.DATA_CODEC.fieldOf("capabilities").forGetter(csGameMap -> csGameMap.getCapabilityMap().getData().data()),
            // 队伍数据
            Codec.unboundedMap(
                    Codec.STRING,
                    CapabilityMap.Wrapper.CODEC
            ).fieldOf("teams").forGetter(csGameMap -> csGameMap.getMapTeams().getData())
    ).apply(instance, CSGameMap::new));

    public static void save(FPSMDataManager manager){
        FPSMCore.getInstance().getMapByClass(CSGameMap.class)
                .forEach((map -> {
                    map.saveConfig();
                    manager.saveData(map,map.getMapName(),false);
                }));
    }
    private static final List<Class<? extends MapCapability>> MAP_CAPABILITIES = List.of(DemolitionModeCapability.class, GameEndTeleportCapability.class);
    private static final List<Class<? extends TeamCapability>> TEAM_CAPABILITIES = List.of(PauseCapability.class, CompensationCapability.class, TeamSwitchRestrictionCapability.class, ShopCapability.class, StartKitsCapability.class, ColoredPlayerCapability.class);

    private Setting<Integer> winnerRound;
    private Setting<Integer> overtimeRound;
    private Setting<Integer> pauseTime;
    private Setting<Integer> winnerWaitingTime;
    private Setting<Integer> warmUpTime;
    private Setting<Integer> waitingTime;
    private Setting<Integer> roundTimeLimit;
    private Setting<Integer> startMoney;
    private Setting<Integer> defaultLoserEconomy;
    private Setting<Integer> defuseBonus;
    private Setting<Integer> compensationBase;
    private Setting<Integer> tDeathRewardPer;
    private Setting<Integer> closeShopTime;
    private Setting<Boolean> knifeSelection;
    private Setting<Integer> c4InstantKillRadius;

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
    private boolean roundStarted = false;

    private final Map<UUID,Integer> knifeCache = new HashMap<>();

    /**
     * 构造函数：创建CS地图实例
     * @param serverLevel 服务器世界实例
     * @param mapName 地图名称
     * @param areaData 地图区域数据
     * @see #addTeam(TeamData) 初始化时自动添加CT和T阵营
     */
    public CSGameMap(ServerLevel serverLevel, String mapName, AreaData areaData) {
        super(serverLevel,mapName,areaData, MAP_CAPABILITIES,TEAM_CAPABILITIES);
        this.registerCommands();
        CapabilityMap.getMapCapability(this, DemolitionModeCapability.class).ifPresent(cap -> cap.setDemolitionTeam(this.getT()));
    }

    private CSGameMap(String mapName,AreaData areaData,ResourceLocation serverLevel, Map<String, JsonElement> capabilities, Map<String, CapabilityMap.Wrapper> teams) {
        this(FPSMCore.getInstance().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,serverLevel)), mapName, areaData);
        this.getCapabilityMap().write(capabilities);
        this.getMapTeams().writeData(teams);
    }

    private void registerCommands(){
        this.registerCommand("p", this::setPauseState);
        this.registerCommand("pause", this::setPauseState);
        this.registerCommand("unpause", this::startUnpauseVote);
        this.registerCommand("up", this::startUnpauseVote);
        this.registerCommand("agree", this::handleAgreeCommand);
        this.registerCommand("a", this::handleAgreeCommand);
        this.registerCommand("disagree", this::handleDisagreeCommand);
        this.registerCommand("da", this::handleDisagreeCommand);
        this.registerCommand("d", this::handleDropKnifeCommand);
        this.registerCommand("drop", this::handleDropKnifeCommand);

        if(!FMLEnvironment.production){
            this.registerCommand("debug_1",(p)->{
                getCT().setScores(winnerRound.get() - 2);
                getT().setScores(winnerRound.get() - 1);
            });

            this.registerCommand("debug_2",(p)->{
                this.switchTeams();
            });

            this.registerCommand("debug_3",(p)->{
                p.displayClientMessage(Component.literal("team: " + this.getMapTeams().getTeamByPlayer(p).map(t->t.name).orElse("none")),false);
            });
        }
    }

    /**
     * 添加队伍并初始化商店系统
     * @param data 队伍数据
     * @see FPSMShop 每个队伍拥有独立商店实例
     */
    @Override
    public ServerTeam addTeam(TeamData data){
        ServerTeam team = super.addTeam(data);
        CapabilityMap.getTeamCapability(this,ShopCapability.class).forEach((t,opt)->{
                opt.ifPresent(cap -> cap.initialize("cs",startMoney.get()));
        });
        return team;
    }

    @Override
    public void setup() {
        winnerRound = this.addSetting("winnerRound",13); // 13回合
        overtimeRound = this.addSetting("overtimeRound",3); // 3回合
        pauseTime = this.addSetting("pauseTime",1200); // 60秒
        winnerWaitingTime = this.addSetting("winnerWaitingTime",160);
        warmUpTime = this.addSetting("warmUpTime",1200);
        waitingTime = this.addSetting("waitingTime",300);
        roundTimeLimit = this.addSetting("roundTimeLimit",2300);
        startMoney = this.addSetting("startMoney",800);
        defaultLoserEconomy = this.addSetting("defaultLoserEconomy",1400);
        defuseBonus = this.addSetting("defuseBonus",600);
        compensationBase = this.addSetting("compensationBase",500);
        tDeathRewardPer = this.addSetting("tDeathRewardPer",50);
        closeShopTime = this.addSetting("closeShopTime",200);
        knifeSelection = this.addSetting("knifeSelection",false);
        c4InstantKillRadius = this.addSetting("c4InstantKillRadius",20);
    }

    @Override
    public Function<ServerPlayer, Boolean> getPlayerCanOpenShop() {
        return player -> !isShopLocked;
    }

    public int getNextRoundMinMoney(ServerTeam team){
        int defaultEconomy = 1400;
        int compensation = 500;
        int compensationFactor = Math.min(0, getCompensation(team).getFactor() - 2);
        // 计算失败补偿
        return defaultEconomy + compensation * compensationFactor;
    }

    public CompensationCapability getCompensation(ServerTeam team){
        return team.getCapabilityMap().get(CompensationCapability.class).orElse(null);
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
        super.tick();
        if(isStart && !checkPauseTime()){
            // 暂停 / 热身 / 回合开始前的等待时间
            if (!checkWarmUpTime() & !checkWaitingTime()) {
                if(!isRoundTimeEnd()){
                    if(!this.isDebug()){
                        boolean flag = this.getMapTeams().getJoinedPlayers().size() != 1;
                        switch (this.blastState()){
                            case TICKING : this.checkBlastingVictory(); break;
                            case EXPLODED : this.roundVictory(this.getT(),WinnerReason.DETONATE_BOMB); break;
                            case DEFUSED : this.roundVictory(this.getCT(),WinnerReason.DEFUSE_BOMB); break;
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
                        this.roundVictory(this.getCT(),WinnerReason.TIME_OUT);
                    }else if(this.currentPauseTime >= winnerWaitingTime.get()){
                        this.startNewRound();
                    }
                }
            }
        }
    }

    @Override
    public void join(String teamName, ServerPlayer player) {
        super.join(teamName, player);
    }

    @Override
    public void leave(ServerPlayer player) {
        this.sendPacketToAllPlayer(new CSTabRemovalS2CPacket(player.getUUID()));
        super.leave(player);
    }

    @Override
    public String getGameType() {
        return TYPE;
    }


    /**
     * 开始新游戏（初始化所有玩家状态）
     * 核心优化：拆分职责、复用Setting配置、消除魔法值、简化嵌套逻辑
     * @see #giveBlastTeamBomb() 给爆破方分配C4
     */
    public boolean start() {
        if (!super.start()) {
            return false;
        }

        MapTeams mapTeams = getMapTeams();

        setTeamNameColors();
        if (this.isError) return false;

        resetGameCoreState();

        if (!this.setTeamSpawnPoints()) {
            this.reset();
            return false;
        }

        cleanupMap();
        mapTeams.startNewRound();
        mapTeams.resetLivingPlayers();
        initializeTeams(mapTeams);

        initializeAllJoinedPlayers(mapTeams);

        handleKnifeSelectionPhase();

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
     * 重置游戏核心状态（加时、计数、等待状态等）
     */
    public void resetGameCoreState() {
        this.isOvertime = false;
        this.overCount = 0;
        this.isWaitingOverTimeVote = false;
        this.isStart = true;
        this.isWaiting = true;
        this.isWaitingWinner = false;
        this.currentPauseTime = 0;
        this.isShopLocked = false;
    }

    /**
     * 初始化队伍状态（分数、补偿因子、玩家数据重置）
     */
    private void initializeTeams(MapTeams mapTeams) {
        mapTeams.getNormalTeams().forEach(team -> {
            team.setScores(0);
            team.getCapabilityMap().get(CompensationCapability.class).ifPresentOrElse(cap->cap.setFactor(0),()->{
                FPSMatch.LOGGER.error("CSGameMap {} Compensation fail set to {}",this.getMapName(), 0);
            });
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
            this.clearInventory(player);
            this.teleportPlayerToReSpawnPoint(player);
        });
    }

    /**
     * 处理刀选阶段逻辑
     */
    private void handleKnifeSelectionPhase() {
        boolean isKnifeSelectionPhase = knifeSelection.get() && !isKnifeSelected;
        this.isShopLocked = isKnifeSelectionPhase;

        int shopCloseTime = this.closeShopTime.get();
        syncShopInfo(!isKnifeSelectionPhase, shopCloseTime);

        this.giveAllPlayersKits((type)-> !isKnifeSelectionPhase || type == DropType.THIRD_WEAPON);

        if (!isKnifeSelectionPhase) {
            this.giveBlastTeamBomb();
            ShopCapability.setPlayerMoney(this, this.startMoney.get());
            ShopCapability.syncShopData(this);
        }
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
                if(this.getVote() != null && this.getVote().getVoteTitle().equals("unpause")){
                    this.setVote(null);
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

            if(!roundStarted){
                roundStarted = true;
                this.onRoundStarted();
            }
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
            this.roundVictory(this.getCT(),WinnerReason.ACED);
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
            this.roundVictory(this.getT(),WinnerReason.ACED);
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

    @Override
    public Team.Visibility nameTagVisibility() {
        return Team.Visibility.HIDE_FOR_OTHER_TEAMS;
    }

    @Override
    public boolean isError() {
        return isError;
    }

    @Override
    public boolean isPause() {
        return isPause;
    }

    @Override
    public boolean isWaiting() {
        return isWaiting;
    }

    @Override
    public boolean isWaitingWinner() {
        return isWaitingWinner;
    }

    @Override
    public boolean canGiveEconomy() {
        return this.isStart;
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
     * 统一发送金钱奖励消息
     */
    private void sendMoneyRewardMessage(ServerPlayer player, int amount, String reasonDesc) {
        player.sendSystemMessage(Component.translatable(
                "blockoffensive.map.cs.reward.money",
                amount,
                reasonDesc
        ));
    }


    private void onRoundStarted(){
        this.sendNewRoundVoice();
    }

    /**
     * 处理回合胜利逻辑
     * @param winnerTeam 获胜队伍
     * @param reason 胜利原因（如炸弹拆除/爆炸）
     * @see #checkMatchPoint() 检查赛点状态
     * @see MVPMusicManager MVP音乐播放逻辑
     */
    private void roundVictory(@NotNull ServerTeam winnerTeam, @NotNull WinnerReason reason) {
        if (handleKnifeRoundSpecialCase(winnerTeam)) {
            return;
        }

        if (isWaitingWinner) {
            return;
        }

        MapTeams mapTeams = getMapTeams();
        isWaitingWinner = true;

        MvpReason mvpReason = processMvpLogic(winnerTeam, mapTeams);

        sendPacketToAllPlayer(new MvpMessageS2CPacket(mvpReason));
        MinecraftForge.EVENT_BUS.post(new CSGameRoundEndEvent(this, winnerTeam, reason));

        processRoundScoreAndOvertimeVote(winnerTeam, mapTeams);

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
     * 刀局胜利处理
     */
    private void knifeRoundVictory(ServerTeam winnerTeam) {
        Component translation = Component.translatable("blockoffensive.cs.knife.selection");
        Component message = Component.translatable("blockoffensive.map.vote.message","System",translation);
        pause();
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
        return this.getVote() != null && this.getVote().getVoteTitle().equals("knife");
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
                .setPlayerName(mapTeams.playerName.get(mvpData.uuid()).copy())
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
    private void processEconomicRewards(@NotNull ServerTeam winTeam, @NotNull WinnerReason reason, @NotNull MapTeams mapTeams) {
        List<ServerTeam> loseTeams = mapTeams.getNormalTeams(winTeam);

        // 检查连胜情况
        checkLoseStreaks(winTeam,loseTeams);

        //胜利方经济奖励
        processWinnerEconomicReward(winTeam, reason);

        //失败方经济奖励
        loseTeams.forEach(loserTeam -> processLoserEconomicReward(loserTeam, reason));

        //CT队伍额外奖励
        processCTTeamExtraReward();
    }

    /**
     * 处理胜利方经济奖励
     */
    private void processWinnerEconomicReward(@NotNull ServerTeam winTeam,WinnerReason reason) {
        winTeam.getPlayerList().forEach(uuid -> {
            getShopDataSafely(uuid).ifPresent(shopData -> {
                shopData.addMoney(reason.winMoney);
            });

            getPlayerByUUID(uuid).ifPresent(player ->
                    sendMoneyRewardMessage(player, reason.winMoney, reason.name())
            );
        });
    }

    /**
     * 处理失败方经济奖励
     */
    private void processLoserEconomicReward(@NotNull ServerTeam loserTeam, @NotNull WinnerReason reason) {
        int compensationFactor = getCompensation(loserTeam).getFactor();
        boolean isDefuseBonusApplicable = checkCanPlacingBombs(loserTeam.getFixedName())
                && reason == WinnerReason.DEFUSE_BOMB;

        // 基础经济 + 拆弹额外奖励
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
        // 非超时情况直接发放
        if (reason != WinnerReason.TIME_OUT) {
            return true;
        }

        // 超时情况：仅给死亡玩家发放
        return loserTeam.getPlayerData(uuid)
                .map(data -> !data.isLivingOnServer())
                .orElse(false); // 无玩家数据时不发放
    }

    private void processCTTeamExtraReward() {
        ServerTeam tTeam = getT();
        ServerTeam ctTeam = getCT();

        long deadTCount = tTeam.getPlayersData().stream()
                .filter(data -> !data.isLivingOnServer())
                .count();

        int extraReward = (int) deadTCount * tDeathRewardPer.get();
        if (extraReward <= 0) {
            return;
        }

        ctTeam.getPlayerList().forEach(uuid -> {
            // 更新商店数据
            getShopDataSafely(uuid).ifPresent(shopData -> {
                shopData.addMoney(extraReward);
            });

            // 发送团队奖励消息
            ctTeam.sendMessage(Component.translatable("blockoffensive.map.cs.reward.team", extraReward, deadTCount));
        });
    }

    private void checkLoseStreaks(ServerTeam winTeam, @NotNull List<ServerTeam> loseTeams) {
        winTeam.getCapabilityMap().get(CompensationCapability.class).ifPresentOrElse(cap->{
            cap.reduce(2);
        },()->FPSMatch.LOGGER.error("Failed to reduce Compensation capability"));

        loseTeams.forEach(team -> {
            team.getCapabilityMap().get(CompensationCapability.class).ifPresentOrElse(cap->{
                cap.add(1);
            },()->FPSMatch.LOGGER.error("Failed to add Compensation capability"));
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
            this.roundStarted = false;
            this.cleanupMap();
            this.sendRoundDamageMessage();
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
        int ctScore = this.getCT().getScores();
        int tScore = this.getT().getScores();
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

    @Override
    public void victory() {
        reset();
    }

    @Override
    public boolean victoryGoal() {
        if (this.isWaitingOverTimeVote || this.isDebug()) {
            return false;
        }

        ServerTeam winnerTeam = null;
        int requiredScore = calculateRequiredScore();

        for (ServerTeam team : this.getMapTeams().getNormalTeams()) {
            if (team.getScores() >= requiredScore) {
                winnerTeam = team;
                break;
            }
        }

        if (winnerTeam == null) {
            return false;
        }

        handleVictory(winnerTeam);
        return true;
    }

    private int calculateRequiredScore() {
        int winner = winnerRound.get();
        if (!isOvertime) {
            return winner;
        }

        return winner + ((this.overCount + 1) * this.overtimeRound.get()) + 1;
    }

    private void handleVictory(ServerTeam winnerTeam) {
        // 发送胜利消息
        this.sendVictoryMessage(
                Component.translatable("map.cs.message.victory.head",
                                winnerTeam.name.toUpperCase(Locale.US))
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Comparator.comparingDouble(PlayerData::getDamage).reversed()
        );

        // 发送胜利标题
        this.sendAllPlayerTitle(
                Component.translatable("blockoffensive.map.cs.winner." + winnerTeam.name + ".message")
                        .withStyle(winnerTeam.name.equals("ct") ?
                                ChatFormatting.DARK_AQUA : ChatFormatting.YELLOW),
                null
        );
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
        this.isWaiting = true;

        this.getMapTeams().getNormalTeams().forEach(team-> {
            team.resetCapabilities();

            team.getPlayers().forEach((uuid, playerData) -> {
                playerData.setLiving(false);
            });

            team.getCapabilityMap().get(ShopCapability.class)
                    .flatMap(ShopCapability::getShopSafe).ifPresent(shop -> {
                        shop.setStartMoney(10000);
                        shop.resetPlayerData(true);
            });
        });
        this.startNewRound();
    }

    public boolean cleanupMap() {
        if (!super.cleanupMap()) {
            return false;
        }

        MapTeams mapTeams = getMapTeams();
        int ctScore = getCT().getScores();
        int tScore = getT().getScores();

        sendPhysicsRagdollRemovalPacket(PxRagdollRemovalCompatS2CPacket.ALL);
        cleanupSpecificEntities();
        notifySpectatorsOfBombFuse();

        boolean shouldSwitchTeams = handleOvertimeAndTeamSwitch(ctScore, tScore, mapTeams);

        resetMapBaseState();

        resetAllJoinedPlayersState(mapTeams, shouldSwitchTeams);

        knifeCache.clear();
        ShopCapability.syncShopData(this);

        return true;
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

    private void pause(){
        isPause = true;
        currentPauseTime = 0;
        isWaiting = true;
    }

    /**
     * 处理加时赛投票与队伍切换逻辑
     * @return 是否需要切换队伍
     */
    private boolean handleOvertimeAndTeamSwitch(int ctScore, int tScore, MapTeams mapTeams) {
        // 清空暂停计时（两个分支都需要）
        currentPauseTime = 0;

        // 计算关键分数阈值
        int scoreToTriggerOvertime = calculateScoreToTriggerOvertime();
        int maxNormalScore = winnerRound.get() - 1;

        if (!isOvertime) {
            return handleNormalTimeLogic(ctScore, tScore, maxNormalScore, scoreToTriggerOvertime);
        } else {
            return handleOvertimeLogic(ctScore, tScore, maxNormalScore);
        }
    }

    /**
     * 计算触发加时赛的分数阈值
     */
    private int calculateScoreToTriggerOvertime() {
        // 假设winnerRound是获胜所需轮数（如13），则触发加时赛的分数是winnerRound-1（如12）
        return winnerRound.get() - 1;
    }

    /**
     * 处理正常比赛时间的逻辑
     */
    private boolean handleNormalTimeLogic(int ctScore, int tScore, int maxNormalScore, int overtimeTriggerScore) {
        // 检查是否触发加时赛：双方都达到触发分数
        if (ctScore == overtimeTriggerScore && tScore == overtimeTriggerScore) {
            startOvertimeSequence();
            return false; // 不切换队伍，进入加时赛投票
        }

        // 计算当前总局数
        int totalRoundsPlayed = getMapTeams().getNormalTeams().stream()
                .mapToInt(BaseTeam::getScores)
                .sum();

        // 是否达到换边条件：总局数达到换边阈值
        boolean shouldSwitchTeams = totalRoundsPlayed == maxNormalScore;

        if (shouldSwitchTeams) {
            isWaiting = true;
            switchTeams();
        }

        return shouldSwitchTeams;
    }

    /**
     * 开始加时赛序列
     */
    private void startOvertimeSequence() {
        startOvertimeVote();
        setBombEntity(null);
        currentRoundTime = 0;
        isPause = true;
        // currentPauseTime = 0; 已在主方法开头设置
    }

    /**
     * 处理加时赛逻辑
     */
    private boolean handleOvertimeLogic(int ctScore, int tScore, int maxNormalScore) {
        int overtimeRound = this.overtimeRound.get();

        // 计算进入加时赛后的总局数
        int totalRoundsInOvertime = calculateTotalRoundsInOvertime(ctScore, tScore, maxNormalScore);

        // 检查是否需要换边：每overtimeRound局换一次
        boolean shouldSwitchTeams = shouldSwitchTeamsInOvertime(totalRoundsInOvertime, overtimeRound);

        if (shouldSwitchTeams) {
            isWaiting = true;
            switchTeams();

            // 检查是否应该增加加时赛轮次
            if (shouldIncreaseOvertimeCount(totalRoundsInOvertime, overtimeRound, ctScore, tScore, maxNormalScore)) {
                overCount++;
            }
        }

        return shouldSwitchTeams;
    }

    /**
     * 计算进入加时赛后的总局数
     */
    private int calculateTotalRoundsInOvertime(int ctScore, int tScore, int maxNormalScore) {
        // 总局数减去正常比赛最大可能分数（双方都达到触发加时赛的分数）
        return (ctScore + tScore) - (maxNormalScore * 2);
    }

    /**
     * 判断加时赛中是否需要换边
     */
    private boolean shouldSwitchTeamsInOvertime(int totalRoundsInOvertime, int overtimeRound) {
        // 每overtimeRound局换一次边，且至少进行了一局加时赛
        return totalRoundsInOvertime > 0 && totalRoundsInOvertime % overtimeRound == 0;
    }

    /**
     * 判断是否应该增加加时赛计数
     */
    private boolean shouldIncreaseOvertimeCount(int totalRoundsInOvertime, int overtimeRound,
                                                int ctScore, int tScore, int maxNormalScore) {
        // 只有在完成第一个加时赛段时才考虑增加计数
        if (overCount == 0 && totalRoundsInOvertime != overtimeRound) {
            return false;
        }

        // 计算当前加时赛段的分数上限
        int currentOvertimeScoreCap = calculateCurrentOvertimeScoreCap(maxNormalScore, overtimeRound);

        // 如果双方都未达到当前加时赛段的分数上限，则增加计数
        return ctScore < currentOvertimeScoreCap && tScore < currentOvertimeScoreCap;
    }

    /**
     * 计算当前加时赛段的分数上限
     * 公式解释：正常比赛最高分 + 已完成的加时赛局数 + (正常比赛最高分 + 1)
     * 例如：12 + (3 * 0) + (3 + 1) = 16
     * 表示第一个加时赛段需要达到16分才能获胜
     */
    private int calculateCurrentOvertimeScoreCap(int maxNormalScore, int overtimeRound) {
        return maxNormalScore + (overtimeRound * overCount) + (overtimeRound + 1);
    }

    /**
     * 重置地图基础状态（炸弹、回合时间、商店锁定）
     */
    public void resetMapBaseState() {
        setBombEntity(null);
        currentRoundTime = 0;
        isShopLocked = false;
    }

    /**
     * 重置所有已加入队伍的玩家状态
     */
    private void resetAllJoinedPlayersState(MapTeams mapTeams, boolean shouldSwitchTeams) {
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
        player.heal(player.getMaxHealth());
        player.setGameMode(GameType.ADVENTURE);

        if (shouldSwitchTeams) {
            clearInventory(player);
            givePlayerKits(player);
            sendPacketToJoinedPlayer(player, new ClientboundSetTitleTextPacket(teamSwitchTitle), true);
            return;
        }

        if (!data.isLiving()) {
            clearInventory(player);
            givePlayerKits(player);
        } else {
            resetGunAmmo();
        }

        ShopCapability.getPlayerShopData(this, player.getUUID())
                .ifPresent(shopData -> shopData.lockShopSlots(player));
    }


    @Override
    public void givePlayerKits(ServerPlayer player) {
        CapabilityMap.getTeamCapability(this,StartKitsCapability.class)
                .forEach((team, opt) -> {
                    if(team.hasPlayer(player.getUUID())){
                        opt.ifPresent(cap -> cap.givePlayerKits(player));
                    }
        });
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
                    clearInventory(uuid, itemStack -> itemStack.getItem() instanceof CompositionC4)
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
        if(this.getVote() == null){
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
            this.getVote().processVote(serverPlayer,true);
        }else{
            Component translation = Component.translatable("blockoffensive.cs." + this.getVote().getVoteTitle());
            serverPlayer.displayClientMessage(Component.translatable("blockoffensive.map.vote.fail.alreadyHasVote", translation).withStyle(ChatFormatting.RED),false);
        }
    }

    private void handleDropKnifeCommand(ServerPlayer player) {
        List<ItemStack> list = FPSMUtil.searchInventoryForType(player.getInventory(),DropType.THIRD_WEAPON);
        int currentKnives = knifeCache.getOrDefault(player.getUUID(), 0);
        if(!list.isEmpty() && currentKnives < 5){
            knifeCache.put(player.getUUID(), currentKnives + 1);
            FPSMUtil.playerDropMatchItem(player,list.get(0).copy());
        }
    }

    @Override
    public void reset() {
        super.reset();
        MapTeams mapTeams = this.getMapTeams();
        this.isOvertime = false;
        this.isWaitingOverTimeVote = false;
        this.overCount = 0;
        this.isShopLocked = false;
        this.roundStarted = false;
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
        mapTeams.getJoinedPlayers().forEach(data-> data.getPlayer().ifPresent(this::resetPlayerClientData));
        mapTeams.reset();
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

    @Override
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
    @Override
    public void syncToClient() {
        super.syncToClient();
        this.syncToClient(true);
    }

    @Override
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

    public int getC4InstantKillRadius(){
        return c4InstantKillRadius.get();
    }

    @Override
    public void handleDeath(ServerPlayer dead){
        if(this.isStart){
            MapTeams teams = this.getMapTeams();
            teams.getTeamByPlayer(dead).ifPresent(deadPlayerTeam -> {
                CapabilityMap.getTeamCapability(deadPlayerTeam, ShopCapability.class)
                        .flatMap(ShopCapability::getShopSafe).ifPresent(shop -> {
                            shop.getDefaultAndPutData(dead.getUUID());
                        });

                this.sendPacketToJoinedPlayer(dead, new ShopStatesS2CPacket(false, 0, 0), true);
                // 清除c4,并掉落c4
                dropC4(dead);
                // 清除玩家所属子弹
                this.getServerLevel().getEntitiesOfClass(EntityKineticBullet.class, mapArea.getAABB())
                        .stream()
                        .filter(entityKineticBullet -> entityKineticBullet.getOwner() != null && entityKineticBullet.getOwner().getUUID().equals(dead.getUUID()))
                        .toList()
                        .forEach(Entity::discard);
                // 清除拆弹工具,并掉落拆弹工具
                int ik = dead.getInventory().clearOrCountMatchingItems((i) -> i.getItem() instanceof BombDisposalKit, -1, dead.inventoryMenu.getCraftSlots());
                if (ik > 0) {
                    dead.drop(new ItemStack(BOItemRegister.BOMB_DISPOSAL_KIT.get(), 1), false, false).setGlowingTag(true);
                }
                FPSMUtil.playerDeadDropWeapon(dead, true);
                dead.getInventory().clearContent();
                dead.heal(dead.getMaxHealth());
                dead.setGameMode(GameType.SPECTATOR);
                dead.setRespawnPosition(dead.level().dimension(), dead.getOnPos().above(), 0, true, false);
                this.setBystander(dead);
                this.syncInventory(dead);
            });
        }
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
