// MaximizeMTRMod — F3 调试叠加层：显示优化状态和压力值

package io.github.mmtr.client.debug;

import io.github.mmtr.client.chunk.ChunkPreloadManager;
import io.github.mmtr.config.MmtrConfig;

import java.util.ArrayList;
import java.util.List;

public final class MmtrDebugHud {

    private MmtrDebugHud() {}

    public static List<String> getLines() {
        List<String> lines = new ArrayList<>();
        lines.add("");

        MmtrConfig cfg = MmtrConfig.getInstance();
        double pressure = ChunkPreloadManager.INSTANCE.getPressure();

        lines.add("§b[MMTR]§r 性能压力: " + formatPressure(pressure));

        // 渲染状态
        StringBuilder sb = new StringBuilder("§b[MMTR]§r 渲染:");
        sb.append(" 列车").append(cfg.enableDistanceLOD ? "§a" : "§c").append(cfg.vehicleMaxRenderDistance).append("§r");
        sb.append(" PSD/信号").append(cfg.enableBlockEntityCulling ? "§a" : "§c").append(cfg.blockEntityMaxRenderDistance).append("§r");
        sb.append(" 电梯").append(cfg.enableLiftCulling ? "§a" : "§c").append(cfg.liftMaxRenderDistance).append("§r");
        lines.add(sb.toString());

        // 同步+帧交错状态
        StringBuilder sb2 = new StringBuilder("§b[MMTR]§r 同步:");
        if (cfg.enableDataSyncOptimization) {
            sb2.append("§a降频(").append(cfg.reducedSyncInterval).append("tick)§r");
        } else {
            sb2.append("§c关§r");
        }
        sb2.append(" 帧交错:");
        if (cfg.enableRenderFrequencyReduction) {
            sb2.append("§a开(").append(cfg.distantVehicleFrameCycle).append("帧)§r");
        } else {
            sb2.append("§c关§r");
        }
        sb2.append(" 轨道:").append(cfg.enableRailCulling ? "§a" : "§c").append(cfg.enableRailCulling ? "开" : "关").append("§r");
        lines.add(sb2.toString());

        return lines;
    }

    private static String formatPressure(double p) {
        if (p < 0.01) return "§a无§r";
        if (p < 0.3) return "§e" + String.format("%.1f", p) + "§r";
        if (p < 0.6) return "§6" + String.format("%.1f", p) + "§r";
        return "§c" + String.format("%.1f", p) + "§r";
    }
}
