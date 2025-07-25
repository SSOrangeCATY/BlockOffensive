package com.phasetranscrystal.blockoffensive.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.phasetranscrystal.blockoffensive.entity.CompositionC4Entity;
import com.phasetranscrystal.blockoffensive.sound.BOSoundRegister;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import com.phasetranscrystal.fpsmatch.core.item.BlastBombItem;
import com.phasetranscrystal.fpsmatch.core.map.BaseMap;
import com.phasetranscrystal.fpsmatch.core.map.BaseTeam;
import com.phasetranscrystal.fpsmatch.core.map.BlastModeMap;
import com.phasetranscrystal.fpsmatch.core.map.ShopMap;
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
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.function.Consumer;

public class CompositionC4 extends Item implements BlastBombItem {

	public CompositionC4(Properties pProperties) {
		super(pProperties);
	}

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			private static final HumanoidModel.ArmPose ITEM_C4 = HumanoidModel.ArmPose.create("ITEM_C4", true, (model, entity, arm) -> {
				float rotationAngle = (float) Math.toRadians(30);  // 将角度设置为 30 度（可以根据需要调整）
				// 右臂旋转
				if (arm == HumanoidArm.RIGHT) {
					model.rightArm.xRot = -rotationAngle;  // 右臂旋转向中间
					model.rightArm.yRot = -rotationAngle; // 右臂绕 Y 轴旋转
				}
				// 左臂旋转
				else {
					model.leftArm.xRot = -rotationAngle;  // 左臂旋转向中间
					model.leftArm.yRot = rotationAngle;   // 左臂绕 Y 轴旋转（相反方向）
				}
			});

			@Override
			public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
				if (!itemStack.isEmpty()) {
					if (entityLiving.getUsedItemHand() == hand && entityLiving.getUseItemRemainingTicks() > 0) {
						return ITEM_C4;
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
				serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1,0.1f,0.1f),1),player.getX(),player.getY() + 2,player.getZ(),1,0,0,0,1);
			}
		}
	}

	@Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if(level.isClientSide) return InteractionResultHolder.pass(itemstack);
        BaseMap baseMap = FPSMCore.getInstance().getMapByPlayer(player);
        if(baseMap instanceof BlastModeMap<?> map) {
            if(!baseMap.isStart) {
                player.displayClientMessage(Component.translatable("blockoffensive.item.c4.use.fail.map.notStart"), true);
                return InteractionResultHolder.pass(itemstack);
            }
            BaseTeam team = baseMap.getMapTeams().getTeamByPlayer(player).orElse(null);
            if(team == null) {
                player.displayClientMessage(Component.translatable("blockoffensive.item.c4.use.fail.team.notInTeam"), true);
                return InteractionResultHolder.pass(itemstack);
            }
            // 检查玩家是否在指定区域内, 检查地图是否在爆炸状态中, 检查玩家是否在地上
            boolean canPlace = map.checkCanPlacingBombs(team.getFixedName()) && map.isBlasting() == 0 && player.onGround();
            boolean isInBombArea = map.checkPlayerIsInBombArea(player);
            if(canPlace && isInBombArea){
                player.startUsingItem(hand);
                return InteractionResultHolder.consume(itemstack);
            }else{
                if(!canPlace) {
                    player.displayClientMessage(Component.translatable("blockoffensive.item.c4.use.fail"), true);
                }else{
                    if(map.getBombAreaData().isEmpty()) {
                        player.displayClientMessage(Component.translatable("blockoffensive.item.c4.use.fail.noArea"), true);
                    }else{
                        player.displayClientMessage(Component.translatable("blockoffensive.item.c4.use.fail.notInArea"), true);
                    }
                }
                return InteractionResultHolder.pass(itemstack);
            }
        }else{
            player.displayClientMessage(Component.translatable("blockoffensive.item.c4.use.fail.noMap"), true);
            return InteractionResultHolder.pass(itemstack);
        }
    }

	@Override
	public void onUseTick(@NotNull Level pLevel, @NotNull LivingEntity pLivingEntity, @NotNull ItemStack pStack, int pRemainingUseDuration) {
		if (pLevel.isClientSide && Minecraft.getInstance().player != null && pLivingEntity.getUUID().equals(Minecraft.getInstance().player.getUUID())) {
			Minecraft.getInstance().options.keyUp.setDown(false);
			Minecraft.getInstance().options.keyLeft.setDown(false);
			Minecraft.getInstance().options.keyDown.setDown(false);
			Minecraft.getInstance().options.keyRight.setDown(false);
			Minecraft.getInstance().options.keyJump.setDown(false);
		}
		if (pLivingEntity instanceof Player player && player.isUsingItem() && pStack == player.getUseItem()) {
			if (pRemainingUseDuration % 8 == 0) {
				pLevel.playSound(null, player.getX(), player.getY(), player.getZ(), BOSoundRegister.click.get(), SoundSource.PLAYERS, 3.0F, 1.0F);
			}
		}
	}


	public @NotNull ItemStack finishUsingItem(@NotNull ItemStack pStack, @NotNull Level pLevel, @NotNull LivingEntity pLivingEntity) {
		if (pLivingEntity instanceof ServerPlayer player) {
			BaseMap map = FPSMCore.getInstance().getMapByPlayer(player);
			if (map instanceof BlastModeMap<?> blastModeMap) {
				boolean isInBombArea = blastModeMap.checkPlayerIsInBombArea(player);
				if (!isInBombArea) {
					player.displayClientMessage(Component.translatable("blockoffensive.item.c4.use.fail.notInArea"), true);
					return pStack;
				} else {
					BaseTeam team = map.getMapTeams().getTeamByPlayer(player).orElse(null);
					if (team != null && map instanceof ShopMap<?> shopMap) {
						shopMap.addPlayerMoney(player.getUUID(), 300);
					}
					CompositionC4Entity entityC4 = new CompositionC4Entity(pLevel, player.getX(), player.getY() + 0.25F, player.getZ(), player, blastModeMap);
					pLevel.addFreshEntity(entityC4);
					pLevel.playSound(null, player.getX(), player.getY(), player.getZ(), BOSoundRegister.planted.get(), SoundSource.PLAYERS, 3.0F, 1F);
					map.getMapTeams().getJoinedPlayers().forEach(data -> {
						data.getPlayer().ifPresent(serverPlayer->{
							serverPlayer.displayClientMessage(Component.translatable("blockoffensive.item.c4.planted").withStyle(ChatFormatting.RED),true);
						});
					});
					return ItemStack.EMPTY;
				}
			} else {
				return pStack;
			}
		}
		return pStack;
	}

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack pStack) {
        return UseAnim.CUSTOM;
    }

	@Override
	public int getUseDuration(@NotNull ItemStack itemstack) {
		return 80;
	}

}