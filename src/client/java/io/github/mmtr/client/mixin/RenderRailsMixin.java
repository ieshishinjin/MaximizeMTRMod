// MaximizeMTRMod — 轨道渲染距离剔除
// 无轨道或距离太远时跳过轨道渲染；
// 拿着 MTR 物品时始终渲染（放置预览需要显示）。
// 轨道数量多、每帧渲染开销大，此优化效果明显。

package io.github.mmtr.client.mixin;

import io.github.mmtr.client.lod.LODUtil;
import io.github.mmtr.client.util.MtrItemUtil;
import io.github.mmtr.config.MmtrConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.render.RenderRails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderRails.class, remap = false)
public abstract class RenderRailsMixin {

	@Unique
	private static final String MMTR_TARGET = "render()V";

	@Inject(method = MMTR_TARGET, at = @At("HEAD"), cancellable = true, remap = false)
	private static void mmtr$beforeRenderRails(CallbackInfo ci) {
		MmtrConfig cfg = MmtrConfig.getInstance();
		if (!cfg.enableRailCulling) return;

		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null) return;

		if (MtrItemUtil.isHoldingMtrItem(player)) return;

		MinecraftClientData data = MinecraftClientData.getInstance();
		if (data.railWrapperList == null || data.railWrapperList.isEmpty()) {
			ci.cancel();
			return;
		}

		double px = player.getX(), pz = player.getZ();
		int maxDist = LODUtil.adjustedDistance(cfg.railMaxRenderDistance);
		double maxDistSq = (double) maxDist * maxDist;

		for (var wrapper : data.railWrapperList.values()) {
			if (wrapper == null) continue;
			var sv = wrapper.startVector;
			var ev = wrapper.endVector;
			if (sv != null && rangeSq(px, pz, sv.x, sv.z) <= maxDistSq) return;
			if (ev != null && rangeSq(px, pz, ev.x, ev.z) <= maxDistSq) return;
			if (sv != null && ev != null && rangeSq(px, pz, (sv.x + ev.x) * 0.5, (sv.z + ev.z) * 0.5) <= maxDistSq) return;
		}
		ci.cancel();
	}

	// 二维距离平方（内联工具方法，避免重复写公式）
	@Unique
	private static double rangeSq(double px, double pz, double tx, double tz) {
		double dx = px - tx, dz = pz - tz;
		return dx * dx + dz * dz;
	}
}
