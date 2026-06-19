package com.jozufozu.flywheel.backend;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.jozufozu.flywheel.api.FlywheelWorld;
import com.jozufozu.flywheel.backend.gl.versioned.GlCompat;
import com.jozufozu.flywheel.backend.instancing.ParallelTaskEngine;
import com.jozufozu.flywheel.config.BackendType;
import com.jozufozu.flywheel.config.FlwConfig;
import com.jozufozu.flywheel.core.shader.ProgramSpec;
import com.mojang.logging.LogUtils;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class Backend {
	public static final Logger LOGGER = LogUtils.getLogger();

	private static BackendType backendType = BackendType.OFF;

	private static boolean loggedShaderOff;
	private static boolean loggedShaderBatching;

	private static ParallelTaskEngine taskEngine;

	private static final Loader loader = new Loader();

	/**
	 * Get the current Flywheel backend type.
	 */
	public static BackendType getBackendType() {
		return backendType;
	}

	/**
	 * Get a thread pool for running Flywheel related work in parallel.
	 * @return A global Flywheel thread pool.
	 */
	public static ParallelTaskEngine getTaskEngine() {
		if (taskEngine == null) {
			taskEngine = new ParallelTaskEngine("Flywheel");
			taskEngine.startWorkers();
		}

		return taskEngine;
	}

	/**
	 * Get a string describing the Flywheel backend. When there are eventually multiple backends
	 * (Meshlet, MDI, GL31 Draw Instanced are planned), this will name which one is in use.
	 */
	public static String getBackendDescriptor() {
		return backendType == null ? "Uninitialized" : backendType.getProperName();
	}

	@Nullable
	public static ProgramSpec getSpec(ResourceLocation name) {
		return loader.get(name);
	}

	public static void refresh() {
		BackendType previousType = backendType;
		backendType = chooseEngine();

		if (previousType != backendType) {
			LOGGER.info("Flywheel backend: {}", getBackendDescriptor());
		}
	}

	public static boolean isOn() {
		return backendType != BackendType.OFF;
	}

	public static boolean canUseInstancing(@Nullable Level world) {
		return isOn() && isFlywheelWorld(world);
	}

	/**
	 * Used to avoid calling Flywheel functions on (fake) worlds that don't specifically support it.
	 */
	public static boolean isFlywheelWorld(@Nullable LevelAccessor world) {
		if (world == null) return false;

		if (!world.isClientSide()) return false;

		if (world instanceof FlywheelWorld && ((FlywheelWorld) world).supportsFlywheel()) return true;

		return world == Minecraft.getInstance().level;
	}

	public static boolean isGameActive() {
		return !(Minecraft.getInstance().level == null || Minecraft.getInstance().player == null);
	}

	public static void reloadWorldRenderers() {
		RenderWork.enqueue(Minecraft.getInstance().levelRenderer::allChanged);
	}

	private static BackendType chooseEngine() {
		BackendType preferredChoice = FlwConfig.get()
				.getBackendType();

		boolean usingShaders = isShaderPackInUse();
		boolean allowWithShaders = FlwConfig.get()
				.allowBackendWithShaders() || forceBackendWithShaders();

		if (usingShaders && preferredChoice == BackendType.INSTANCING && allowWithShaders) {
			logShaderBatching();
			return BackendType.BATCHING;
		}

		if (usingShaders && !allowWithShaders) {
			logShaderOff();
			return BackendType.OFF;
		}

		boolean shadersAllowed = !usingShaders || allowWithShaders;
		boolean canUseEngine = switch (preferredChoice) {
			case OFF -> true;
			case BATCHING -> shadersAllowed;
			case INSTANCING -> !usingShaders && instancingSupported();
		};

		return canUseEngine ? preferredChoice : BackendType.OFF;
	}

	public static void init() {
		// noop
	}

	private static boolean forceBackendWithShaders() {
		return Boolean.getBoolean("flywheel.forceBackendWithShaders")
				|| Boolean.getBoolean("create_ufoport.flywheel.forceBackendWithShaders");
	}

	private static boolean isShaderPackInUse() {
		try {
			if (ShadersModHandler.isShaderPackInUse())
				return true;
		} catch (Throwable ignored) {
		}
		return isIrisShaderPackConfigured();
	}

	private static boolean isIrisShaderPackConfigured() {
		try {
			Path config = FabricLoader.getInstance()
					.getConfigDir()
					.resolve("iris.properties");
			if (!Files.isRegularFile(config))
				return false;

			Properties properties = new Properties();
			try (InputStream input = Files.newInputStream(config)) {
				properties.load(input);
			}

			if (!Boolean.parseBoolean(properties.getProperty("enableShaders", "false")))
				return false;

			String shaderPack = properties.getProperty("shaderPack", "")
					.trim();
			return !shaderPack.isEmpty() && !"OFF".equalsIgnoreCase(shaderPack) && !"null".equalsIgnoreCase(shaderPack);
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static void logShaderOff() {
		if (loggedShaderOff)
			return;
		LOGGER.warn("Create-UfoPort: active Iris shaderpack detected before legacy Flywheel backend selection; forcing legacy backend to off.");
		loggedShaderOff = true;
	}

	private static void logShaderBatching() {
		if (loggedShaderBatching)
			return;
		LOGGER.warn("Create-UfoPort: active Iris shaderpack detected; using legacy Flywheel batching instead of GL instancing.");
		loggedShaderBatching = true;
	}

	private static boolean instancingSupported() {
		try {
			return GlCompat.getInstance()
					.instancedArraysSupported();
		} catch (Throwable t) {
			LOGGER.warn("Flywheel instancing unavailable while probing OpenGL capabilities.", t);
			return false;
		}
	}

	private Backend() {
		throw new UnsupportedOperationException("Backend is a static class!");
	}
}
