package com.simibubi.create.foundation.render;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simibubi.create.Create;

import net.fabricmc.loader.api.FabricLoader;

public final class IrisFlwPerformanceTuning {
	private static final String SKIP_SHADOW_PASS_PROPERTY = "create_ufoport.flywheel.skipIrisFlwShadowPass";
	private static final String DISABLE_SHADOW_PASS_SKIP_PROPERTY = "create_ufoport.flywheel.disableIrisFlwShadowPassSkip";
	private static final String DISABLE_COMPAT_CONFIG_PROPERTY = "create_ufoport.flywheel.disableCompatShaderConfig";
	private static final String SKIP_SHADOW_PASS_CONFIG_KEY = "skipIrisFlwShadowPass";
	private static final String DISABLE_SHADOW_PASS_SKIP_CONFIG_KEY = "disableIrisFlwShadowPassSkip";
	private static final String RENDER_LAYER_EVENT_STATE_MANAGER = "top.leonx.irisflw.flywheel.RenderLayerEventStateManager";
	private static Boolean compatConfigSkipsShadowPass;
	private static Boolean compatConfigDisablesShadowPassSkip;
	private static Method isRenderingShadowMethod;
	private static boolean loggedCompatConfig;
	private static boolean loggedDefaultSkip;
	private static boolean loggedShadowSkip;
	private static boolean loggedReflectionFailure;

	private IrisFlwPerformanceTuning() {
	}

	public static boolean shouldSkipIrisFlwShadowPass() {
		if (Boolean.getBoolean(DISABLE_SHADOW_PASS_SKIP_PROPERTY))
			return false;
		if (compatConfigDisablesIrisFlwShadowPassSkip())
			return false;
		if (Boolean.getBoolean(SKIP_SHADOW_PASS_PROPERTY))
			return true;
		if (compatConfigSkipsIrisFlwShadowPass())
			return true;
		if (!loggedDefaultSkip) {
			Create.LOGGER.warn("Create-UfoPort: enabling IrisFLW shadow-pass skip by default for Flywheel instanced visuals under shaders.");
			loggedDefaultSkip = true;
		}
		return true;
	}

	public static boolean isRenderingIrisFlwShadowPass() {
		try {
			Method method = isRenderingShadowMethod;
			if (method == null) {
				method = Class.forName(RENDER_LAYER_EVENT_STATE_MANAGER)
					.getMethod("isRenderingShadow");
				isRenderingShadowMethod = method;
			}
			return Boolean.TRUE.equals(method.invoke(null));
		} catch (Throwable t) {
			if (!loggedReflectionFailure) {
				Create.LOGGER.warn("Create-UfoPort: unable to read IrisFLW shadow render state; keeping IrisFLW shadow pass unchanged.", t);
				loggedReflectionFailure = true;
			}
			return false;
		}
	}

	public static void logIrisFlwShadowPassSkipped() {
		if (loggedShadowSkip)
			return;
		Create.LOGGER.warn("Create-UfoPort: skipping IrisFLW Flywheel instanced shadow pass to reduce shader GPU spikes. Disable with -D{}=true or set {} to true in create_flywheel_compat.json.",
			DISABLE_SHADOW_PASS_SKIP_PROPERTY, DISABLE_SHADOW_PASS_SKIP_CONFIG_KEY);
		loggedShadowSkip = true;
	}

	private static boolean compatConfigDisablesIrisFlwShadowPassSkip() {
		if (Boolean.getBoolean(DISABLE_COMPAT_CONFIG_PROPERTY))
			return false;
		if (compatConfigDisablesShadowPassSkip != null)
			return compatConfigDisablesShadowPassSkip;

		compatConfigDisablesShadowPassSkip = readCompatConfigBoolean(DISABLE_SHADOW_PASS_SKIP_CONFIG_KEY, false);
		return compatConfigDisablesShadowPassSkip;
	}

	private static boolean compatConfigSkipsIrisFlwShadowPass() {
		if (Boolean.getBoolean(DISABLE_COMPAT_CONFIG_PROPERTY))
			return false;
		if (compatConfigSkipsShadowPass != null)
			return compatConfigSkipsShadowPass;

		compatConfigSkipsShadowPass = readCompatConfigBoolean(SKIP_SHADOW_PASS_CONFIG_KEY, false);

		if (compatConfigSkipsShadowPass && !loggedCompatConfig) {
			Create.LOGGER.warn("Create-UfoPort: create_flywheel_compat.json enables IrisFLW shadow-pass skip for Flywheel instanced visuals.");
			loggedCompatConfig = true;
		}
		return compatConfigSkipsShadowPass;
	}

	private static boolean readCompatConfigBoolean(String key, boolean fallback) {
		try {
			Path config = FabricLoader.getInstance()
				.getConfigDir()
				.resolve("create_flywheel_compat.json");
			if (!Files.isRegularFile(config))
				return fallback;

			JsonObject root = JsonParser.parseString(Files.readString(config))
				.getAsJsonObject();
			if (!root.has(key))
				return fallback;

			return root.get(key)
				.getAsBoolean();
		} catch (Throwable t) {
			Create.LOGGER.warn("Create-UfoPort: unable to read create_flywheel_compat.json for IrisFLW shadow-pass tuning; using default shadow-pass skip behavior.", t);
			return fallback;
		}
	}
}
