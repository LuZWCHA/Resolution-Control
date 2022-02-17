package io.github.ultimateboomer.resolutioncontrol.mixin;

import net.minecraft.client.MainWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = MainWindow.class)
public interface MainWindowAccessor {
    @Invoker
    void callUpdateFramebufferSize();
}
