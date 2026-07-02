package com.phasetranscrystal.blockoffensive.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.phasetranscrystal.blockoffensive.entity.CompositionC4Entity;
import com.phasetranscrystal.blockoffensive.event.CSGameMapEvent;
import com.phasetranscrystal.blockoffensive.map.CSGameMap;
import com.phasetranscrystal.blockoffensive.sound.BOSoundRegister;
import com.phasetranscrystal.blockoffensive.util.BOUtil;
import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.common.capability.team.ShopCapability;
import com.phasetranscrystal.fpsmatch.common.packet.FPSMSoundPlayS2CPacket;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import com.phasetranscrystal.fpsmatch.core.item.BlastBombItem;
import com.phasetranscrystal.fpsmatch.core.map.*;
import com.phasetranscrystal.fpsmatch.core.team.ServerTeam;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.NotNull;
import net.minecraft.util.TriState;

import java.util.Optional;
import java.util.function.Consumer;

@EventBusSubscriber(modid = "blockoffensive")
public class CompositionC4 extends Item implements BlastBombItem {
	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		Player player = event.getEntity();
		ItemStack stack = player.getItemInHand(event.getHand());

		if (stack.getItem() instanceof CompositionC4) {
			event.setUseItem(TriState.TRUE);
			event.setUseBlock(TriState.FALSE);
		}
	}

	public CompositionC4(Properties pProperties) {
		super(pProperties);
	}

	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			@Override
			public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
				if (!itemStack.isEmpty()) {
					if (entityLiving.getUsedItemHand() == hand && entityLiving.getUseItemRemainingTicks() > 0) {
						return HumanoidModel.ArmPose.ITEM;
					}
				}
				return HumanoidModel.ArmPose.EMPTY;
			}

			@Override
			public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess) {
				int i = arm == HumanoidArm.RIGHT ? 1 : -1;
				poseStack.translate(i * 0.36F, -0.52F, -0.72F);
				if (player.getUseItem() == itemInHand && player.isUsingItem()) {
					poseStack.translate(0.0, -0.05, 0.0);
				}
				return true;
			}
		});
	}

	public void inventoryTick(@NotNull ItemStack pStack, @NotNull Level pLevel, @NotNull Entity pEntity, int pSlotId, boolean pIsSelected) {
		if(pLevel instanceof ServerLevel serverLevel && pEntity instanceof ServerPlayer player) {
			int i = player.getInventory().countItem(BOItemRegister.C4.get());
			if (i > 0) {
				double yawRad = Math.toRadians(player.getYRot());
				double distance = -0.5;
				double xOffset = -Math.sin(yawRad) * distance;
				double zOffset = Math.cos(yawRad) * distance;
				serverLevel.sendParticles(new DustParticleOptions(0xFF1A1A,1),player.getX()+xOffset,player.getY() + 1,player.getZ()+zOffset,1,0,0,0,1);
			}
		}
	}

	@Override
	public @NotNull InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide()) return InteractionResult.SUCCESS;

		FPSMCore core = FPSMCore.getInstance();
		Optional<BaseMap> optional = core.getMapByPlayer(player);

		if (optional.isEmpty()) {
			BOUtil.sendClientMessage(player, Component.translatable("blockoffensive.item.c4.use.fail.noMap"), true);
			return InteractionResult.PASS;
		}
		BaseMap baseMap = optional.get();

		if (!(baseMap instanceof CSGameMap map)) {
			BOUtil.sendClientMessage(player, Component.translatable("blockoffensive.item.c4.use.fail.noMap"), true);
			return InteractionResult.PASS;
		}

		if (!baseMap.isStart()) {
			BOUtil.sendClientMessage(player, Component.translatable("blockoffensive.item.c4.use.fail.map.notStart"), true);
			return InteractionResult.PASS;
		}

		ServerTeam team = baseMap.getMapTeams().getTeamByPlayer(player).orElse(null);
		if (team == null) {
			BOUtil.sendClientMessage(player, Component.translatable("blockoffensive.item.c4.use.fail.team.notInTeam"), true);
			return InteractionResult.PASS;
		}

		boolean canPlace = map.checkCanPlacingBombs(team.getFixedName())
				&& map.blastState() == BlastBombState.NONE
				&& player.onGround();
		boolean inBombArea = map.checkPlayerIsInBombArea(player);

		if (canPlace && inBombArea) {
			player.startUsingItem(hand);
			playClickSound(level, player, team);
			team.sendMessage(BOUtil.buildTeamChatMessage(player,team,Component.translatable("blockoffensive.place.message.c4"),Component.empty(), BOUtil.parseTextColor(team.name.equals("ct") ? "#96C8FA" : "#EAC055")));
			return InteractionResult.CONSUME;
		}

		if (!canPlace) {
			BOUtil.sendClientMessage(player, Component.translatable("blockoffensive.item.c4.use.fail"), true);
		} else if (map.getBombAreaData().isEmpty()) {
			BOUtil.sendClientMessage(player, Component.translatable("blockoffensive.item.c4.use.fail.noArea"), true);
		} else {
			BOUtil.sendClientMessage(player, Component.translatable("blockoffensive.item.c4.use.fail.notInArea"), true);
		}
		return InteractionResult.PASS;
	}

	public @NotNull InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		Level level = context.getLevel();

		if (player == null) return InteractionResult.PASS;

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		InteractionResult result = this.use(level, player, context.getHand());

		if (result == InteractionResult.CONSUME) {
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	private void playClickSound(Level level, LivingEntity entity, ServerTeam team) {
		level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
				BOSoundRegister.CLICK.get(), SoundSource.PLAYERS, 3.0F, 1.0F);
		team.getOnline().forEach(player -> {
			FPSMatch.sendToPlayer(player,new FPSMSoundPlayS2CPacket(BOUtil.soundId(BOSoundRegister.T_PLANTINGBOMB.get())));
		});

	}

	@Override
	public void onUseTick(@NotNull Level level, @NotNull LivingEntity entity,
						  @NotNull ItemStack stack, int remainingTicks) {
		if (!(level.isClientSide())) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !entity.getUUID().equals(mc.player.getUUID())) return;
		// 禁用移动控制
		disableMovementKeys(mc);


		if (remainingTicks != 80 && remainingTicks % 8 == 0) {
			level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), BOSoundRegister.CLICK.get(), SoundSource.PLAYERS, 3.0F, 1.0F,false);
		}
	}

	private void disableMovementKeys(Minecraft mc) {
		mc.options.keyUp.setDown(false);
		mc.options.keyLeft.setDown(false);
		mc.options.keyDown.setDown(false);
		mc.options.keyRight.setDown(false);
		mc.options.keyJump.setDown(false);
	}

	@Override
	public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level,
											  @NotNull LivingEntity entity) {
		if (!(entity instanceof ServerPlayer player)) return stack;

		FPSMCore core = FPSMCore.getInstance();
		Optional<BaseMap> optional = core.getMapByPlayer(player);

		if (optional.isEmpty()) return stack;
		BaseMap baseMap = optional.get();

		if (!(baseMap instanceof CSGameMap map)) return stack;

		if (!map.checkPlayerIsInBombArea(player)) {
			BOUtil.sendClientMessage(player, Component.translatable("blockoffensive.item.c4.use.fail.notInArea"), true);
			return stack;
		}

		// 放置C4实体
		CompositionC4Entity c4 = new CompositionC4Entity(
				level, player.getX(), player.getY() + 0.25, player.getZ(), player, map
		);
		level.addFreshEntity(c4);

		// 播放放置音效
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				BOSoundRegister.PLANTED.get(), SoundSource.PLAYERS, 3.0F, 1.0F);

		// 经济奖励
		baseMap.getMapTeams().getTeamByPlayer(player)
				.flatMap(team -> team.getCapabilityMap().get(ShopCapability.class)
						.flatMap(ShopCapability::getShopSafe)).ifPresent(shop -> {
							shop.getPlayerShopData(player.getUUID()).addMoney(300);
        });

		// 通知所有玩家
		Component message = Component.translatable("blockoffensive.item.c4.planted").withStyle(ChatFormatting.RED);
		baseMap.getMapTeams().getJoinedPlayers().forEach(data ->
				data.getPlayer().ifPresent(p -> BOUtil.sendClientMessage(p, message, true))
		);

		map.getMapTeams().getTeamByPlayer(player).ifPresent(team -> {
			NeoForge.EVENT_BUS.post(new CSGameMapEvent.PlayerEvent.PlacedC4Event(map,team,player));
		});

		return ItemStack.EMPTY;
	}

	@Override
	public @NotNull ItemUseAnimation getUseAnimation(@NotNull ItemStack stack) {
		return ItemUseAnimation.NONE;
	}

	@Override
	public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
		return 80;
	}

}
