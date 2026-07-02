package com.phasetranscrystal.blockoffensive.item.test;

import com.phasetranscrystal.blockoffensive.client.screen.CSGameShopScreen;
import icyllis.modernui.mc.MuiModApi;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class TestItem extends Item {
    public TestItem(Properties pProperties) {
        super(pProperties);
    }
    @Override
    public @NotNull InteractionResult use(Level pLevel, @NotNull Player pPlayer, @NotNull InteractionHand pUsedHand) {
        if(pLevel.isClientSide()){
            try{
               MuiModApi.openScreen(CSGameShopScreen.getInstance());
            }catch (Exception e){
                e.fillInStackTrace();
            }
        }
        return super.use(pLevel,pPlayer,pUsedHand);
    }
}
