package com.phasetranscrystal.blockoffensive.entity;

import com.phasetranscrystal.blockoffensive.BOConfig;
import com.phasetranscrystal.blockoffensive.BlockOffensive;
import com.phasetranscrystal.blockoffensive.item.BOItemRegister;
import com.phasetranscrystal.blockoffensive.map.CSGameMap;
import com.phasetranscrystal.blockoffensive.net.bomb.BombActionS2CPacket;
import com.phasetranscrystal.blockoffensive.net.bomb.BombDemolitionProgressS2CPacket;
import com.phasetranscrystal.blockoffensive.net.spec.BombFuseS2CPacket;
import com.phasetranscrystal.blockoffensive.sound.BOSoundRegister;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import com.phasetranscrystal.fpsmatch.core.entity.BlastBombEntity;
import com.phasetranscrystal.fpsmatch.core.map.BaseMap;
import com.phasetranscrystal.fpsmatch.core.map.BlastBombState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;

public class CompositionC4Entity extends Entity implements TraceableEntity , BlastBombEntity {
    private static final EntityDataAccessor<Integer> DATA_EXPLOSION_RADIUS = SynchedEntityData.defineId(CompositionC4Entity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DELETE_TIME = SynchedEntityData.defineId(CompositionC4Entity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_EXPLOSION_INTERACTION = SynchedEntityData.defineId(CompositionC4Entity.class, EntityDataSerializers.INT);
    private static final int DEFAULT_FUSE_TIME = 800; // 40秒
    private static final int DEFAULT_EXPLOSION_RADIUS = 60;
    private Player owner;
    @Nullable
    private Player demolisher;
    private boolean deleting = false;
    private int demolitionProgress = 0;
    private int fuse = DEFAULT_FUSE_TIME;
    private BlastBombState state = BlastBombState.TICKING;
    private CSGameMap map;

    public CompositionC4Entity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.blocksBuilding = true;
        this.noCulling = true;
        this.setDeleteTime(0);
    }

    public CompositionC4Entity(Level pLevel, double pX, double pY, double pZ, Player pOwner, @NotNull CSGameMap map) {
        this(BOEntityRegister.C4.get(), pLevel);
        this.map = map;
        this.setFuse(BOConfig.common.fuseTime.get());
        this.setExplosionRadius(BOConfig.common.explosionRadius.get());
        this.setPos(pX, pY, pZ);
        this.owner = pOwner;
        this.map.setBombEntity(this);
    }

    public CompositionC4Entity(Level pLevel, Vec3 pos, Player pOwner, @NotNull CSGameMap map, int fuseTime, int explosionRadius) {
        this(BOEntityRegister.C4.get(), pLevel);
        this.map = map;
        this.setFuse(fuseTime);
        this.setExplosionRadius(explosionRadius);
        this.setPos(pos);
        this.owner = pOwner;
        this.map.setBombEntity(this);
    }

    public CompositionC4Entity(Level pLevel, Vec3 pos, Player pOwner, @NotNull CSGameMap map, int fuseTime, int explosionRadius, Level.ExplosionInteraction explosionInteraction) {
        this(BOEntityRegister.C4.get(), pLevel);
        this.map = map;
        this.setExplosionInteraction(explosionInteraction);
        this.setFuse(fuseTime);
        this.setExplosionRadius(explosionRadius);
        this.setPos(pos);
        this.owner = pOwner;
        this.map.setBombEntity(this);
    }

    @Override
    public boolean isAlwaysTicking() {
        return true;
    }

    public boolean isDeleting(){
        return this.deleting;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_EXPLOSION_RADIUS, DEFAULT_EXPLOSION_RADIUS);
        this.entityData.define(DATA_DELETE_TIME,0);
        this.entityData.define(DATA_EXPLOSION_INTERACTION, Level.ExplosionInteraction.NONE.ordinal());
    }


    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
    }


    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (!this.level().isClientSide) {
            if(map != null){
                map.setBombEntity(null);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        this.setFuse(pCompound.getInt("Fuse"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        pCompound.putInt("Fuse", this.getFuse());
    }

    public void tick() {
        if(this.getDeleteTime() >= 140){
            demolitionProgress = 0;
            this.discard();
        }

        if(!this.level().isClientSide){
            if(this.map == null){
                this.discard();
                return;
            }

            if(this.deleting){
                int d = this.getDeleteTime() + 1;
                this.setDeleteTime(d);
                return;
            }

            int i = this.getFuse() - 1;
            this.setFuse(i);
            if(demolisher == null){
                demolitionProgress = 0;
            }else{
                demolitionProgress++;
                if (demolisher instanceof ServerPlayer serverPlayer){
                    if(serverPlayer.gameMode.getGameModeForPlayer() == GameType.SPECTATOR){
                        demolitionProgress = 0;
                        this.demolisher = null;
                    }
                    if(i % 2 == 0 && i > 0){
                        BlockOffensive.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new BombActionS2CPacket());
                    }
                }
            }

            this.syncDemolitionProgress();
            if(demolitionProgress >= getTotalDemolitionProgress()){
                this.state = BlastBombState.DEFUSED;
                this.deleting = true;
                this.demolitionProgress = 0;
                this.playDefusedSound();
                return;
            }

            if (i <= 0) {
                if (!this.level().isClientSide) {
                    this.state = BlastBombState.EXPLODED;
                    this.explode();
                }
            }

            if(i < 200){
                if(i < 100){
                    if(i == 20) this.playNvgOnSound();
                    if(i % 5 == 0){
                        this.playBeepSound();
                    }
                }else if( i % 10 == 0){
                    this.playBeepSound();
                }
            } else{
                if(i % 20 == 0){
                    this.playBeepSound();
                }
            }
        }

        if (!this.isNoGravity()) {
            this.setDeltaMovement(0,0,0);
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D)); // 重力
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
    }


    public void playBeepSound(){
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), BOSoundRegister.BEEP.get(), SoundSource.VOICE, 3.0F, 1);
    }

    public void playNvgOnSound(){
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), BOSoundRegister.WEAPON_C4_PRE_EXPLODE.get(), SoundSource.VOICE, 3.0F, 1);
    }

    public void playDefusingSound(){
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.VOICE, 3.0F, 1);
    }
    public void playDefusedSound(){
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), BOSoundRegister.DEFUSED.get(), SoundSource.VOICE, 3.0F, 1);
    }

    private void explode() {
        float explosionRadius = this.getExplosionRadius(); // 爆炸半径
        this.deleting = true;
        this.demolitionProgress = 0;
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), explosionRadius, this.explosionInteraction());
    }

    @Nullable
    public LivingEntity getOwner() {
        return this.owner;
    }

    protected float getEyeHeight(@NotNull Pose pPose, @NotNull EntityDimensions pSize) {
        return 0.15F;
    }

    public void setFuse(int pLife) {
        this.fuse = pLife;
        this.map.getMapTeams().getSpecPlayers().forEach((pUUID)->{
            Optional<ServerPlayer> receiver = FPSMCore.getInstance().getPlayerByUUID(pUUID);
            receiver.ifPresent(player -> BlockOffensive.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new BombFuseS2CPacket(pLife,BOConfig.common.fuseTime.get())));
        });
    }

    public void syncDemolitionProgress(){
        BaseMap map = this.map;
        float progress = this.getDemolitionProgress();
        if(map != null){
            map.getMapTeams().getJoinedPlayers().forEach((data)->{
                data.getPlayer().ifPresent(receiver->{
                    map.getMapTeams().getTeamByPlayer(receiver).ifPresent(team->{
                        boolean flag = this.map.checkCanPlacingBombs(team.getFixedName());
                        if(!flag){
                            BlockOffensive.INSTANCE.send(PacketDistributor.PLAYER.with(() -> receiver), new BombDemolitionProgressS2CPacket(progress));
                        }
                    });
                });
            });

            map.getMapTeams().getSpecPlayers().forEach((pUUID)-> {
                ServerPlayer receiver = (ServerPlayer) this.level().getPlayerByUUID(pUUID);
                if (receiver != null) {
                    BlockOffensive.INSTANCE.send(PacketDistributor.PLAYER.with(() -> receiver), new BombDemolitionProgressS2CPacket(progress));
                }
            });
        }
    }

    public int getFuse() {
        return this.fuse;
    }

    public void setExplosionRadius(int radius){
        this.entityData.set(DATA_EXPLOSION_RADIUS, radius);
    }

    public int getExplosionRadius() {
        return this.entityData.get(DATA_EXPLOSION_RADIUS);
    }

    public int getDeleteTime(){
        return this.entityData.get(DATA_DELETE_TIME);
    }

    public int getTotalDemolitionProgress(){
        int j = 200;
        if(demolisher != null && this.demolisher.getInventory().countItem(BOItemRegister.BOMB_DISPOSAL_KIT.get()) >= 1){
            j = 100;
        }
        return j;
    }

    public float getDemolitionProgress(){
        return (float) this.demolitionProgress / this.getTotalDemolitionProgress();
    }

    public void setDeleteTime(int progress){
        this.entityData.set(DATA_DELETE_TIME, progress);
    }

    public void setDemolisher(@org.jetbrains.annotations.Nullable Player player){
        if(this.demolisher == null) {
            if (player != null && checkDemolisher(player)) {
                this.playDefusingSound();
                this.demolisher = player;
            }
        }
    }

    public void resetDemolisher(){
        this.demolisher = null;
    }

    public AABB getR(){
        AABB ab = this.getBoundingBox();
        int r = 3;
        return new AABB(ab.minX - r, ab.minY - r, ab.minZ - r, ab.maxX + r, ab.maxY + r, ab.maxZ + r);
    }
    public boolean checkDemolisher(Player player){
        return this.getR().contains(player.getPosition(0));
    }

    @Nullable
    public LivingEntity getDemolisher() {
        return demolisher;
    }

    public void setExplosionInteraction(Level.ExplosionInteraction explosionInteraction) {
        this.entityData.set(DATA_EXPLOSION_INTERACTION, explosionInteraction.ordinal());
    }

    public Level.ExplosionInteraction explosionInteraction() {
        return Level.ExplosionInteraction.values()[this.entityData.get(DATA_EXPLOSION_INTERACTION)];
    }

    public BlastBombState getState(){
        return state;
    }

}