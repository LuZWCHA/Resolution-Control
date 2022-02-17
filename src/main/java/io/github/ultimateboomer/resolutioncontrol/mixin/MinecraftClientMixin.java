package io.github.ultimateboomer.resolutioncontrol.mixin;

import io.github.ultimateboomer.resolutioncontrol.ResolutionControlMod;
import net.minecraft.client.GameConfiguration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IWindowEventListener;
import net.minecraft.profiler.ISnooperInfo;
import net.minecraft.util.concurrent.RecursiveEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Minecraft.class)
public abstract class MinecraftClientMixin{

	@Inject(method = "<init>", at = @At(value = "RETURN", target = "Lnet/minecraft/client/shader/Framebuffer;<init>(IIZZ)V"))
	private void onInitFramebuffer(GameConfiguration gameConfiguration, CallbackInfo ci) {
		ResolutionControlMod mod = ResolutionControlMod.getInstance();
		if (mod != null && mod.isScreenshotFramebufferAlwaysAllocated()) {
			mod.initScreenshotFramebuffer();
		}
	}

}
