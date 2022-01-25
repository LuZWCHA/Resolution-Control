package io.github.ultimateboomer.resolutioncontrol;

import io.github.ultimateboomer.resolutioncontrol.client.gui.screen.MainSettingsScreen;
import io.github.ultimateboomer.resolutioncontrol.client.gui.screen.SettingsScreen;
import io.github.ultimateboomer.resolutioncontrol.mixin.MainWindowAccessor;
import io.github.ultimateboomer.resolutioncontrol.mixin.MinecraftAccessor;
import io.github.ultimateboomer.resolutioncontrol.util.*;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.*;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Mod("resolutioncontrol")
public class ResolutionControlMod{
	public static final String MOD_ID = "resolutioncontrol";
	public static final String MOD_NAME = "ResolutionControl+";

	public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

	public static ResourceLocation identifier(String path) {
		return new ResourceLocation(MOD_ID, path);
	}
	
	private final Minecraft client;
	
	private static ResolutionControlMod instance;
	
	public static ResolutionControlMod getInstance() {
		return instance;
	}

	private static final String SCREENSHOT_PREFIX = "fb";

	private boolean optifineInstalled;
	
	private KeyBinding settingsKey;
	private KeyBinding screenshotKey;
	
	private boolean shouldScale = false;
	
	@Nullable
	private Framebuffer framebuffer;

	@Nullable
	private Framebuffer screenshotFrameBuffer;
	
	@Nullable
	private Framebuffer clientFramebuffer;

	private Set<Framebuffer> minecraftFramebuffers;

	private Class<? extends SettingsScreen> lastSettingsScreen = MainSettingsScreen.class;

	private int currentWidth;
	private int currentHeight;

	private long estimatedMemory;

	private boolean screenshot = false;

	private int lastWidth;
	private int lastHeight;


	public ResolutionControlMod(){
		client = Minecraft.getInstance();

		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

		MinecraftForge.EVENT_BUS.register(this);

		onResolutionChanged();
	}

	private void setup(final FMLCommonSetupEvent event)
	{
		// some preinit code
	}

	private void doClientStuff(final FMLClientSetupEvent event) {
		onInitialize();
	}

	private void enqueueIMC(final InterModEnqueueEvent event)
	{
	}

	private void processIMC(final InterModProcessEvent event){
	}

	@SubscribeEvent
	public void serverLoaded(FMLServerStartedEvent event){
		if (ConfigHandler.instance.getConfig().enableDynamicResolution) {
			DynamicResolutionHandler.INSTANCE.reset();
		}
	}

	@SubscribeEvent
	public void modLoaded(FMLLoadCompleteEvent event){
		onResolutionChanged();
		Object obj = Minecraft.getInstance().getMainWindow();
		((MainWindowAccessor)(obj)).callUpdateFramebufferSize();
	}

	public void onInitialize() {
		instance = this;

		settingsKey = new KeyBinding(
				"key.resolutioncontrol.settings",
				GLFW.GLFW_KEY_O,
				"key.categories.resolutioncontrol");
		ClientRegistry.registerKeyBinding(settingsKey);

		screenshotKey = new KeyBinding(
				"key.resolutioncontrol.screenshot",
				-1,
				"key.categories.resolutioncontrol");
		ClientRegistry.registerKeyBinding(screenshotKey);

		// TODO: 2022/1/24
		optifineInstalled = false;

		//Forge mod will be init after framebuffer init, do init again after the mod is loaded.
//		resizeMinecraftFramebuffers();
//		onResolutionChanged();
	}

	public static boolean isInit(){
		return instance != null;
	}

	@SubscribeEvent
	public void onClientEvent(TickEvent.ClientTickEvent event){
		if(event.phase.equals(TickEvent.Phase.END)){
			while (settingsKey.isPressed()) {
				client.displayGuiScreen(SettingsScreen.getScreen(lastSettingsScreen));
			}

			while (screenshotKey.isPressed()) {
				if (getOverrideScreenshotScale()) {
					this.screenshot = true;
					client.player.sendStatusMessage(
							new TranslationTextComponent("resolutioncontrol.screenshot.wait"), false);
				} else {
					saveScreenshot(framebuffer);
				}
			}

			if (ConfigHandler.instance.getConfig().enableDynamicResolution && client.world != null
					&& getWindow().getWindowX() != -32000) {
				DynamicResolutionHandler.INSTANCE.tick();
			}
		}
	}

	private void saveScreenshot(Framebuffer fb) {
		ScreenShotHelper.saveScreenshot(client.gameDir,
				RCUtil.getScreenshotFilename(client.gameDir).toString(),
				fb.framebufferTextureWidth, fb.framebufferTextureHeight, fb,
				text -> client.player.sendStatusMessage(text, false));
	}
	
	public void setShouldScale(boolean shouldScale) {
		if (shouldScale == this.shouldScale) return;

		if (getScaleFactor() == 1) return;
		
		MainWindow window = getWindow();
		if (framebuffer == null) {
			this.shouldScale = true; // so we get the right dimensions
			framebuffer = new Framebuffer(
					window.getFramebufferWidth(),
					window.getFramebufferHeight(),
					true,
					Minecraft.IS_RUNNING_ON_MAC
			);
			calculateSize();
		}

		this.shouldScale = shouldScale;

		client.getProfiler().endStartSection(shouldScale ? "startScaling" : "finishScaling");

		// swap out framebuffers as needed
		if (shouldScale) {
			clientFramebuffer = client.getFramebuffer();

			if (screenshot) {
				resizeMinecraftFramebuffers();

				if (!isScreenshotFramebufferAlwaysAllocated() && screenshotFrameBuffer != null) {
					screenshotFrameBuffer.deleteFramebuffer();
				}

				if (screenshotFrameBuffer == null) {
					initScreenshotFramebuffer();
				}

				setClientFramebuffer(screenshotFrameBuffer);

				screenshotFrameBuffer.bindFramebuffer(true);
			} else {
				setClientFramebuffer(framebuffer);

				framebuffer.bindFramebuffer(true);
			}
			// nothing on the client's framebuffer yet
		} else {
			setClientFramebuffer(clientFramebuffer);
			client.getFramebuffer().bindFramebuffer(true);

			// Screenshot framebuffer
			if (screenshot) {
				saveScreenshot(screenshotFrameBuffer);

				if (!isScreenshotFramebufferAlwaysAllocated()) {
					screenshotFrameBuffer.deleteFramebuffer();
					screenshotFrameBuffer = null;
				}

				screenshot = false;
				resizeMinecraftFramebuffers();
			} else {
				framebuffer.framebufferRender(
						window.getFramebufferWidth(),
						window.getFramebufferHeight()
				);
			}
		}

		client.getProfiler().endStartSection("level");
	}

	public void initMinecraftFramebuffers() {
		if (minecraftFramebuffers != null) {
			minecraftFramebuffers.clear();
		} else {
			minecraftFramebuffers = new HashSet<>();
		}

		minecraftFramebuffers.add(client.worldRenderer.getEntityOutlineFramebuffer());
		minecraftFramebuffers.add(client.worldRenderer.getTranslucentFrameBuffer());
		minecraftFramebuffers.add(client.worldRenderer.getItemEntityFrameBuffer());
		minecraftFramebuffers.add(client.worldRenderer.getParticleFrameBuffer());
		minecraftFramebuffers.add(client.worldRenderer.getWeatherFrameBuffer());
		minecraftFramebuffers.add(client.worldRenderer.getCloudFrameBuffer());
		minecraftFramebuffers.remove(null);
	}

	public Framebuffer getFramebuffer() {
		return framebuffer;
	}

	public void initScreenshotFramebuffer() {
		if (Objects.nonNull(screenshotFrameBuffer)) screenshotFrameBuffer.deleteFramebuffer();

		screenshotFrameBuffer = new Framebuffer(
				getScreenshotWidth(), getScreenshotHeight(),
				true, Minecraft.IS_RUNNING_ON_MAC);
	}
	
	public float getScaleFactor() {
		return Config.getInstance().scaleFactor;
	}
	
	public void setScaleFactor(float scaleFactor) {
		Config.getInstance().scaleFactor = scaleFactor;
		
		updateFramebufferSize();
		
		ConfigHandler.instance.saveConfig();
	}

	public ScalingAlgorithm getUpscaleAlgorithm() {
		return Config.getInstance().upscaleAlgorithm;
	}

	public void setUpscaleAlgorithm(ScalingAlgorithm algorithm) {
		if (algorithm == Config.getInstance().upscaleAlgorithm) return;

		Config.getInstance().upscaleAlgorithm = algorithm;

		onResolutionChanged();

		ConfigHandler.instance.saveConfig();
	}

	public void nextUpscaleAlgorithm() {
		ScalingAlgorithm currentAlgorithm = getUpscaleAlgorithm();
		if (currentAlgorithm.equals(ScalingAlgorithm.NEAREST)) {
			setUpscaleAlgorithm(ScalingAlgorithm.LINEAR);
		} else {
			setUpscaleAlgorithm(ScalingAlgorithm.NEAREST);
		}
	}

	public ScalingAlgorithm getDownscaleAlgorithm() {
		return Config.getInstance().downscaleAlgorithm;
	}

	public void setDownscaleAlgorithm(ScalingAlgorithm algorithm) {
		if (algorithm == Config.getInstance().downscaleAlgorithm) return;

		Config.getInstance().downscaleAlgorithm = algorithm;

		onResolutionChanged();

		ConfigHandler.instance.saveConfig();
	}

	public void nextDownscaleAlgorithm() {
		ScalingAlgorithm currentAlgorithm = getDownscaleAlgorithm();
		if (currentAlgorithm.equals(ScalingAlgorithm.NEAREST)) {
			setDownscaleAlgorithm(ScalingAlgorithm.LINEAR);
		} else {
			setDownscaleAlgorithm(ScalingAlgorithm.NEAREST);
		}
	}
	
	public double getCurrentScaleFactor() {
		return shouldScale ?
				Config.getInstance().enableDynamicResolution ?
						DynamicResolutionHandler.INSTANCE.getCurrentScale() : Config.getInstance().scaleFactor : 1;
	}

	public boolean getOverrideScreenshotScale() {
		return Config.getInstance().overrideScreenshotScale;
	}

	public void setOverrideScreenshotScale(boolean value) {
		Config.getInstance().overrideScreenshotScale = value;
		if (value && isScreenshotFramebufferAlwaysAllocated()) {
			initScreenshotFramebuffer();
		} else {
			if (screenshotFrameBuffer != null) {
				screenshotFrameBuffer.deleteFramebuffer();
				screenshotFrameBuffer = null;
			}
		}
	}

	public int getScreenshotWidth() {
		return Math.max(Config.getInstance().screenshotWidth, 1);
	}

	public void setScreenshotWidth(int width) {
		Config.getInstance().screenshotWidth = width;
	}

	public int getScreenshotHeight() {
		return Math.max(Config.getInstance().screenshotHeight, 1);
	}

	public void setScreenshotHeight(int height) {
		Config.getInstance().screenshotHeight = height;
	}

	public boolean isScreenshotFramebufferAlwaysAllocated() {
		return Config.getInstance().screenshotFramebufferAlwaysAllocated;
	}

	public void setScreenshotFramebufferAlwaysAllocated(boolean value) {
		Config.getInstance().screenshotFramebufferAlwaysAllocated = value;

		if (value) {
			if (getOverrideScreenshotScale() && Objects.isNull(this.screenshotFrameBuffer)) {
				initScreenshotFramebuffer();
			}
		} else {
			if (this.screenshotFrameBuffer != null) {
				this.screenshotFrameBuffer.deleteFramebuffer();
				this.screenshotFrameBuffer = null;
			}
		}
	}

	public void setEnableDynamicResolution(boolean enableDynamicResolution) {
		Config.getInstance().enableDynamicResolution = enableDynamicResolution;
	}

	public void onResolutionChanged() {
		if (getWindow() == null)
			return;

		LOGGER.info("Size changed to {}x{} {}x{} {}x{}",
				getWindow().getFramebufferWidth(), getWindow().getFramebufferHeight(),
				getWindow().getWidth(), getWindow().getHeight(),
				getWindow().getScaledWidth(), getWindow().getScaledHeight());

//		if (getWindow().getScaledHeight() == lastWidth
//				|| getWindow().getScaledHeight() == lastHeight)
//		{
			updateFramebufferSize();

			lastWidth = getWindow().getScaledHeight();
			lastHeight = getWindow().getScaledHeight();
//		}


	}
	
	public void updateFramebufferSize() {
		if (framebuffer == null)
			return;

		resize(framebuffer);
		resize(client.worldRenderer.getEntityOutlineFramebuffer());
//		resizeMinecraftFramebuffers();

		calculateSize();
	}

	public void resizeMinecraftFramebuffers() {
		initMinecraftFramebuffers();
		minecraftFramebuffers.forEach(this::resize);
	}

	public void calculateSize() {
		currentWidth = framebuffer.framebufferTextureWidth;
		currentHeight = framebuffer.framebufferTextureHeight;

		// Framebuffer uses color (4 x 8 = 32 bit int) and depth (32 bit float)
		estimatedMemory = (long) currentWidth * currentHeight * 8;
	}
	
	public void resize(@Nullable Framebuffer framebuffer) {
		if (framebuffer == null) return;

		boolean prev = shouldScale;
		shouldScale = true;
		if (screenshot) {
			framebuffer.resize(
					getScreenshotWidth(),
					getScreenshotHeight(),
					Minecraft.IS_RUNNING_ON_MAC
			);
		} else {
			framebuffer.resize(
					getWindow().getFramebufferWidth(),
					getWindow().getFramebufferHeight(),
					Minecraft.IS_RUNNING_ON_MAC
			);
		}
		shouldScale = prev;
	}
	
	private MainWindow getWindow() {
		return client.getMainWindow();
	}

	private void setClientFramebuffer(Framebuffer framebuffer) {
		((MinecraftAccessor)client).setFramebuffer(framebuffer);
	}

	public KeyBinding getSettingsKey() {
		return settingsKey;
	}

	public int getCurrentWidth() {
		return currentWidth;
	}

	public int getCurrentHeight() {
		return currentHeight;
	}

	public long getEstimatedMemory() {
		return estimatedMemory;
	}

	public boolean isScreenshotting() {
		return screenshot;
	}

	public boolean isOptifineInstalled() {
		return optifineInstalled;
	}

	public void saveSettings() {
		ConfigHandler.instance.saveConfig();
	}

	public void setLastSettingsScreen(Class<? extends SettingsScreen> ordinal) {
		this.lastSettingsScreen = ordinal;
	}

}
