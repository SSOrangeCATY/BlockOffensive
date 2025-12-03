package com.phasetranscrystal.blockoffensive.mixin;

import com.phasetranscrystal.blockoffensive.item.BOItemRegister;
import com.phasetranscrystal.blockoffensive.item.BombDisposalKit;
import com.phasetranscrystal.blockoffensive.item.CompositionC4;
import com.phasetranscrystal.blockoffensive.map.CSGameMap;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import com.phasetranscrystal.fpsmatch.core.map.BaseMap;
import com.phasetranscrystal.fpsmatch.core.team.ServerTeam;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ItemEntity.class)
public abstract class C4KitsItemEntityMixin {
    @Shadow public abstract ItemStack getItem();

    @Inject(at = @At("HEAD"), method = "playerTouch", cancellable = true)
    public void fpsMatch$playerTouch$CustomC4(Player player, CallbackInfo ci) {
        if(!player.isCreative() && !player.level().isClientSide){
            if(this.getItem().getItem() instanceof CompositionC4){
                Optional<BaseMap> optional = FPSMCore.getInstance().getMapByPlayer(player);
                if (optional.isEmpty()) {
                    ci.cancel();
                    return;
                }
                BaseMap map = optional.get();
                ServerTeam team = map.getMapTeams().getTeamByPlayer(player).orElse(null);
                if(team != null && map instanceof CSGameMap csGameMap){
                    if(!csGameMap.checkCanPlacingBombs(team.getFixedName())){
                        ci.cancel();
                    }
                }else{
                    ci.cancel();
                }
            }

            if(this.getItem().getItem() instanceof BombDisposalKit){
                Optional<BaseMap> optional = FPSMCore.getInstance().getMapByPlayer(player);
                if (optional.isEmpty()) {
                    ci.cancel();
                    return;
                }
                BaseMap map = optional.get();
                ServerTeam team = map.getMapTeams().getTeamByPlayer(player).orElse(null);
                if(team != null && map instanceof CSGameMap csGameMap){
                    if(!csGameMap.checkCanPlacingBombs(team.getFixedName())){
                        int i = player.getInventory().countItem(BOItemRegister.BOMB_DISPOSAL_KIT.get());
                        if(i > 0){
                            ci.cancel();
                        }
                    }else{
                        ci.cancel();
                    }
                }else{
                    ci.cancel();
                }
            }
        }
    }
}
