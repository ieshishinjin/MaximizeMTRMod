// MaximizeMTRMod — F3 调试叠加：在调试屏幕底部追加 MMTR 状态信息

package io.github.mmtr.client.mixin;

import io.github.mmtr.client.debug.MmtrDebugHud;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugOverlayMixin {

    @Inject(method = "getSystemInformation", at = @At("RETURN"))
    private void mmtr$onSystemInfo(CallbackInfoReturnable<List<String>> cir) {
        List<String> lines = cir.getReturnValue();
        if (lines != null) {
            lines.addAll(MmtrDebugHud.getLines());
        }
    }
}
