package io.github.ultimateboomer.resolutioncontrol.mixin;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

public class RSCConnector implements IMixinConnector {
    @Override
    public void connect() {
        Mixins.addConfiguration("resolutioncontrol.mixins.json");
    }

}
