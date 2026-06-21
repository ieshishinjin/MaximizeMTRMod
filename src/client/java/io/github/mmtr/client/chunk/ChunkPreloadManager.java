// MaximizeMTRMod — 区块加载性能平滑器
//
// 客户端无法强制服务器加载区块，但可以通过追踪列车位置和行驶方向，
// 预测即将进入未加载区块的时刻，并生成一个"性能压力"信号。
// 渲染 mixin 读取此信号后临时收紧所有渲染距离，在区块加载期间
// 降低 GPU 负载，从而减轻掉帧感。
//
// 压力值范围 0.0（无压力）~ 1.0（最大压力）

package io.github.mmtr.client.chunk;

import io.github.mmtr.config.MmtrConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import org.mtr.core.tool.Vector;
import org.mtr.mapping.holder.ClientChunkManager;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.VehicleExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChunkPreloadManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("mmtr-chunk");
    public static final ChunkPreloadManager INSTANCE = new ChunkPreloadManager();

    // 每辆列车上次记录的头部位置，用于计算行驶方向
    private final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<Vector> previousPositions =
            new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();

    // 当前性能压力 (0.0 ~ 1.0)
    private double performancePressure = 0.0;
    private long lastTickTime = 0;

    // 压力每 200ms 检测一次（避免每 tick 都做全量遍历）
    private static final long   TICK_INTERVAL_MS    = 200;
    // 低于此速度的列车视为静止，不做预测
    private static final double MIN_SPEED           = 0.05;
    // 最大前瞻距离（方块），避免超出实际加载范围太多
    private static final double LOOK_AHEAD_FACTOR   = 30.0;
    // 每检测到一个未加载区块时压力增量
    private static final double PRESSURE_INC        = 0.15;
    // 每 tick 压力的指数衰减系数
    private static final double PRESSURE_DECAY      = 0.92;

    private ChunkPreloadManager() {}

    public double getPressure() {
        return performancePressure;
    }

    // 每 tick 调用一次（通过 Fabric ClientTickEvents 注册）
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Player player = mc.player;

        performancePressure *= PRESSURE_DECAY;
        if (performancePressure < 0.01) performancePressure = 0.0;

        if (level == null || player == null) return;
        if (!MmtrConfig.getInstance().enableChunkPreloading) return;

        long now = System.currentTimeMillis();
        if (now - lastTickTime < TICK_INTERVAL_MS) return;
        lastTickTime = now;

        MinecraftClientData data = MinecraftClientData.getInstance();

        for (VehicleExtension vehicle : data.vehicles) {
            if (vehicle == null || !vehicle.isMoving()) continue;
            if (vehicle.getSpeed() < MIN_SPEED) continue;

            Vector headPos = vehicle.getHeadPosition();
            if (headPos == null) continue;

            long id = vehicle.getId();
            Vector prevPos = previousPositions.get(id);

            if (prevPos != null) {
                double dx = headPos.x - prevPos.x;
                double dz = headPos.z - prevPos.z;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist < 0.1) continue;

                // 标准化方向向量
                double ndx = dx / dist;
                double ndz = dz / dist;

                // 计算前瞻位置：速度越快、看得越远
                double lookAhead = Math.min(Math.abs(vehicle.getSpeed()) * 15.0, LOOK_AHEAD_FACTOR);
                double aheadX = headPos.x + ndx * lookAhead;
                double aheadZ = headPos.z + ndz * lookAhead;

                int chunkX = (int) Math.floor(aheadX / 16.0);
                int chunkZ = (int) Math.floor(aheadZ / 16.0);

                // 通过 MTR 的 ClientChunkManager 判断区块是否已加载
                ClientChunkCache chunkCache = level.getChunkSource();
                boolean loaded = new ClientChunkManager(chunkCache).isChunkLoaded(chunkX, chunkZ);
                if (!loaded) {
                    performancePressure = Math.min(1.0, performancePressure + PRESSURE_INC);
                }
            }
            previousPositions.put(id, headPos);
        }

        // 定期清理已消失列车的旧位置记录
        if (previousPositions.size() > data.vehicles.size() * 2 + 10) {
            previousPositions.clear();
        }
    }
}
