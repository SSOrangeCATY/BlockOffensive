package com.phasetranscrystal.blockoffensive.sound;

import com.phasetranscrystal.blockoffensive.BlockOffensive;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BOSoundRegister {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BlockOffensive.MODID);
    public static RegistryObject<SoundEvent> beep = SOUNDS.register("beep", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(BlockOffensive.MODID, "beep")));
    public static RegistryObject<SoundEvent> planting = SOUNDS.register("planting", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(BlockOffensive.MODID, "planting")));
    public static RegistryObject<SoundEvent> planted = SOUNDS.register("planted", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(BlockOffensive.MODID, "planted")));
    public static RegistryObject<SoundEvent> defused = SOUNDS.register("defused", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(BlockOffensive.MODID, "defused")));
    public static RegistryObject<SoundEvent> click = SOUNDS.register("buttons_click", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(BlockOffensive.MODID, "buttons_click")));
    public static RegistryObject<SoundEvent> voice_t_win = SOUNDS.register("voice_ct_win", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(BlockOffensive.MODID, "voice_ct_win")));
    public static RegistryObject<SoundEvent> voice_ct_win = SOUNDS.register("voice_t_win", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(BlockOffensive.MODID, "voice_t_win")));
}
