package io.github.ultimateboomer.resolutioncontrol.client.gui.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import io.github.ultimateboomer.resolutioncontrol.ResolutionControlMod;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("StaticInitializerReferencesSubClass")
public class SettingsScreen extends Screen {
    protected static final ResourceLocation backgroundTexture = ResolutionControlMod.identifier("textures/gui/settings.png");

    protected static TranslationTextComponent text(String path, Object... args) {
        return new TranslationTextComponent(ResolutionControlMod.MOD_ID + "." + path, args);
    }

    protected static final int containerWidth = 192;
    protected static final int containerHeight = 128;

    protected static final Map<Class<? extends SettingsScreen>,
            Function<Screen, SettingsScreen>> screensSupplierList;

    static {
        screensSupplierList = new LinkedHashMap<>();
        screensSupplierList.put(MainSettingsScreen.class, MainSettingsScreen::new);
        screensSupplierList.put(ScreenshotSettingsScreen.class, ScreenshotSettingsScreen::new);
        screensSupplierList.put(InfoSettingsScreen.class, InfoSettingsScreen::new);
    }

    protected final ResolutionControlMod mod = ResolutionControlMod.getInstance();

    @Nullable
    protected final Screen parent;

    protected int centerX;
    protected int centerY;
    protected int startX;
    protected int startY;

    protected Map<Class<? extends SettingsScreen>, Button> menuButtons;

    protected Button doneButton;

    protected SettingsScreen(TranslationTextComponent title, @Nullable Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        centerX = width / 2;
        centerY = height / 2;
        startX = centerX - containerWidth / 2;
        startY = centerY - containerHeight / 2;

        // Init menu buttons
        menuButtons = new LinkedHashMap<>();
        final int menuButtonWidth = 80;
        final int menuButtonHeight = 20;
        MutableInt o = new MutableInt();

        screensSupplierList.forEach((c, constructor) -> {
            SettingsScreen r = constructor.apply(this.parent);
            Button b = new Button(
                    startX - menuButtonWidth - 20, startY + o.getValue(),
                    menuButtonWidth, menuButtonHeight,
                    r.getTitle(),
                    button -> {
                        if (minecraft != null) {
                            minecraft.displayGuiScreen(constructor.apply(this.parent));
                        }
                    }
            );

            if (this.getClass().equals(c))
                b.active = false;

            menuButtons.put(c, b);
            o.add(25);
        });

        menuButtons.values().forEach(this::addButton);

        doneButton = new Button(
                centerX + 15, startY + containerHeight - 30,
                60, 20,
                new TranslationTextComponent("gui.done"),
                button -> {
                    applySettingsAndCleanup();
                    if (minecraft != null) {
                        minecraft.displayGuiScreen(this.parent);
                    }
                }
        );
        addButton(doneButton);
    }

    @Override
    public void render(@Nonnull MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (minecraft != null && minecraft.world == null) {
            renderBackground(matrices, 0);
        }

        GlStateManager.enableAlphaTest();
        minecraft.getTextureManager().bindTexture(backgroundTexture);
        GlStateManager.color4f(1, 1, 1, 1);

        int textureWidth = 256;
        int textureHeight = 192;
        blit(
                matrices,
                centerX - textureWidth / 2, centerY - textureHeight / 2,
                0, 0,
                textureWidth, textureHeight
        );

        super.render(matrices, mouseX, mouseY, delta);

        drawLeftAlignedString(matrices, "\u00a7r" + getTitle().getString(),
                centerX + 15, startY + 10, 0x000000);

        drawRightAlignedString(matrices, text("settings.title").getString(),
                centerX + 5, startY + 10, 0x404040);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((ResolutionControlMod.getInstance().getSettingsKey().matchesKey(keyCode, scanCode))) {
            this.applySettingsAndCleanup();
            if (this.minecraft != null) {
                this.minecraft.displayGuiScreen(this.parent);
            }
            this.minecraft.mouseHelper.grabMouse();
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public void onClose() {
        this.applySettingsAndCleanup();
        super.onClose();
    }

    protected void applySettingsAndCleanup() {
        mod.saveSettings();
        mod.setLastSettingsScreen(this.getClass());
    };

    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    protected void drawCenteredString(MatrixStack matrices, String text, float x, float y, int color) {
        font.drawString(matrices, text, x - font.getStringWidth(text) / 2, y, color);
    }

    protected void drawLeftAlignedString(MatrixStack matrices, String text, float x, float y, int color) {
        font.drawString(matrices, text, x, y, color);
    }

    protected void drawRightAlignedString(MatrixStack matrices, String text, float x, float y, int color) {
        font.drawString(matrices, text, x - font.getStringWidth(text), y, color);
    }

    public static SettingsScreen getScreen(Class<? extends SettingsScreen> screenClass) {
        return screensSupplierList.get(screenClass).apply(null);
    }

    protected static ITextComponent getStateText(boolean enabled) {
        return enabled ? new TranslationTextComponent("addServer.resourcePack.enabled")
                : new TranslationTextComponent("addServer.resourcePack.disabled");
    }
}
