package com.phasetranscrystal.blockoffensive.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.phasetranscrystal.blockoffensive.BlockOffensive;
import com.phasetranscrystal.blockoffensive.sound.MVPMusicManager;
import com.phasetranscrystal.fpsmatch.common.command.FPSMHelpManager;
import com.phasetranscrystal.fpsmatch.common.event.register.RegisterFPSMCommandEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Collection;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = BlockOffensive.MODID)
public class BOCommandRegister {

    @SubscribeEvent
    public static void onFPSMCommandRegister(RegisterFPSMCommandEvent event) {
        event.addChild(Commands.literal("mvp")
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("sound", ResourceLocationArgument.id())
                                .suggests(SuggestionProviders.AVAILABLE_SOUNDS)
                                .executes(BOCommandRegister::handleMvp))));
        FPSMHelpManager.getInstance().registerCommandHelp("fpsm mvp", Component.translatable("commands.blockoffensive.mvp.description"));
        FPSMHelpManager.getInstance().registerCommandParameters("fpsm mvp", "*targets", "*sound");
    }

    private static int handleMvp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        ResourceLocation sound = ResourceLocationArgument.getId(context, "sound");
        players.forEach(player -> MVPMusicManager.getInstance().addMvpMusic(player.getUUID().toString(), sound));
        context.getSource().sendSuccess(() -> Component.translatable("commands.blockoffensive.mvp.success", players.size(), sound), true);
        return 1;
    }

}
