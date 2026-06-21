// MaximizeMTRMod — 配置文件：所有优化参数集中管理，自动读写 config/mmtr.json

package io.github.mmtr.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MmtrConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("mmtr-config");
    private static final Path CONFIG_PATH = Path.of("config", "mmtr.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static MmtrConfig INSTANCE;

    //  渲染距离（单位：米/方块）

    // 列车在此距离内渲染完整模型（车身、内饰、灯光全部绘制）；
    // 超过此距离但未达到 vehicleMaxRenderDistance 时仅渲染外壳
    public int vehicleFullRenderDistance = 40;

    // 列车超过此距离后完全停止渲染，不产生任何 GPU 开销。
    // 此值配合帧交错（distantVehicleFrameCycle）可进一步降频
    public int vehicleMaxRenderDistance = 128;

    // 屏蔽门（PSD/APG）、信号机、PIDS 显示屏、站名牌等方块实体的最大渲染距离。
    // 地铁站内方块实体密集，过大距离会导致大量 draw call
    public int blockEntityMaxRenderDistance = 56;

    // 电梯井的最大渲染距离。电梯模型面数较高但数量较少
    public int liftMaxRenderDistance = 80;

    //  数据同步节流

    // 玩家在此距离内有列车/电梯时，保持正常的 HTTP 同步频率（每 tick）
    public int fullSyncDistance = 40;

    // 玩家在此距离内但超出 fullSyncDistance 时，使用降频同步
    public int reducedSyncDistance = 80;

    // 降频模式的同步间隔（tick 数）。每 12 tick ≈ 0.6 秒。
    // 增大此值可减少 CPU 开销，但列车位置更新会变慢
    public int reducedSyncInterval = 12;

    // 玩家超过此距离后完全停止 HTTP 同步，直到靠近再恢复
    public int noSyncDistance = 160;

    //  区块加载预测

    // 启用后将监控列车方向并检测前方区块是否已加载；
    // 若检测到即将加载区块，临时收紧渲染距离以平滑掉帧
    public boolean enableChunkPreloading = true;

    // 沿列车前进方向预检测的区块数量（前方最多 N 个区块）
    public int preloadChunkCount = 5;

    //  渲染帧交错（频率降级）

    // 启用后，远处列车在部分帧上自动缩减渲染距离，
    // 相当于让远处物体每隔 N 帧才完整渲染一次。
    // 人眼对远处细节不敏感，此优化几乎无感知但 GPU 减半
    public boolean enableRenderFrequencyReduction = true;

    // 帧交错周期：
    //   2 = 隔帧降距（远处列车每 2 帧完全渲染一次）
    //   3 = 每 3 帧一次（更激进）
    //   1 或更小 = 关闭交错
    public int distantVehicleFrameCycle = 2;

    //  功能开关

    // 启用列车/轨道 LOD 剔除
    public boolean enableDistanceLOD = true;

    // 启用方块实体（PSD/信号/PIDS）距离剔除
    public boolean enableBlockEntityCulling = true;

    // 启用数据同步节流
    public boolean enableDataSyncOptimization = true;

    // 启用电梯距离剔除
    public boolean enableLiftCulling = true;

    // 启用轨道渲染剔除
    public boolean enableRailCulling = true;

    //  调试

    // 开启后在屏幕显示渲染距离标记和剔除数量
    // 用于调参时验证优化效果
    public boolean debugRenderDistance = false;

    //  单例 + 持久化
    public static MmtrConfig getInstance() {
        if (INSTANCE == null) INSTANCE = load();
        return INSTANCE;
    }

    public static void reload() {
        INSTANCE = load();
        LOGGER.info("Config reloaded");
    }

    private static MmtrConfig load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                MmtrConfig cfg = GSON.fromJson(json, MmtrConfig.class);
                if (cfg != null) {
                    LOGGER.info("Loaded config from {}", CONFIG_PATH.toAbsolutePath());
                    return cfg;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load config from {}, using defaults", CONFIG_PATH.toAbsolutePath(), e);
        }
        MmtrConfig defaults = new MmtrConfig();
        defaults.save();
        return defaults;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            LOGGER.error("Failed to save config to {}", CONFIG_PATH.toAbsolutePath(), e);
        }
    }
}
