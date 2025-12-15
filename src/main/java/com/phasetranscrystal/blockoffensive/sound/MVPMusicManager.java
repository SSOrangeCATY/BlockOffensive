package com.phasetranscrystal.blockoffensive.sound;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.phasetranscrystal.blockoffensive.BlockOffensive;
import com.phasetranscrystal.fpsmatch.core.event.register.RegisterFPSMSaveDataEvent;
import com.phasetranscrystal.fpsmatch.core.persistence.FPSMDataManager;
import com.phasetranscrystal.fpsmatch.core.persistence.SaveHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@SuppressWarnings("all")
@Mod.EventBusSubscriber(modid = BlockOffensive.MODID,bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MVPMusicManager {
    public static final Codec<MVPMusicManager> CODEC = Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC).xmap(MVPMusicManager::new,
            (manager)-> manager.mvpMusicMap);
    private final Map<String, ResourceLocation> mvpMusicMap;
    private static MVPMusicManager INSTANCE = new MVPMusicManager();

    public static MVPMusicManager getInstance(){
        return INSTANCE;
    }

    public MVPMusicManager(){
        mvpMusicMap = Maps.newHashMap();
    }

    public MVPMusicManager(Map<String, ResourceLocation> mvpMusicMap){
        this.mvpMusicMap = Maps.newHashMap();
        this.mvpMusicMap.putAll(mvpMusicMap);
    }

    public void addMvpMusic(String uuid, ResourceLocation music){
        mvpMusicMap.put(uuid, music);
    }

    public ResourceLocation getMvpMusic(String uuid){
        return this.mvpMusicMap.getOrDefault(uuid, ResourceLocation.tryBuild("fpsmatch", "empty"));
    }

    public boolean playerHasMvpMusic(String uuid){
        return this.mvpMusicMap.containsKey(uuid);
    }

    @SubscribeEvent
    public static void onDataRegister(RegisterFPSMSaveDataEvent event){
        event.registerData(MVPMusicManager.class,"MvpMusicData",
                new SaveHolder.Builder<>(CODEC)
                        .withLoadHandler(MVPMusicManager::read)
                        .withSaveHandler(MVPMusicManager::write)
                        .withMergeHandler(MVPMusicManager::merge)
                        .withVersion(0)
                        .isGlobal(true)
                        .build()
        );
    }

    private void read() {
        INSTANCE = this;
    }

    private void read(MVPMusicManager mvpMusicManager) {
        this.mvpMusicMap.putAll(mvpMusicManager.mvpMusicMap);
    }

    public static void write(FPSMDataManager manager){
        manager.saveData(INSTANCE,"data",false);
    }

    public static MVPMusicManager merge(@Nullable MVPMusicManager old, MVPMusicManager newer){
        if(old == null){
            return newer;
        }else{
            old.read(newer);
            return old;
        }
    }
}
