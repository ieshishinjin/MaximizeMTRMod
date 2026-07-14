// MaximizeMTRMod — 列车渲染距离剔除
// 全部列车超出最大距离时跳过渲染。

package io.github.mmtr.client.mixin;

import io.github.mmtr.client.lod.LODUtil;
import io.github.mmtr.config.MmtrConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.mtr.core.data.Vehicle;
import org.mtr.core.tool.Vector;
import org.mtr.mapping.holder.Vector3d;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.render.RenderVehicles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderVehicles.class, remap = false)
public abstract class RenderVehiclesMixin {

	@Unique
	private static final String MMTR_TARGET = "render(JLorg/mtr/mapping/holder/Vector3d;)V";

	@Inject(method = MMTR_TARGET, at = @At("HEAD"), cancellable = true, remap = false)
	private static void mmtr$beforeRenderVehicles(long timerMillis, Vector3d playerPos, CallbackInfo ci) {
		LODUtil.onFrameStart();
		MmtrConfig cfg = MmtrConfig.getInstance();
		if (!cfg.enableDistanceLOD) return;

		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null) return;

		double px = player.getX(), py = player.getY(), pz = player.getZ();

		MinecraftClientData data = MinecraftClientData.getInstance();
		if (data.vehicles.isEmpty()) return;

		int maxDist = LODUtil.adjustedDistance(cfg.vehicleMaxRenderDistance);
		double sq = (double) maxDist * maxDist;

		for (VehicleExtension vehicle : data.vehicles) {
			if (vehicle == null) continue;
			Vector headPos = ((Vehicle) vehicle).getHeadPosition();
			if (headPos == null) continue;
			double dx = px - headPos.x, dy = py - headPos.y, dz = pz - headPos.z;
			if (dx * dx + dy * dy + dz * dz <= sq) return;
		}
		ci.cancel();
	}
}
