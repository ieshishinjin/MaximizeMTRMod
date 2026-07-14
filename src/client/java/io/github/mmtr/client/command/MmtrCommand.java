// MaximizeMTRMod — 客户端命令：/mmtr reload
// 允许运行时重载 config/mmtr.json，无需重启游戏

package io.github.mmtr.client.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.mmtr.config.MmtrConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public final class MmtrCommand {

	private static final String PREFIX = "§b[MMTR]§r ";

	private MmtrCommand() {}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(ClientCommandManager.literal("mmtr")
				.then(ClientCommandManager.literal("reload")
						.executes(ctx -> {
							MmtrConfig.reload();
							ctx.getSource().sendFeedback(Component.literal(PREFIX + "配置已重载 §a✓"));
							return 1;
						})
				)
		);
	}
}
