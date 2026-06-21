// MaximizeMTRMod - Lift位置访问器（@Accessor）
// by ieshishinjin

package io.github.mmtr.client.mixin;

import org.mtr.core.data.Lift;
import org.mtr.core.data.Position;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Lift.class, remap = false)
public interface LiftAccessor {
	@Accessor("minPosition")
	Position getMinPosition();

	@Accessor("maxPosition")
	Position getMaxPosition();
}
