// MaximizeMTRMod — 主入口：预加载配置，标记 Mod 启动完成

package io.github.mmtr;

import io.github.mmtr.config.MmtrConfig;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class mmtrMod implements ModInitializer {
	public static final String MOD_ID = "mmtr";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// 在服务端/客户端共用阶段触发配置加载
		MmtrConfig.getInstance();
		LOGGER.info("MaximizeMTRMod loaded");
	}

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}
}
