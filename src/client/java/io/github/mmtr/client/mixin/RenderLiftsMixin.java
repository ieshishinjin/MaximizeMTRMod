// MaximizeMTRMod — 电梯渲染距离剔除
// 当所有电梯都远离玩家时取消渲染调用。
// 利用 @Accessor 获取 Lift 内部 minPosition/maxPosition 来计算中心坐标。

package io.github.mmtr.client.mixin;

import io.github.mmtr.client.lod.LODUtil;
import io.github.mmtr.config.MmtrConfig;
import net.minecraft.client.Minecraft;
import org.mtr.core.data.Lift;
import org.mtr.core.data.Position;
import org.mtr.mapping.holder.Vector3d;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.render.RenderLifts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderLifts.class, remap = false)
public abstract class RenderLiftsMixin {

	@Unique
	private static final String MMTR_TARGET = "render(JLorg/mtr/mapping/holder/Vector3d;)V";

	@Inject(method = MMTR_TARGET, at = @At("HEAD"), cancellable = true, remap = false)
	private static void mmtr$beforeRenderLifts(long timerMillis, Vector3d playerPos, CallbackInfo ci) {
		MmtrConfig cfg = MmtrConfig.getInstance();
		if (!cfg.enableLiftCulling) return;

		Minecraft mc = Minecraft.getInstance();
		net.minecraft.world.entity.player.Player player = mc.player;
		if (player == null) return;

		double px = player.getX(), pz = player.getZ();

		MinecraftClientData data = MinecraftClientData.getInstance();
		if (data.liftWrapperList == null || data.liftWrapperList.isEmpty()) return;

		int maxDist = LODUtil.adjustedDistance(cfg.liftMaxRenderDistance);
		double maxDistSq = (double) maxDist * maxDist;

		for (var entry : data.liftWrapperList.values()) {
			if (entry == null) continue;
			Lift lift = entry.getLift();
			if (lift == null) continue;
			if (lift instanceof LiftAccessor) {
				LiftAccessor acc = (LiftAccessor) lift;
				Position min = acc.getMinPosition(), max = acc.getMaxPosition();
				if (min == null || max == null) continue;
				double cx = (min.getX() + max.getX()) / 2.0;
				double cz = (min.getZ() + max.getZ()) / 2.0;
				double dx = px - cx, dz = pz - cz;
				if (dx * dx + dz * dz <= maxDistSq) return;
			}
		}
		ci.cancel();
	}
}
