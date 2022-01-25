package io.github.ultimateboomer.resolutioncontrol.util;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.lwjgl.opengl.GL11;

public enum ScalingAlgorithm {
    NEAREST(new TranslationTextComponent("resolutioncontrol.settings.main.nearest"),
            GL11.GL_NEAREST, GL11.GL_NEAREST_MIPMAP_NEAREST),
    LINEAR(new TranslationTextComponent("resolutioncontrol.settings.main.linear"),
            GL11.GL_LINEAR, GL11.GL_LINEAR_MIPMAP_NEAREST);

    private final ITextComponent text;
    private final int id;
    private final int idMipped;

    ScalingAlgorithm(ITextComponent text, int id, int idMipped) {
        this.text = text;
        this.id = id;
        this.idMipped = idMipped;
    }

    public ITextComponent getText() {
        return text;
    }

    public int getId(boolean mipped) {
        return mipped ? idMipped : id;
    }
}
