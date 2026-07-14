// MaximizeMTRMod — MTR 物品判断工具类
// 检查玩家主手/副手是否持有 mtr 命名空间的物品，
// 用于渲染剔除和同步节流中需要保留全功能的情形。

package io.github.mmtr.client.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class MtrItemUtil {

	private static final String MTR_NAMESPACE = "mtr";

	private MtrItemUtil() {}

	// 检查玩家主手或副手是否持有 mtr 命名空间的物品
	public static boolean isHoldingMtrItem(Player player) {
		if (player == null) return false;
		return isMtrItem(player.getMainHandItem()) || isMtrItem(player.getOffhandItem());
	}

	private static boolean isMtrItem(ItemStack stack) {
		if (stack.isEmpty()) return false;
		var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return id != null && MTR_NAMESPACE.equals(id.getNamespace());
	}
}
