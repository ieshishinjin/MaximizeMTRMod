// MaximizeMTRMod — 轨道渲染距离剔除
// 无轨道或距离太远时跳过轨道渲染；
// 拿着 MTR 物品时始终渲染（放置预览需要显示）。
// 轨道数量多、每帧渲染开销大，此优化效果明显。

package io.github.mmtr.client.mixin;

import io.github.mmtr.client.lod.LODUtil;
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

		if (mmtr$isHoldingMtrItem(player)) return;

		MinecraftClientData data = MinecraftClientData.getInstance();
		if (data.railWrapperList == null || data.railWrapperList.isEmpty()) {
			ci.cancel();
			return;
		}

		double px = player.getX(), pz = player.getZ();
		int maxDist = LODUtil.adjustedDistance(128);
		double maxDistSq = (double) maxDist * maxDist;

		for (var wrapper : data.railWrapperList.values()) {
			if (wrapper == null) continue;
			var sv = wrapper.startVector;
			if (sv != null && (px - sv.x) * (px - sv.x) + (pz - sv.z) * (pz - sv.z) <= maxDistSq) return;
			var ev = wrapper.endVector;
			if (ev != null && (px - ev.x) * (px - ev.x) + (pz - ev.z) * (pz - ev.z) <= maxDistSq) return;
		}
		ci.cancel();
	}

	// 检查主手或副手是否持有 mtr 命名空间的物品
	@Unique
	private static boolean mmtr$isHoldingMtrItem(Player player) {
		var main = player.getMainHandItem();
		if (!main.isEmpty() && mmtr$isMtrNamespace(main)) return true;
		var off = player.getOffhandItem();
		return !off.isEmpty() && mmtr$isMtrNamespace(off);
	}

	@Unique
	private static boolean mmtr$isMtrNamespace(net.minecraft.world.item.ItemStack stack) {
		var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
		return id != null && "mtr".equals(id.getNamespace());
	}
}
