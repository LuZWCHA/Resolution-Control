package io.github.ultimateboomer.resolutioncontrol.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Minecraft.class)
public interface MinecraftAccessor {
    @Accessor
    Framebuffer getFramebuffer();

    @Mutable
    @Accessor(value = "framebuffer")
    void setFramebuffer(Framebuffer framebuffer);
}
