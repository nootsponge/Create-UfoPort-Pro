package com.simibubi.create.foundation.render;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.simibubi.create.Create;

import net.fabricmc.loader.api.FabricLoader;

public final class IrisFlwGlslVersionCompat {
	private static final String DISABLE_PATCH_PROPERTY = "create_ufoport.flywheel.disableIrisFlwGlslVersionPatch";
	private static final String FORCE_GLSL330_PROPERTY = "create_ufoport.flywheel.forceIrisFlwGlsl330";
	private static final String DUMP_LIMIT_PROPERTY = "create_ufoport.flywheel.irisFlwShaderDumpLimit";
	private static final Pattern VERSION_PATTERN =
		Pattern.compile("(?m)^\\s*#version\\s+(\\d+)(?:\\s+\\w+)?\\s*$");
	private static final Pattern GL_VERSION_STRING_PATTERN =
		Pattern.compile("(\\d+)\\.(\\d+)");
	private static final Pattern EXPLICIT_VERTEX_INPUT_PATTERN =
		Pattern.compile("\\blayout\\s*\\(\\s*location\\s*=\\s*\\d+\\s*\\)\\s*in\\b");
	private static final Pattern FLW_UNIFORM_INITIALIZER_PATTERN =
		Pattern.compile("\\buniform\\s+([^;=]*\\b_flw_[A-Za-z0-9_]*\\b[^;=]*?)\\s*=\\s*[^;]+;");
	private static final Pattern FLW_VERTEX_ID_BASE_VERTEX_PATTERN =
		Pattern.compile("\\bflw_vertexId\\s*=\\s*gl_VertexID\\s*-\\s*baseVertex\\s*;");
	private static final Pattern FLW_UINT_MASK_BOOLEAN_PATTERN =
		Pattern.compile("\\((p\\s*&\\s*_FLW_[A-Z0-9_]+_MASK)\\)\\s*!=\\s*0\\s*;");
	private static final Pattern LEGACY_ATTRIBUTE_PATTERN =
		Pattern.compile("\\battribute\\b");
	private static final Pattern LEGACY_VARYING_PATTERN =
		Pattern.compile("\\bvarying\\b");
	private static boolean loggedRewrite;
	private static boolean loggedNativePath;
	private static boolean loggedContextOverride;
	private static boolean loggedFailure;
	private static boolean loggedRequestedContext;
	private static boolean loggedConfirmedContext;
	private static boolean loggedVertexIdRewrite;
	private static boolean loggedUintMaskRewrite;
	private static int dumpedShaders;
	private static int requestedContextMaxVersion;
	private static int confirmedContextMaxVersion;

	private IrisFlwGlslVersionCompat() {
	}

	public static boolean shouldPatch() {
		return !Boolean.getBoolean(DISABLE_PATCH_PROPERTY) && !Boolean.getBoolean(FORCE_GLSL330_PROPERTY);
	}

	public static void recordRequestedOpenGl33Context() {
		requestedContextMaxVersion = Math.max(requestedContextMaxVersion, 330);
		if (!loggedRequestedContext) {
			Create.LOGGER.warn("Create-UfoPort: recorded requested OpenGL 3.3 context as GLSL 330-capable for IrisFLW shader selection.");
			loggedRequestedContext = true;
		}
	}

	public static void recordCurrentOpenGlContext(String source) {
		int liveVersion = readLiveContextMaxVersion();
		if (liveVersion < 330)
			return;
		confirmedContextMaxVersion = Math.max(confirmedContextMaxVersion, liveVersion);
		if (!loggedConfirmedContext) {
			Create.LOGGER.warn("Create-UfoPort: confirmed OpenGL context from {} supports GLSL {}; caching it for IrisFLW shader selection.", source, liveVersion);
			loggedConfirmedContext = true;
		}
	}

	public static String restoreFlywheelMaxVersion(String stage, String irisSource, String patchedSource) {
		if (!shouldPatch() || irisSource == null || patchedSource == null)
			return patchedSource;

		String compatibleSource = fixStrictGlslTypeMismatches(stage, patchedSource);
		int targetVersion = targetVersion(irisSource, compatibleSource);
		if (targetVersion >= 330) {
			if (!loggedNativePath) {
				Create.LOGGER.warn("Create-UfoPort: keeping IrisFLW {} shader at GLSL {} because the current OpenGL context can run the native shaderpack instancing path.", stage, targetVersion);
				loggedNativePath = true;
			}
			dumpShader(stage, targetVersion, irisSource, patchedSource, compatibleSource);
			return compatibleSource;
		}

		String rewritten = replaceVersion(compatibleSource, targetVersion);
		rewritten = normalizeLegacyIoQualifiers(stage, rewritten);
		rewritten = preserveOrRemoveExplicitVertexInputLocations(rewritten);
		rewritten = removeUniformInitializers(rewritten);
		rewritten = ensureExtension(rewritten, "GL_ARB_shader_bit_encoding");

		if (!loggedRewrite && !rewritten.equals(patchedSource)) {
			Create.LOGGER.warn("Create-UfoPort: rewriting IrisFLW {} shader from GLSL 330 to GLSL {} for Flywheel shaderpack instancing compatibility.", stage, targetVersion);
			loggedRewrite = true;
		}

		dumpShader(stage, targetVersion, irisSource, patchedSource, rewritten);
		return rewritten;
	}

	private static String fixStrictGlslTypeMismatches(String stage, String source) {
		if (!"vertex".equals(stage))
			return source;

		String rewritten = source;
		if (rewritten.indexOf("gl_VertexID") >= 0 && rewritten.indexOf("baseVertex") >= 0) {
			String vertexIdFixed = FLW_VERTEX_ID_BASE_VERTEX_PATTERN.matcher(rewritten)
				.replaceAll("flw_vertexId = uint(gl_VertexID) - baseVertex;");
			if (!vertexIdFixed.equals(rewritten) && !loggedVertexIdRewrite) {
				Create.LOGGER.warn("Create-UfoPort: patched IrisFLW vertex shader gl_VertexID/baseVertex uint arithmetic for strict GLSL drivers.");
				loggedVertexIdRewrite = true;
			}
			rewritten = vertexIdFixed;
		}

		if (rewritten.indexOf("_FLW_") >= 0 && rewritten.indexOf("_MASK") >= 0) {
			String uintMaskFixed = FLW_UINT_MASK_BOOLEAN_PATTERN.matcher(rewritten)
				.replaceAll("($1) != 0u;");
			if (!uintMaskFixed.equals(rewritten) && !loggedUintMaskRewrite) {
				Create.LOGGER.warn("Create-UfoPort: patched IrisFLW vertex shader uint mask boolean comparisons for strict GLSL drivers.");
				loggedUintMaskRewrite = true;
			}
			rewritten = uintMaskFixed;
		}
		return rewritten;
	}

	public static boolean shouldPreferIrisFlwWithShaders() {
		return shouldPatch() || isOpenGl33Capable();
	}

	public static boolean hasOpenGl33Context() {
		return isOpenGl33Capable();
	}

	private static int targetVersion(String irisSource, String patchedSource) {
		int flywheelMaxVersion = Math.max(readFlywheelMaxVersion(), 150);
		int patchedVersion = Math.max(readVersion(patchedSource), 150);
		if (flywheelMaxVersion >= 330)
			return Math.min(Math.max(patchedVersion, 330), flywheelMaxVersion);

		int shaderPackVersion = Math.max(readVersion(irisSource), 150);
		return Math.min(shaderPackVersion, flywheelMaxVersion);
	}

	private static int readVersion(String source) {
		Matcher matcher = VERSION_PATTERN.matcher(source);
		if (!matcher.find())
			return 150;
		try {
			return Integer.parseInt(matcher.group(1));
		} catch (NumberFormatException ignored) {
			return 150;
		}
	}

	private static int readFlywheelMaxVersion() {
		int contextMaxVersion = readCurrentContextMaxVersion();
		int flywheelMaxVersion = 150;
		try {
			Class<?> glCompat = Class.forName("dev.engine_room.flywheel.backend.gl.GlCompat");
			Object max = glCompat.getField("MAX_GLSL_VERSION")
				.get(null);
			if (max instanceof Enum<?> maxEnum) {
				String name = maxEnum.name();
				if (name.length() > 1 && name.charAt(0) == 'V')
					flywheelMaxVersion = Integer.parseInt(name.substring(1));
			}
		} catch (Throwable t) {
			if (!loggedFailure) {
				Create.LOGGER.warn("Create-UfoPort: unable to read Flywheel max GLSL version; defaulting IrisFLW shader rewrite to GLSL 150.", t);
				loggedFailure = true;
			}
		}
		if (contextMaxVersion > flywheelMaxVersion) {
			if (!loggedContextOverride) {
				Create.LOGGER.warn("Create-UfoPort: Flywheel static max GLSL version is {}, but the current OpenGL context supports GLSL {}; using the current context for IrisFLW shader compatibility.", flywheelMaxVersion, contextMaxVersion);
				loggedContextOverride = true;
			}
			return contextMaxVersion;
		}
		return flywheelMaxVersion;
	}

	private static int readCurrentContextMaxVersion() {
		int version = Math.max(requestedContextMaxVersion, confirmedContextMaxVersion);
		return Math.max(version, readLiveContextMaxVersion());
	}

	private static int readLiveContextMaxVersion() {
		int version = 150;
		try {
			org.lwjgl.opengl.GLCapabilities capabilities = org.lwjgl.opengl.GL.getCapabilities();
			if (capabilities.OpenGL46)
				version = Math.max(version, 460);
			if (capabilities.OpenGL45)
				version = Math.max(version, 450);
			if (capabilities.OpenGL44)
				version = Math.max(version, 440);
			if (capabilities.OpenGL43)
				version = Math.max(version, 430);
			if (capabilities.OpenGL42)
				version = Math.max(version, 420);
			if (capabilities.OpenGL41)
				version = Math.max(version, 410);
			if (capabilities.OpenGL40)
				version = Math.max(version, 400);
			if (capabilities.OpenGL33)
				version = Math.max(version, 330);
		} catch (Throwable ignored) {
		}
		version = Math.max(version, readGlVersionString(org.lwjgl.opengl.GL20C.GL_SHADING_LANGUAGE_VERSION));
		version = Math.max(version, readGlVersionString(org.lwjgl.opengl.GL20C.GL_VERSION));
		return version;
	}

	private static int readGlVersionString(int name) {
		try {
			return parseGlVersionString(org.lwjgl.opengl.GL20C.glGetString(name));
		} catch (Throwable ignored) {
			return 150;
		}
	}

	private static int parseGlVersionString(String text) {
		if (text == null)
			return 150;
		Matcher matcher = GL_VERSION_STRING_PATTERN.matcher(text);
		if (!matcher.find())
			return 150;
		try {
			int major = Integer.parseInt(matcher.group(1));
			int minor = Integer.parseInt(matcher.group(2));
			return major * 100 + (minor < 10 ? minor * 10 : minor);
		} catch (NumberFormatException ignored) {
			return 150;
		}
	}

	private static boolean isOpenGl33Capable() {
		try {
			return org.lwjgl.opengl.GL.getCapabilities().OpenGL33 || readCurrentContextMaxVersion() >= 330;
		} catch (Throwable ignored) {
			return readCurrentContextMaxVersion() >= 330;
		}
	}

	private static boolean explicitAttribLocationSupported() {
		try {
			org.lwjgl.opengl.GLCapabilities capabilities = org.lwjgl.opengl.GL.getCapabilities();
			return capabilities.OpenGL33 || capabilities.GL_ARB_explicit_attrib_location || readCurrentContextMaxVersion() >= 330;
		} catch (Throwable ignored) {
			return readCurrentContextMaxVersion() >= 330;
		}
	}

	private static String replaceVersion(String source, int version) {
		String versionLine = "#version " + version + (needsCompatibilityProfile(source, version) ? " compatibility" : "");
		Matcher matcher = VERSION_PATTERN.matcher(source);
		if (matcher.find())
			return matcher.replaceFirst(versionLine);
		return versionLine + "\n" + source;
	}

	private static boolean needsCompatibilityProfile(String source, int version) {
		return version >= 150 && (source.contains("gl_ModelView")
			|| source.contains("gl_ProjectionMatrix")
			|| source.contains("gl_TextureMatrix")
			|| source.contains("gl_Normal")
			|| source.contains("gl_Vertex")
			|| source.contains("gl_Color")
			|| source.contains("gl_MultiTexCoord"));
	}

	private static String ensureExtension(String source, String extension) {
		String directive = "#extension " + extension + " : enable";
		if (source.contains("#extension " + extension))
			return source;

		Matcher matcher = VERSION_PATTERN.matcher(source);
		if (!matcher.find())
			return directive + "\n" + source;

		int insert = matcher.end();
		return source.substring(0, insert) + "\n" + directive + source.substring(insert);
	}

	private static String preserveOrRemoveExplicitVertexInputLocations(String source) {
		if (!EXPLICIT_VERTEX_INPUT_PATTERN.matcher(source).find())
			return source;
		if (explicitAttribLocationSupported())
			return ensureExtension(source, "GL_ARB_explicit_attrib_location");
		return EXPLICIT_VERTEX_INPUT_PATTERN.matcher(source)
			.replaceAll("in");
	}

	private static String removeUniformInitializers(String source) {
		return FLW_UNIFORM_INITIALIZER_PATTERN.matcher(source)
			.replaceAll("uniform $1;");
	}

	private static String normalizeLegacyIoQualifiers(String stage, String source) {
		if ("vertex".equals(stage)) {
			source = LEGACY_ATTRIBUTE_PATTERN.matcher(source)
				.replaceAll("in");
			source = LEGACY_VARYING_PATTERN.matcher(source)
				.replaceAll("out");
		} else if ("fragment".equals(stage)) {
			source = LEGACY_VARYING_PATTERN.matcher(source)
				.replaceAll("in");
		}
		return source;
	}

	private static synchronized void dumpShader(String stage, int targetVersion, String irisSource, String patchedSource,
		String rewrittenSource) {
		int limit = Integer.getInteger(DUMP_LIMIT_PROPERTY, 6);
		if (limit <= 0 || dumpedShaders >= limit)
			return;

		int index = dumpedShaders++;
		try {
			Path dir = FabricLoader.getInstance()
				.getConfigDir()
				.resolve("create-ufoport-debug")
				.resolve("irisflw-shaders");
			Files.createDirectories(dir);
			Path dump = dir.resolve(String.format("%02d-%s-glsl%d.txt", index, sanitize(stage), targetVersion));
			String text = "Create-UfoPort IrisFLW shader rewrite dump\n"
				+ "stage=" + stage + "\n"
				+ "targetVersion=" + targetVersion + "\n"
				+ "rewrittenChanged=" + !rewrittenSource.equals(patchedSource) + "\n\n"
				+ "===== rewritten source =====\n"
				+ rewrittenSource
				+ "\n\n===== patched source before rewrite =====\n"
				+ patchedSource
				+ "\n\n===== original iris source =====\n"
				+ irisSource;
			Files.writeString(dump, text, StandardCharsets.UTF_8);
		} catch (Throwable t) {
			if (!loggedFailure) {
				Create.LOGGER.warn("Create-UfoPort: unable to dump rewritten IrisFLW shader source.", t);
				loggedFailure = true;
			}
		}
	}

	private static String sanitize(String text) {
		return text == null ? "unknown" : text.replaceAll("[^A-Za-z0-9._-]", "_");
	}
}
