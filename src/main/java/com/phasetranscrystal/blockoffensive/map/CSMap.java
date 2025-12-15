package com.phasetranscrystal.blockoffensive.map;

import com.phasetranscrystal.blockoffensive.client.data.WeaponData;
import com.phasetranscrystal.blockoffensive.data.DeathMessage;
import com.phasetranscrystal.blockoffensive.event.CSGamePlayerJoinEvent;
import com.phasetranscrystal.blockoffensive.item.BOItemRegister;
import com.phasetranscrystal.blockoffensive.item.CompositionC4;
import com.phasetranscrystal.blockoffensive.net.CSGameSettingsS2CPacket;
import com.phasetranscrystal.blockoffensive.net.DeathMessageS2CPacket;
import com.phasetranscrystal.blockoffensive.net.shop.ShopStatesS2CPacket;
import com.phasetranscrystal.blockoffensive.net.spec.CSGameWeaponDataS2CPacket;
import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.common.attributes.ammo.BulletproofArmorAttribute;
import com.phasetranscrystal.fpsmatch.common.capability.team.ShopCapability;
import com.phasetranscrystal.fpsmatch.common.capability.team.SpawnPointCapability;
import com.phasetranscrystal.fpsmatch.common.capability.team.StartKitsCapability;
import com.phasetranscrystal.fpsmatch.common.entity.drop.DropType;
import com.phasetranscrystal.fpsmatch.common.packet.FPSMatchStatsResetS2CPacket;
import com.phasetranscrystal.fpsmatch.compat.CounterStrikeGrenadesCompat;
import com.phasetranscrystal.fpsmatch.compat.LrtacticalCompat;
import com.phasetranscrystal.fpsmatch.compat.impl.FPSMImpl;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import com.phasetranscrystal.fpsmatch.core.capability.CapabilityMap;
import com.phasetranscrystal.fpsmatch.core.capability.map.MapCapability;
import com.phasetranscrystal.fpsmatch.core.capability.team.TeamCapability;
import com.phasetranscrystal.fpsmatch.core.data.AreaData;
import com.phasetranscrystal.fpsmatch.core.data.PlayerData;
import com.phasetranscrystal.fpsmatch.core.data.Setting;
import com.phasetranscrystal.fpsmatch.core.data.SpawnPointData;
import com.phasetranscrystal.fpsmatch.core.map.BaseMap;
import com.phasetranscrystal.fpsmatch.core.map.IConfigureMap;
import com.phasetranscrystal.fpsmatch.core.map.VoteObj;
import com.phasetranscrystal.fpsmatch.core.shop.FPSMShop;
import com.phasetranscrystal.fpsmatch.core.team.MapTeams;
import com.phasetranscrystal.fpsmatch.core.team.ServerTeam;
import com.phasetranscrystal.fpsmatch.core.team.TeamData;
import com.phasetranscrystal.fpsmatch.util.FPSMUtil;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.IGun;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class CSMap extends BaseMap implements IConfigureMap<CSMap> {

    private static final Vector3f T_COLOR = new Vector3f(1, 0.75f, 0.25f);
    private static final Vector3f CT_COLOR = new Vector3f(0.25f, 0.55f, 1);

    private final Map<String, Consumer<ServerPlayer>> commands = new ConcurrentHashMap<>();
    private final List<Setting<?>> settings = new ArrayList<>();
    protected final Setting<Boolean> autoStart = this.addSetting("autoStart", true);
    protected final Setting<Integer> autoStartTime = this.addSetting("autoStartTime", 6000);
    protected final Setting<Boolean> allowFriendlyFire = this.addSetting("allowFriendlyFire",false);

    private final Setting<Integer> ctLimit = this.addSetting("ctLimit",5);
    private final Setting<Integer> tLimit = this.addSetting("tLimit",5);

    private final ServerTeam ctTeam;
    private final ServerTeam tTeam;

    private VoteObj voteObj;

    private int autoStartTimer = 0;
    private boolean autoStartFirstMessageFlag = false;

    public CSMap(ServerLevel serverLevel,
                 String mapName,
                 AreaData areaData,
                 List<Class<? extends MapCapability>> mapCapabilities,
                 List<Class<? extends TeamCapability>> teamCapabilities
    ) {
        this(serverLevel, mapName, areaData, mapCapabilities, teamCapabilities, teamCapabilities);
    }

    public CSMap(ServerLevel serverLevel,
                 String mapName,
                 AreaData areaData,
                 List<Class<? extends MapCapability>> capabilities ,
                 List<Class<? extends TeamCapability>> ctCapabilities,
                 List<Class<? extends TeamCapability>> tCapabilities
    ) {
        super(serverLevel, mapName, areaData, capabilities);
        this.setup();
        this.loadConfig();
        this.ctTeam = this.addTeam(TeamData.of("ct",ctLimit.get(), ctCapabilities));
        this.ctTeam.setColor(CT_COLOR);
        this.tTeam = this.addTeam(TeamData.of("t",tLimit.get(), tCapabilities));
        this.tTeam.setColor(T_COLOR);
    }

    // Config
    public abstract void setup();

    //Shop
    public abstract Function<ServerPlayer,Boolean> getPlayerCanOpenShop();
    public abstract int getNextRoundMinMoney(ServerTeam team);
    public abstract int getShopCloseTime();

    //Flags
    public abstract Team.Visibility nameTagVisibility();
    public boolean allowFriendlyFire(){
        return allowFriendlyFire.get();
    };

    public abstract boolean isError();
    public abstract boolean isPause();
    public abstract boolean isWaiting();
    public abstract boolean isWaitingWinner();
    public abstract boolean canGiveEconomy();
    public abstract int getClientTime();

    @Override
    public Collection<Setting<?>> settings() {
        return settings;
    }

    @Override
    public <I> Setting<I> addSetting(Setting<I> setting) {
        settings.add(setting);
        return setting;
    }


    @Override
    public void tick() {
        this.voteLogic();
        this.autoStartLogic();
    }

    protected void autoStartLogic() {
        if (!autoStart.get()) return;
        if (isStart) {
            resetAutoStartState();
            return;
        }

        boolean bothTeamsHavePlayers = !getCT().getOnlinePlayers().isEmpty() && !getT().getOnlinePlayers().isEmpty();
        if (bothTeamsHavePlayers) {
            handleActiveCountdown();
        } else {
            resetAutoStartState();
        }
    }

    protected void resetAutoStartState() {
        if (autoStartTimer != 0 || autoStartFirstMessageFlag) {
            autoStartTimer = 0;
            autoStartFirstMessageFlag = false;
            clearOfflinePlayers();
        }
    }

    protected void clearOfflinePlayers() {
        for (ServerTeam team : this.getMapTeams().getNormalTeams()) {
            for (UUID offline : team.getOfflinePlayers()) {
                this.getMapTeams().leaveTeam(offline);
            }
        }
    }

    protected void handleActiveCountdown() {
        autoStartTimer++;
        int totalTicks = autoStartTime.get();
        int secondsLeft = (totalTicks - autoStartTimer) / 20;

        if (!autoStartFirstMessageFlag) {
            sendAutoStartMessage(secondsLeft);
            autoStartFirstMessageFlag = true;
        }

        if (autoStartTimer >= totalTicks) {
            startGameWithAnnouncement();
            return;
        }

        if (shouldSendTitleNotification(totalTicks)) {
            sendTitleNotification(secondsLeft);
        } else if (shouldSendActionbar()) {
            sendActionbarMessage(secondsLeft);
        }
    }

    protected boolean shouldSendTitleNotification(int totalTicks) {
        if (autoStartTimer >= (totalTicks - 600) && autoStartTimer % 200 == 0) {
            return true;
        }
        return autoStartTimer >= (totalTicks - 200) && autoStartTimer % 20 == 0;
    }

    protected boolean shouldSendActionbar() {
        return autoStartTimer % 20 == 0 && this.getVote() == null;
    }

    protected void sendAutoStartMessage(int seconds) {
        Component message = Component.translatable("blockoffensive.map.cs.auto.start.message", seconds)
                .withStyle(ChatFormatting.YELLOW);
        this.sendAllPlayerMessage(message, false);
    }

    protected void sendTitleNotification(int seconds) {
        Component title = Component.translatable("blockoffensive.map.cs.auto.start.title", seconds)
                .withStyle(ChatFormatting.YELLOW);
        Component subtitle = Component.translatable("blockoffensive.map.cs.auto.start.subtitle")
                .withStyle(ChatFormatting.YELLOW);
        sendTitleToAllPlayers(title, subtitle);
    }

    protected void sendActionbarMessage(int seconds) {
        Component message = Component.translatable("blockoffensive.map.cs.auto.start.actionbar", seconds)
                .withStyle(ChatFormatting.YELLOW);
        this.sendAllPlayerMessage(message, true);
    }

    protected void startGameWithAnnouncement() {
        Component title = Component.translatable("blockoffensive.map.cs.auto.started")
                .withStyle(ChatFormatting.YELLOW);
        sendTitleToAllPlayers(title);
        resetAutoStartState();
        this.start();
    }

    // 通用标题发送工具（从CSGameMap迁移）
    protected void sendTitleToAllPlayers(Component title) {
        sendTitleToAllPlayers(title, Component.empty());
    }

    protected void sendTitleToAllPlayers(Component title, Component subtitle) {
        this.getMapTeams().getJoinedPlayers().forEach(data ->
                data.getPlayer().ifPresent(player -> {
                    player.connection.send(new ClientboundSetTitleTextPacket(title));
                    player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
                })
        );
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
        playerTeam.setNameTagVisibility(nameTagVisibility());
        playerTeam.setAllowFriendlyFire(allowFriendlyFire());
        playerTeam.setSeeFriendlyInvisibles(false);
        playerTeam.setDeathMessageVisibility(Team.Visibility.NEVER);
        return team;
    }

    /**
     * 切换两个队伍的阵营
     */
    public void switchTeams() {
        MapTeams.switchAttackAndDefend(this, getT(), getCT());
    }

    public final VoteObj getVote() {
        return voteObj;
    }

    public final void setVote(VoteObj voteObj) {
        this.voteObj = voteObj;
    }

    public @NotNull ServerTeam getT(){
        return this.tTeam;
    }

    public @NotNull ServerTeam getCT(){
        return this.ctTeam;
    }

    public void registerCommand(String command, Consumer<ServerPlayer> consumer) {
        commands.put(command, consumer);
    }

    public void handleChatCommand(String rawText,ServerPlayer player){
        String command = rawText.toLowerCase(Locale.US);
        commands.forEach((k,v)->{
            if (command.equals(k) && rawText.length() == k.length()){
                v.accept(player);
            }
        });
    }

    private void voteLogic() {
        if (this.getVote() != null) {
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

    public final void startVote(VoteObj voteObj){
        if(this.getVote() == null){
            this.setVote(voteObj);
            this.sendVoteMessage(false,
                    voteObj.getMessage(),
                    Component.translatable("blockoffensive.map.vote.help").withStyle(ChatFormatting.GREEN));
        }
    }

    public final void handleAgreeCommand(ServerPlayer serverPlayer){
        if(this.getVote() != null && this.getVote().processVote(serverPlayer, true)){
            this.sendAllPlayerMessage(Component.translatable("blockoffensive.map.vote.agree",serverPlayer.getDisplayName()).withStyle(ChatFormatting.GREEN),false);
        }
    }

    public final void handleDisagreeCommand(ServerPlayer serverPlayer) {
        if(this.getVote() != null && this.getVote().processVote(serverPlayer, false)){
            this.sendAllPlayerMessage(Component.translatable("blockoffensive.map.vote.disagree",serverPlayer.getDisplayName()).withStyle(ChatFormatting.RED),false);
        }
    }

    public final void sendVoteMessage(boolean actionBar,Component... messages){
        if(this.getVote() != null){
            for (UUID uuid : this.getVote().getEligiblePlayers()){
                this.getPlayerByUUID(uuid).ifPresent(player -> {
                    for (Component message : messages){
                        player.displayClientMessage(message,actionBar);
                    }
                });
            }
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

    public abstract boolean setTeamSpawnPoints();

    public Map<ServerTeam,List<SpawnPointData>> getAllSpawnPoints(){
        Map<ServerTeam,List<SpawnPointData>> allSpawnPoints = new HashMap<>();

        for (ServerTeam team : this.getMapTeams().getNormalTeams()){
            team.getCapabilityMap().get(SpawnPointCapability.class).ifPresent(cap->{
                allSpawnPoints.put(team,cap.getSpawnPointsData());
            });
        }

        return allSpawnPoints;
    }

    public void giveEco(ServerPlayer player, Player attacker, ItemStack itemStack, boolean punish) {
        if(!canGiveEconomy()) return;

        ServerTeam killerTeam = this.getMapTeams().getTeamByPlayer(attacker).orElse(null);
        ServerTeam deadTeam = this.getMapTeams().getTeamByPlayer(player).orElse(null);
        if(killerTeam == null || deadTeam == null) {
            FPSMatch.LOGGER.error("CSGameMap {} -> killerTeam or deadTeam are null! : killer {} , dead {}",this.getMapName(),attacker.getDisplayName(),player.getDisplayName());
            return;
        }

        if(attacker.getUUID().equals(player.getUUID())) return;

        if (!killerTeam.getFixedName().equals(deadTeam.getFixedName()) || punish){
            int reward = getRewardByItem(itemStack);
            ShopCapability.getPlayerShopData(this,attacker.getUUID()).ifPresent(shopData -> {
                shopData.addMoney(reward);
                attacker.displayClientMessage(Component.translatable("blockoffensive.kill.message.enemy",reward),false);
            });

        }else{
            ShopCapability.getPlayerShopData(this,attacker.getUUID()).ifPresent(shopData -> {
                shopData.reduceMoney(300);
                attacker.displayClientMessage(Component.translatable("blockoffensive.kill.message.teammate",300),false);
            });
        }
    }

    public void giveAllPlayersKits(Function<DropType, Boolean> checker) {
        CapabilityMap.getTeamCapability(this, StartKitsCapability.class)
                .forEach((team, opt) -> opt.ifPresent(cap -> {
                    team.getPlayersData().forEach(playerData -> {
                        playerData.getPlayer().ifPresent(player -> {
                            ArrayList<ItemStack> items = cap.getTeamKits();
                            player.getInventory().clearContent();
                            for (ItemStack item : items) {
                                ItemStack copy = item.copy();
                                DropType type = DropType.getItemDropType(copy);
                                if(!checker.apply(type)){
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

    public abstract void givePlayerKits(ServerPlayer player);

    public static void dropC4(ServerPlayer player) {
        int im = player.getInventory().clearOrCountMatchingItems((i) -> i.getItem() instanceof CompositionC4, -1, player.inventoryMenu.getCraftSlots());
        if (im > 0) {
            player.drop(new ItemStack(BOItemRegister.C4.get(), 1), false, false).setGlowingTag(true);
            player.getInventory().setChanged();
        }
    }

    public void setBystander(ServerPlayer player) {
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

    public void resetPlayerClientData(ServerPlayer serverPlayer){
        FPSMatchStatsResetS2CPacket packet = new FPSMatchStatsResetS2CPacket();
        FPSMatch.INSTANCE.send(PacketDistributor.PLAYER.with(()-> serverPlayer), packet);
    }

    public void resetGunAmmo(){
        this.getMapTeams().getJoinedPlayers().forEach((data)-> data.getPlayer().ifPresent(FPSMUtil::resetAllGunAmmo));
    }

    public void sendAllPlayerMessage(Component message, boolean actionBar){
        this.getMapTeams().getJoinedPlayers().forEach(data -> data.getPlayer().ifPresent(player -> player.displayClientMessage(message,actionBar)));
    }

    /**
     * 同步游戏设置到客户端（比分/时间等）
     * @see CSGameSettingsS2CPacket
     */
    public void syncToClient(boolean syncWeapon) {
        ServerTeam ct = this.getCT();
        ServerTeam t = this.getT();
        CSGameSettingsS2CPacket packet = new CSGameSettingsS2CPacket(
                ct.getScores(),t.getScores(),
                this.getClientTime(),
                CSGameSettingsS2CPacket.GameFlags.of(
                        this.isDebug(),
                        this.isStart,
                        this.isError(),
                        this.isPause(),
                        this.isWaiting(),
                        this.isWaitingWinner()
                )
        );

        this.sendPacketToAllPlayer(packet);
        this.getMapTeams().sync();

        if(isStart){
            syncShopInfo();

            if(syncWeapon){
                syncWeaponData();
            }
        }
    }

    public void syncShopInfo(){
        this.getMapTeams().getNormalTeams().forEach(team -> {
            team.getPlayers().values().forEach(data -> {
                data.getPlayer().ifPresent(player -> {
                    syncShopInfo(team,player,getPlayerCanOpenShop().apply(player),getShopCloseTime());
                });
            });
        });
    }

    public void syncShopInfo(boolean enable,int time){
        this.getMapTeams().getNormalTeams().forEach(team -> {
            team.getPlayers().values().forEach(data -> {
                data.getPlayer().ifPresent(player -> {
                    syncShopInfo(team,player,enable,time);
                });
            });
        });
    }

    public void syncShopInfo(ServerTeam team, ServerPlayer player, boolean enable, int closeTime){
        var packet = new ShopStatesS2CPacket(enable,getNextRoundMinMoney(team),closeTime);
        this.sendPacketToJoinedPlayer(player,packet,false);
    }

    public void syncWeaponData(){
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
        this.sendPacketToSpecPlayer(weaponDataS2CPacket);
    }

    public void read() {
        FPSMCore.getInstance().registerMap(this.getGameType(),this);
    }

    @Override
    public boolean reload(){
        if (!super.reload()) return false;
        reset();
        loadConfig();
        return true;
    }

    public void onPlayerDeathEvent(ServerPlayer player, DamageSource source) {
        ServerPlayer attacker;
        if (source.getEntity() instanceof ServerPlayer p) {
            attacker = p;
        } else if (source.getEntity() instanceof ThrowableItemProjectile throwable
                && throwable.getOwner() instanceof ServerPlayer p) {
            attacker = p;
        } else {
            attacker = null;
        }
        handleDeathEvent(player,attacker);

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

        giveEco(player, attacker, itemStack,true);

        DeathMessage.Builder builder = new DeathMessage.Builder(attacker, player, itemStack);
        DeathMessageS2CPacket killMessageS2CPacket = new DeathMessageS2CPacket(builder.build());
        this.sendPacketToAllPlayer(killMessageS2CPacket);
    }

    public final void handleDeathEvent(ServerPlayer dead, @Nullable ServerPlayer killer){
        handleDeath(dead);
        calculateTabData(dead,killer);
    }

    public abstract void handleDeath(ServerPlayer dead);

    public void calculateTabData(ServerPlayer dead, @Nullable ServerPlayer killer) {
        if(this.isStart) {
            MapTeams teams = this.getMapTeams();
            Map<UUID, Float> hurtDataMap = teams.getDamageMap().get(dead.getUUID());
            if (hurtDataMap != null && !hurtDataMap.isEmpty()) {
                hurtDataMap.entrySet().stream()
                        .filter(entry -> entry.getValue() > dead.getMaxHealth() / 4)
                        .sorted(Map.Entry.<UUID, Float>comparingByValue().reversed())
                        .limit(1)
                        .findAny().ifPresent(assist->{
                            UUID assistId = assist.getKey();
                            teams.getTeamByPlayer(assistId)
                                    .flatMap(assistPlayerTeam -> assistPlayerTeam.getPlayerData(assistId))
                                    .ifPresent(assistData -> {
                                        if (killer != null && killer.getUUID().equals(assistId)) return;
                                        assistData.addAssist();
                                    });
                        });

            }

            if(killer == null || killer.getUUID().equals(dead.getUUID())) return;
            teams.getTeamByPlayer(killer)
                    .flatMap(killerPlayerTeam -> killerPlayerTeam.getPlayerData(killer.getUUID()))
                    .ifPresent(PlayerData::addKills);
        }
    }

    public int getRewardByItem(ItemStack itemStack){
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

    public int gerRewardByGunId(ResourceLocation gunId){
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
}
