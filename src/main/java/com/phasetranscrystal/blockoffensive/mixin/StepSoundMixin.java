package com.phasetranscrystal.blockoffensive.mixin;

import com.phasetranscrystal.blockoffensive.map.CSGameMap;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import com.phasetranscrystal.fpsmatch.core.map.BaseMap;
import com.phasetranscrystal.fpsmatch.core.map.BaseTeam;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Mixin(Player.class)
public abstract class StepSoundMixin extends LivingEntity {
    protected StepSoundMixin(EntityType<? extends LivingEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(at = @At("HEAD"), method = "playStepSound", cancellable = true)
    public void fpsMatch$playStepSound$stepSoundFix(BlockPos pPos, BlockState pState, CallbackInfo ci) {
        if(this.level().isClientSide) return;

        Player me = (Player)(Object)this;
        BaseMap map = FPSMCore.getInstance().getMapByPlayer(me);
        if (map instanceof CSGameMap && !this.isSilent() && !this.isCrouching()){
            Optional<BaseTeam> team = map.getMapTeams().getTeamByPlayer(me);
            if (team.isPresent()) {
                BaseTeam joined = team.get();
                ci.cancel();
                if (this.isInWater()) {
                    this.waterSwimSound();
                    this.blockoffensive$playMuffledStepSound(pState, pPos,joined);
                } else {
                    BlockPos blockpos = this.getPrimaryStepSoundBlockPos(pPos);
                    if (!pPos.equals(blockpos)) {
                        BlockState blockstate = this.level().getBlockState(blockpos);
                        if (blockstate.is(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)) {
                            this.blockoffensive$playCombinationStepSounds(blockstate, pState, blockpos, pPos,joined);
                        } else {
                            SoundType soundtype = pState.getSoundType(this.level(), pPos, this);
                            this.blockoffensive$playStepSound(soundtype,joined,false);
                        }
                    } else {
                        SoundType soundtype = pState.getSoundType(this.level(), pPos, this);
                        this.blockoffensive$playStepSound(soundtype,joined,false);
                    }
                }
            }
        }
    }

    @Unique
    protected void blockoffensive$playCombinationStepSounds(BlockState pPrimaryState, BlockState pSecondaryState, BlockPos primaryPos, BlockPos secondaryPos, BaseTeam joined) {
        SoundType soundtype = pPrimaryState.getSoundType(this.level(), primaryPos, this);
        blockoffensive$playStepSound(soundtype,joined,false);
        blockoffensive$playMuffledStepSound(pSecondaryState, secondaryPos,joined);
    }

    @Unique
    protected void blockoffensive$playMuffledStepSound(BlockState pState, BlockPos pos, BaseTeam joined) {
        SoundType soundtype = pState.getSoundType(this.level(), pos, this);
        blockoffensive$playStepSound(soundtype,joined,true);
    }

    @Unique
    protected void blockoffensive$playStepSound(SoundType soundtype, BaseTeam joined, boolean isMuffled) {
        List<ServerPlayer> players = Objects.requireNonNull(this.level().getServer()).getPlayerList().getPlayers();
        Holder<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(soundtype.getStepSound());
        var teammate = new ClientboundSoundPacket(sound, this.getSoundSource() ,this.getX(), this.getY(), this.getZ(),soundtype.getVolume() * (isMuffled ? 0.05F : 0.15F), soundtype.getPitch(),this.level().getRandom().nextLong());
        var enemy = new ClientboundSoundPacket(sound, this.getSoundSource() ,this.getX(), this.getY(), this.getZ(),soundtype.getVolume() * (isMuffled ? 0.6F : 1.8F), soundtype.getPitch(),this.level().getRandom().nextLong());

        for (ServerPlayer player : players) {
            BaseMap map = FPSMCore.getInstance().getMapByPlayer(player);
            if(map instanceof CSGameMap){
                Optional<BaseTeam> team = map.getMapTeams().getTeamByPlayer(player);
                if (team.isPresent() && team.get().getFixedName().equals(joined.getFixedName())) {
                    player.connection.send(teammate);
                }else{
                    player.connection.send(enemy);
                }
            }else{
                player.connection.send(enemy);
            }
        }
    }
}
