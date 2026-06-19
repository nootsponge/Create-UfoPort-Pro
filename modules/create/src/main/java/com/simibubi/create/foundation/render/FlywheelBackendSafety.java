package com.simibubi.create.foundation.render;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simibubi.create.Create;

import dev.engine_room.flywheel.api.backend.Backend;
import dev.engine_room.flywheel.lib.util.ShadersModHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;

public final class FlywheelBackendSafety {
	private static final String ALLOW_IRISFLW_WITH_SHADERS_PROPERTY = "create_ufoport.flywheel.allowIrisFlwWithShaders";
	private static final String AUTO_IRISFLW_WITH_SHADERS_PROPERTY = "create_ufoport.flywheel.autoIrisFlwWithShaders";
	private static final String DISABLE_IRISFLW_WITH_SHADERS_PROPERTY = "create_ufoport.flywheel.disableIrisFlwWithShaders";
	private static final String DISABLE_COMPAT_CONFIG_PROPERTY = "create_ufoport.flywheel.disableCompatShaderConfig";
	private static final String DISABLE_CRASH_GUARD_PROPERTY = "create_ufoport.flywheel.disableIrisFlwCrashGuard";
	private static final String FORCE_IRISFLW_WITH_SHADERS_PROPERTY = "create_ufoport.flywheel.forceIrisFlwWithShaders";
	private static final String CRASH_GUARD_FILE = "irisflw-active-session.properties";
	private static final ResourceLocation IRIS_INSTANCING = ResourceLocation.fromNamespaceAndPath("irisflw", "iris_instancing");
	private static final ResourceLocation FLYWHEEL_INSTANCING = ResourceLocation.fromNamespaceAndPath("flywheel", "instancing");
	private static final ResourceLocation FLYWHEEL_INDIRECT = ResourceLocation.fromNamespaceAndPath("flywheel", "indirect");
	private static final ResourceLocation OFF = ResourceLocation.fromNamespaceAndPath("flywheel", "off");
	private static final long CURRENT_PID = ProcessHandle.current()
		.pid();
	private static Boolean compatConfigAllowsIrisFlw;
	private static boolean crashGuardOwned;
	private static boolean shutdownHookRegistered;
	private static boolean loggedCompatConfig;
	private static boolean loggedCrashGuard;
	private static boolean loggedCrashGuardClear;
	private static int irisFlwWorldTicks;

	private FlywheelBackendSafety() {
	}

	public static Backend sanitizeForActiveShaders(Backend selected, Backend offBackend) {
		Backend shaderSafeBackend = chooseForActiveShaders(offBackend);
		if (shaderSafeBackend == null)
			return selected;

		if (shaderSafeBackend == selected)
			return selected;

		if (isIrisBackend(selected) || isVanillaShaderUnsafe(selected))
			return shaderSafeBackend;

		return selected == null ? shaderSafeBackend : selected;
	}

	public static Backend chooseForActiveShaders(Backend offBackend) {
		if (!isShaderPackInUse())
			return null;

		Backend iris = Backend.REGISTRY.get(IRIS_INSTANCING);
		if (shouldUseIrisFlwWithShaders() && iris != null && isSupported(iris)) {
			markIrisFlwAttempt();
			return iris;
		}

		return offBackend;
	}

	public static boolean shouldManageActiveShaders() {
		return isShaderPackInUse();
	}

	public static boolean shouldForce() {
		return isShaderPackInUse() && shouldUseIrisFlwWithShaders();
	}

	public static boolean isForced(Backend backend) {
		return shouldUseIrisFlwWithShaders() && isIrisBackend(backend) && isSupported(backend);
	}

	public static boolean isOff(Backend backend) {
		return hasId(backend, OFF);
	}

	private static boolean isShaderPackInUse() {
		try {
			if (ShadersModHelper.isShaderPackInUse())
				return true;
		} catch (Throwable ignored) {
		}
		return isIrisShaderPackConfigured();
	}

	private static boolean isSupported(Backend backend) {
		try {
			return backend.isSupported();
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static boolean allowIrisFlwWithShaders() {
		return Boolean.getBoolean(ALLOW_IRISFLW_WITH_SHADERS_PROPERTY);
	}

	private static boolean autoIrisFlwWithShaders() {
		return Boolean.getBoolean(AUTO_IRISFLW_WITH_SHADERS_PROPERTY);
	}

	private static boolean forceIrisFlwWithShaders() {
		return Boolean.getBoolean(FORCE_IRISFLW_WITH_SHADERS_PROPERTY);
	}

	private static boolean shouldUseIrisFlwWithShaders() {
		if (Boolean.getBoolean(DISABLE_IRISFLW_WITH_SHADERS_PROPERTY))
			return false;
		if (forceIrisFlwWithShaders())
			return true;
		if (!allowIrisFlwWithShaders() && !autoIrisFlwWithShaders() && !compatConfigAllowsIrisFlwWithShaders())
			return false;
		if (hasUncleanIrisFlwAttempt())
			return false;
		return IrisFlwGlslVersionCompat.hasOpenGl33Context();
	}

	public static void markIrisFlwWorldTick(boolean inWorld) {
		if (!crashGuardOwned || !inWorld)
			return;
		if (++irisFlwWorldTicks < 200)
			return;
		clearIrisFlwAttempt();
	}

	private static boolean isIrisShaderPackConfigured() {
		if (isIrisShaderPackConfiguredByApi())
			return true;
		return isIrisShaderPackConfiguredByConfigFile();
	}

	private static boolean isIrisShaderPackConfiguredByApi() {
		try {
			Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
			Object quick = iris.getMethod("isPackInUseQuick")
				.invoke(null);
			if (Boolean.TRUE.equals(quick))
				return true;
		} catch (Throwable ignored) {
		}

		try {
			Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			Object instance = api.getMethod("getInstance")
				.invoke(null);
			if (instance == null)
				return false;
			Object config = api.getMethod("getConfig")
				.invoke(instance);
			if (config == null)
				return false;
			Object enabled = Class.forName("net.irisshaders.iris.api.v0.IrisApiConfig")
				.getMethod("areShadersEnabled")
				.invoke(config);
			return Boolean.TRUE.equals(enabled);
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static boolean isIrisShaderPackConfiguredByConfigFile() {
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

	private static boolean compatConfigAllowsIrisFlwWithShaders() {
		if (Boolean.getBoolean(DISABLE_COMPAT_CONFIG_PROPERTY))
			return false;
		if (compatConfigAllowsIrisFlw != null)
			return compatConfigAllowsIrisFlw;

		compatConfigAllowsIrisFlw = false;
		try {
			Path config = FabricLoader.getInstance()
				.getConfigDir()
				.resolve("create_flywheel_compat.json");
			if (!Files.isRegularFile(config))
				return false;

			JsonObject root = JsonParser.parseString(Files.readString(config))
				.getAsJsonObject();
			compatConfigAllowsIrisFlw = root.has("allowBackendWithShaders")
				&& root.get("allowBackendWithShaders")
					.getAsBoolean();

			if (compatConfigAllowsIrisFlw && !loggedCompatConfig) {
				Create.LOGGER.warn("Create-UfoPort: create_flywheel_compat.json allows shader backends; active shader sessions may use irisflw:iris_instancing when OpenGL 3.3 is available.");
				loggedCompatConfig = true;
			}
		} catch (Throwable t) {
			Create.LOGGER.warn("Create-UfoPort: unable to read create_flywheel_compat.json for shader backend selection; keeping Flywheel 1.x shader backend opt-in only.", t);
		}
		return compatConfigAllowsIrisFlw;
	}

	private static boolean hasUncleanIrisFlwAttempt() {
		if (Boolean.getBoolean(DISABLE_CRASH_GUARD_PROPERTY) || crashGuardOwned)
			return false;

		Path marker = crashGuardPath();
		if (!Files.isRegularFile(marker))
			return false;

		try (InputStream input = Files.newInputStream(marker)) {
			Properties properties = new Properties();
			properties.load(input);
			long pid = Long.parseLong(properties.getProperty("pid", "-1"));
			if (pid == CURRENT_PID)
				return false;
		} catch (Throwable ignored) {
		}

		if (!loggedCrashGuard) {
			Create.LOGGER.warn("Create-UfoPort: previous IrisFLW shader backend session did not shut down cleanly; keeping Flywheel 1.x off under shaders for this launch. Delete {} or use -D{}=true to retry immediately.", marker, FORCE_IRISFLW_WITH_SHADERS_PROPERTY);
			loggedCrashGuard = true;
		}
		return true;
	}

	private static void markIrisFlwAttempt() {
		if (Boolean.getBoolean(DISABLE_CRASH_GUARD_PROPERTY) || crashGuardOwned)
			return;
		try {
			Path marker = crashGuardPath();
			Files.createDirectories(marker.getParent());
			Properties properties = new Properties();
			properties.setProperty("pid", Long.toString(CURRENT_PID));
			properties.setProperty("startedAt", Long.toString(System.currentTimeMillis()));
			try (var output = Files.newOutputStream(marker)) {
				properties.store(output, "Create-UfoPort IrisFLW crash guard");
			}
			crashGuardOwned = true;
			registerShutdownHook();
		} catch (Throwable t) {
			Create.LOGGER.warn("Create-UfoPort: unable to arm IrisFLW crash guard; continuing with shader backend selection.", t);
		}
	}

	private static void clearIrisFlwAttempt() {
		if (!crashGuardOwned)
			return;
		try {
			Files.deleteIfExists(crashGuardPath());
			if (!loggedCrashGuardClear) {
				Create.LOGGER.warn("Create-UfoPort: IrisFLW shader backend survived world startup; crash guard marker cleared.");
				loggedCrashGuardClear = true;
			}
		} catch (Throwable ignored) {
		}
		crashGuardOwned = false;
	}

	private static void registerShutdownHook() {
		if (shutdownHookRegistered)
			return;
		shutdownHookRegistered = true;
		Runtime.getRuntime()
			.addShutdownHook(new Thread(FlywheelBackendSafety::clearIrisFlwAttempt, "Create-UfoPort IrisFLW crash guard cleanup"));
	}

	private static Path crashGuardPath() {
		return FabricLoader.getInstance()
			.getConfigDir()
			.resolve("create-ufoport-debug")
			.resolve(CRASH_GUARD_FILE);
	}

	private static boolean isIrisBackend(Backend backend) {
		return hasId(backend, IRIS_INSTANCING);
	}

	private static boolean isVanillaShaderUnsafe(Backend backend) {
		return hasId(backend, FLYWHEEL_INDIRECT) || hasId(backend, FLYWHEEL_INSTANCING);
	}

	private static boolean hasId(Backend backend, ResourceLocation id) {
		if (backend == null)
			return false;
		ResourceLocation actual = Backend.REGISTRY.getId(backend);
		return id.equals(actual);
	}
}
