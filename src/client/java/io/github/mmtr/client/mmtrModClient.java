// MaximizeMTRMod — 客户端入口：初始化配置、注册区块预加载 Tick 处理器

package io.github.mmtr.client;

import io.github.mmtr.client.chunk.ChunkPreloadManager;
import io.github.mmtr.config.MmtrConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class mmtrModClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("mmtr-client");

	@Override
	public void onInitializeClient() {
		MmtrConfig cfg = MmtrConfig.getInstance();
		LOGGER.info("MaximizeMTRMod client initialized (render dist: {}, BER dist: {}, sync interval: {})",
				cfg.vehicleMaxRenderDistance,
				cfg.blockEntityMaxRenderDistance,
				cfg.reducedSyncInterval);

		// 每 Tick 驱动区块预测管理器，用于检测列车→未加载区块方向
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			ChunkPreloadManager.INSTANCE.tick();
		});
	}
}
