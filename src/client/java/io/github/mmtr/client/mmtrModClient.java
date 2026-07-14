// MaximizeMTRMod — 客户端入口：初始化配置、注册区块预加载 Tick 处理器、注册命令

package io.github.mmtr.client;

import io.github.mmtr.client.chunk.ChunkPreloadManager;
import io.github.mmtr.client.command.MmtrCommand;
import io.github.mmtr.config.MmtrConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class mmtrModClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("mmtr-client");

	// 记录上一帧的世界引用，检测维度切换以重置区块预加载状态
	private static ClientLevel lastLevel = null;

	@Override
	public void onInitializeClient() {
		MmtrConfig cfg = MmtrConfig.getInstance();
		LOGGER.info("MaximizeMTRMod client initialized (render dist: {}, BER dist: {}, sync interval: {})",
				cfg.vehicleMaxRenderDistance,
				cfg.blockEntityMaxRenderDistance,
				cfg.reducedSyncInterval);

		// 每 Tick 驱动区块预测管理器，用于检测列车→未加载区块方向
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			// 检测维度/世界切换，立即重置性能压力
			if (client.level != lastLevel) {
				if (lastLevel != null) {
					ChunkPreloadManager.INSTANCE.reset();
				}
				lastLevel = client.level;
			}
			ChunkPreloadManager.INSTANCE.tick();
		});

		// 注册 /mmtr 命令（reload 等子命令）
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			MmtrCommand.register(dispatcher);
		});
	}
}
