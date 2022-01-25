package io.github.ultimateboomer.resolutioncontrol.mixin;

import io.github.ultimateboomer.resolutioncontrol.ResolutionControlMod;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.renderer.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Shadow(remap = false)
    private Framebuffer entityOutlineFramebuffer;

    @Inject(at = @At("RETURN"), method = "makeEntityOutlineShader", remap = false)
    private void onLoadEntityOutlineShader(CallbackInfo ci) {
        if(ResolutionControlMod.isInit())
            ResolutionControlMod.getInstance().resizeMinecraftFramebuffers();
    }

    @Inject(at = @At("RETURN"), method = "resetFrameBuffers", remap = false)
    private void onOnResized(CallbackInfo ci) {
        if (entityOutlineFramebuffer == null) return;
        if(ResolutionControlMod.isInit())
            ResolutionControlMod.getInstance().resizeMinecraftFramebuffers();
    }

//    @Inject(at = @At("RETURN"), method = "loadTransparencyShader")
//    private void onLoadTransparencyShader(CallbackInfo ci) {
//        ResolutionControlMod.getInstance().resizeMinecraftFramebuffers();
//    }
}
