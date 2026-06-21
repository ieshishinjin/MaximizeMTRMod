// MaximizeMTRMod — 方块实体渲染距离剔除
// 拦截 Minecraft 的 BlockEntityRenderDispatcher.render()，
// 只对 MTR 的方块实体做距离检查（instanceof BlockEntityExtension），
// 超过距离则取消渲染。屏蔽门（PSD/APG）、信号机、PIDS 显示屏、
// 站名牌等全部统一处理。

package io.github.mmtr.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.mmtr.client.lod.LODUtil;
import io.github.mmtr.config.MmtrConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class MTRBlockEntityRendererMixin {

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private <E extends BlockEntity> void mmtr$onRenderBlockEntity(
			E blockEntity, float tickDelta, PoseStack poseStack,
			MultiBufferSource multiBufferSource, CallbackInfo ci) {

		if (!(blockEntity instanceof BlockEntityExtension)) return;

		MmtrConfig cfg = MmtrConfig.getInstance();
		if (!cfg.enableBlockEntityCulling) return;

		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null) return;

		BlockPos pos = blockEntity.getBlockPos();
		if (!LODUtil.shouldRenderBlockEntity(
				player.getX(), player.getY(), player.getZ(),
				pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
			ci.cancel();
		}
	}
}
