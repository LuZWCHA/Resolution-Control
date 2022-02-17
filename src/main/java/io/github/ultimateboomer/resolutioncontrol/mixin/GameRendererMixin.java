package io.github.ultimateboomer.resolutioncontrol.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.ultimateboomer.resolutioncontrol.ResolutionControlMod;

@Mixin(value = GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(at = @At("HEAD"), method = "renderWorld")
	private void onRenderWorldBegin(CallbackInfo callbackInfo) {
		if(ResolutionControlMod.isInit()) {
			ResolutionControlMod.getInstance().setShouldScale(true);
		}
	}
	
	@Inject(at = @At("RETURN"), method = "renderWorld")
	private void onRenderWorldEnd(CallbackInfo callbackInfo) {
		if(ResolutionControlMod.isInit()) {
			ResolutionControlMod.getInstance().setShouldScale(false);
		}
	}
}
