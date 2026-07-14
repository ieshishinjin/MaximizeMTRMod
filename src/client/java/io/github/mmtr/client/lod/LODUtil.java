// MaximizeMTRMod — LOD 工具类：距离计算、帧交错、性能压力融合
//
// 所有渲染剔除 mixin 都通过此类的 adjustedDistance() 获取实际阈值，
// 该阈值会自动考虑：
//   - ChunkPreloadManager 的性能压力（区块即将加载时收紧）
//   - 帧交错计数器（远处列车隔帧降距，Pixel 级别的频率降级）
//
// 性能：adjustedDistance() 的压力量在 onFrameStart() 中预计算，
// 每帧只读一次 getPressure()，后续所有调用复用缓存值。

package io.github.mmtr.client.lod;

import io.github.mmtr.client.chunk.ChunkPreloadManager;
import io.github.mmtr.config.MmtrConfig;
import org.mtr.core.tool.Vector;
import org.mtr.mapping.holder.Vector3d;

public final class LODUtil {

	public enum LODLevel {
		FULL,
		SIMPLIFIED,
		CULLED
	}

	// 帧计数器，每次 RenderVehicles.render() 被调用时递增；
	// 用于实现帧交错：在部分帧上收紧渲染距离，达到远处物体降频效果
	private static int mmtr$frameCounter = 0;

	// 压力量缓存，在 onFrameStart() 中预计算；避免每帧数百次重复 getPressure() 调用
	private static double cachedPressureFactor = 1.0;

	private LODUtil() {}

	// 每次渲染帧开始时调用，推进帧计数器 + 刷新压力量缓存
	public static void onFrameStart() {
		mmtr$frameCounter = (mmtr$frameCounter + 1) & 0x3FFFFFFF;
		cachedPressureFactor = 1.0 - ChunkPreloadManager.INSTANCE.getPressure() * 0.5;
	}

	// 获取调整后的渲染距离（简洁版，不带帧交错）
	public static int adjustedDistance(int baseDistance) {
		return adjustedDistance(baseDistance, false);
	}

	// 获取调整后的渲染距离（完整版）
	//   baseDistance   — 配置中的基础距离
	//   applyInterlace — 是否应用帧交错降距（仅车辆渲染使用）
	// 返回值：经过性能压力和帧交错缩放后的实际距离，最低 32 格
	public static int adjustedDistance(int baseDistance, boolean applyInterlace) {
		double factor = cachedPressureFactor;

		if (applyInterlace && MmtrConfig.getInstance().enableRenderFrequencyReduction) {
			int cycle = MmtrConfig.getInstance().distantVehicleFrameCycle;
			if (cycle > 1 && mmtr$frameCounter % cycle != 0) {
				factor *= 0.65;
			}
		}

		return Math.max((int) (baseDistance * factor), 32);
	}

	// 判断列车 LOD 等级（应用帧交错）
	public static LODLevel getVehicleLOD(Vector3d playerPos, Vector vehiclePos) {
		if (playerPos == null || vehiclePos == null) return LODLevel.CULLED;
		return getVehicleLOD(
				playerPos.getXMapped(), playerPos.getYMapped(), playerPos.getZMapped(),
				vehiclePos.x, vehiclePos.y, vehiclePos.z);
	}

	public static LODLevel getVehicleLOD(double px, double py, double pz,
	                                      double vx, double vy, double vz) {
		MmtrConfig cfg = MmtrConfig.getInstance();
		if (!cfg.enableDistanceLOD) return LODLevel.FULL;

		double dx = px - vx, dy = py - vy, dz = pz - vz;
		double distSq = dx * dx + dy * dy + dz * dz;

		// 车辆距离判断应用帧交错降距
		int fullDist = adjustedDistance(cfg.vehicleFullRenderDistance, true);
		int maxDist  = adjustedDistance(cfg.vehicleMaxRenderDistance, true);

		if (distSq <= (double) fullDist * fullDist) return LODLevel.FULL;
		else if (distSq <= (double) maxDist * maxDist) return LODLevel.SIMPLIFIED;
		else return LODLevel.CULLED;
	}

	// 判断方块实体是否应在当前帧渲染（不应用帧交错）
	public static boolean shouldRenderBlockEntity(
			double px, double py, double pz,
			double bx, double by, double bz) {
		MmtrConfig cfg = MmtrConfig.getInstance();
		if (!cfg.enableBlockEntityCulling) return true;

		double dx = px - bx, dy = py - by, dz = pz - bz;
		int maxDist = adjustedDistance(cfg.blockEntityMaxRenderDistance);
		return (dx * dx + dy * dy + dz * dz) <= (double) maxDist * maxDist;
	}

	// 判断电梯是否应在当前帧渲染（不应用帧交错）
	public static boolean shouldRenderLift(Vector3d playerPos, double lx, double ly, double lz) {
		MmtrConfig cfg = MmtrConfig.getInstance();
		if (!cfg.enableLiftCulling) return true;

		double dx = playerPos.getXMapped() - lx;
		double dy = playerPos.getYMapped() - ly;
		double dz = playerPos.getZMapped() - lz;
		int maxDist = adjustedDistance(cfg.liftMaxRenderDistance);
		return (dx * dx + dy * dy + dz * dz) <= (double) maxDist * maxDist;
	}

	// 同步等级（给 MinecraftClientDataMixin 使用，不应用帧交错）
	public enum SyncLevel { SYNC_FULL, SYNC_REDUCED, SYNC_NONE }

	public static SyncLevel getSyncLevel(double px, double pz, double tx, double tz) {
		MmtrConfig cfg = MmtrConfig.getInstance();
		if (!cfg.enableDataSyncOptimization) return SyncLevel.SYNC_FULL;

		double dx = px - tx, dz = pz - tz, distSq = dx * dx + dz * dz;
		int fullDist = adjustedDistance(cfg.fullSyncDistance);
		int reducedDist = adjustedDistance(cfg.reducedSyncDistance);

		if (distSq <= (double) fullDist * fullDist) return SyncLevel.SYNC_FULL;
		else if (distSq <= (double) reducedDist * reducedDist) return SyncLevel.SYNC_REDUCED;
		else return SyncLevel.SYNC_NONE;
	}
}
