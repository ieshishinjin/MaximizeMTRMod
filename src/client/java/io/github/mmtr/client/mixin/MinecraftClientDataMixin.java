// MaximizeMTRMod — 数据同步频率节流
// 当玩家远离所有列车和电梯时，降低 MinecraftClientData.sync()
// 的调用频率，减少 HTTP 请求和 CPU 开销。
// 手持 MTR 物品时始终保持全速同步。

package io.github.mmtr.client.mixin;

import io.github.mmtr.client.lod.LODUtil;
import io.github.mmtr.config.MmtrConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.mtr.core.tool.Vector;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.VehicleExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MinecraftClientData.class, remap = false)
public abstract class MinecraftClientDataMixin {

	@Unique
	private static int mmtr$syncSkipCounter = 0;

	@Inject(method = "sync", at = @At("HEAD"), cancellable = true, remap = false)
	private void mmtr$beforeSync(CallbackInfo ci) {
		MmtrConfig cfg = MmtrConfig.getInstance();
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null) return;

		// 手持 MTR 物品时不节流（玩家在操作列车/建筑轨道）
		if (mmtr$isHoldingMtrItem(player)) return;

		double px = player.getX(), pz = player.getZ();
		MinecraftClientData self = (MinecraftClientData) (Object) this;

		if (mmtr$hasNearbyTransit(self, px, pz, cfg)) {
			mmtr$syncSkipCounter = 0;
			return;
		}
		if (!cfg.enableDataSyncOptimization) return;

		mmtr$syncSkipCounter++;
		if (mmtr$syncSkipCounter % cfg.reducedSyncInterval != 0) {
			ci.cancel();
		}
	}

	@Unique
	private static boolean mmtr$hasNearbyTransit(MinecraftClientData self, double px, double pz, MmtrConfig cfg) {
		int dist = LODUtil.adjustedDistance(cfg.fullSyncDistance);
		double sq = (double) dist * dist;

		if (self.vehicles != null) {
			for (VehicleExtension veh : self.vehicles) {
				if (veh == null) continue;
				Vector head = veh.getHeadPosition();
				if (head == null) continue;
				double dx = px - head.x, dz = pz - head.z;
				if (dx * dx + dz * dz <= sq) return true;
			}
		}
		if (self.liftWrapperList != null) {
			for (var wrapper : self.liftWrapperList.values()) {
				if (wrapper == null) continue;
				var lift = wrapper.getLift();
				if (lift == null) continue;
				if (lift instanceof LiftAccessor) {
					var acc = (LiftAccessor) lift;
					var min = acc.getMinPosition();
					var max = acc.getMaxPosition();
					if (min == null || max == null) continue;
					double cx = (min.getX() + max.getX()) / 2.0, cz = (min.getZ() + max.getZ()) / 2.0;
					double dx = px - cx, dz = pz - cz;
					if (dx * dx + dz * dz <= sq) return true;
				}
			}
		}
		return false;
	}

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
