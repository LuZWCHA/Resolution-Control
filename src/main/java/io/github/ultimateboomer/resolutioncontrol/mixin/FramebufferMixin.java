package io.github.ultimateboomer.resolutioncontrol.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.ultimateboomer.resolutioncontrol.ResolutionControlMod;
import io.github.ultimateboomer.resolutioncontrol.util.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL45;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.IntBuffer;

@Mixin(value = Framebuffer.class, remap = false)
public abstract class FramebufferMixin {
    @Unique private boolean isMipmapped;
    @Unique private float scaleMultiplier;

    @Shadow
    public abstract int getFrameBufferTexture();

    @Inject(method = "createBuffers", at = @At("HEAD"))
    private void onInitFbo(int width, int height, boolean getError, CallbackInfo ci) {
        scaleMultiplier = (float) width / Minecraft.getInstance().getMainWindow().getWidth();
        isMipmapped = Config.getInstance().mipmapHighRes && scaleMultiplier > 2.0f;
    }


    @Redirect(method = "*", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;texParameter(III)V"))
    private void onSetTexFilter(int target, int pname, int param) {

        if(ResolutionControlMod.isInit())
            if (pname == GL11.GL_TEXTURE_MIN_FILTER) {
            GlStateManager.texParameter(target, pname,
                    ResolutionControlMod.getInstance().getUpscaleAlgorithm().getId(isMipmapped));
            } else if (pname == GL11.GL_TEXTURE_MAG_FILTER) {
                GlStateManager.texParameter(target, pname,
                        ResolutionControlMod.getInstance().getDownscaleAlgorithm().getId(false));
            } else if (pname == GL11.GL_TEXTURE_WRAP_S || pname == GL11.GL_TEXTURE_WRAP_T) {
                // Fix linear scaling creating black borders
                GlStateManager.texParameter(target, pname, GL12.GL_CLAMP_TO_EDGE);
            } else {
                GlStateManager.texParameter(target, pname, param);
            }
    }

    @Redirect(method = "createBuffers", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;texImage2D(IIIIIIIILjava/nio/IntBuffer;)V")
    )
    private void onTexImage(int target, int level, int internalFormat, int width, int height, int border, int format,
                            int type, IntBuffer pixels) {
        if (isMipmapped) {
            int mipmapLevel = MathHelper.ceil(Math.log(scaleMultiplier) / Math.log(2));
            for (int i = 0; i < mipmapLevel; i++) {
                GlStateManager.texImage2D(target, i, internalFormat,
                       width << i, height << i,
                        border, format, type, pixels);
            }
        } else {
            GlStateManager.texImage2D(target, 0, internalFormat, width, height, border, format, type, pixels);
        }

    }

    @Inject(method = "framebufferRenderExtRaw", at = @At("HEAD"))
    private void onDraw(int width, int height, boolean bl, CallbackInfo ci) {
        if (isMipmapped) {
            GlStateManager.bindTexture(this.getFrameBufferTexture());
            GL45.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        }
    }
}
