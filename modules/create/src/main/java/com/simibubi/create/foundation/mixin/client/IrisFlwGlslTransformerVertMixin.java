package com.simibubi.create.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.foundation.render.IrisFlwGlslVersionCompat;

import dev.engine_room.flywheel.api.material.LightShader;

@Pseudo
@Mixin(targets = "top.leonx.irisflw.transformer.GlslTransformerVertPatcher", remap = false)
public class IrisFlwGlslTransformerVertMixin {
	@Inject(
		method = "patch(Ljava/lang/String;Ljava/lang/String;ZZLdev/engine_room/flywheel/api/material/LightShader;Z)Ljava/lang/String;",
		at = @At("RETURN"),
		cancellable = true,
		require = 0
	)
	private void create_ufoport$restoreFlywheelMaxGlslVersion(String irisSource, String flywheelTemplate,
		boolean shadow, boolean embedded, LightShader lightShader, boolean extendedVertexFormat,
		CallbackInfoReturnable<String> cir) {
		cir.setReturnValue(IrisFlwGlslVersionCompat.restoreFlywheelMaxVersion("vertex", irisSource, cir.getReturnValue()));
	}
}
