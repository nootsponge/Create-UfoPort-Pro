package com.simibubi.create.foundation.mixin;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.fabricmc.loader.api.FabricLoader;

public class CreateMixinPlugin implements IMixinConfigPlugin {
	private static final String IRISFLW_FABRIC_MESH_EMITTER_MANAGER =
		"com.simibubi.create.foundation.mixin.client.IrisFlywheelFabricMeshEmitterManagerMixin";
	private static final String IRISFLW_GLSL_TRANSFORMER_FRAG =
		"com.simibubi.create.foundation.mixin.client.IrisFlwGlslTransformerFragMixin";
	private static final String IRISFLW_GLSL_TRANSFORMER_VERT =
		"com.simibubi.create.foundation.mixin.client.IrisFlwGlslTransformerVertMixin";
	private static final String IRISFLW_INSTANCED_DRAW_MANAGER =
		"com.simibubi.create.foundation.mixin.client.IrisFlwInstancedDrawManagerMixin";
	private static final String DISABLE_IRISFLW_WITH_SHADERS_PROPERTY =
		"create_ufoport.flywheel.disableIrisFlwWithShaders";

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (IRISFLW_FABRIC_MESH_EMITTER_MANAGER.equals(mixinClassName)) {
			FabricLoader loader = FabricLoader.getInstance();
			return loader.isModLoaded("iris") && loader.isModLoaded("irisflw") && loader.isModLoaded("flywheel")
				&& !shouldDisableIrisFlywheelCompat();
		}
		if (IRISFLW_GLSL_TRANSFORMER_FRAG.equals(mixinClassName)
			|| IRISFLW_GLSL_TRANSFORMER_VERT.equals(mixinClassName)
			|| IRISFLW_INSTANCED_DRAW_MANAGER.equals(mixinClassName)) {
			FabricLoader loader = FabricLoader.getInstance();
			return loader.isModLoaded("iris") && loader.isModLoaded("irisflw") && loader.isModLoaded("flywheel")
				&& !shouldDisableIrisFlywheelCompat();
		}

		return true;
	}

	private static boolean shouldDisableIrisFlywheelCompat() {
		return Boolean.getBoolean(DISABLE_IRISFLW_WITH_SHADERS_PROPERTY);
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
