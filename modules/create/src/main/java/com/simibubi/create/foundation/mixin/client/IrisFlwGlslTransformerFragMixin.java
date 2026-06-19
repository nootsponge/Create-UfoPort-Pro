package com.simibubi.create.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.foundation.render.IrisFlwGlslVersionCompat;

@Pseudo
@Mixin(targets = "top.leonx.irisflw.transformer.GlslTransformerFragPatcher", remap = false)
public class IrisFlwGlslTransformerFragMixin {
	@Inject(method = "patch(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", at = @At("RETURN"), cancellable = true, require = 0)
	private void create_ufoport$restoreFlywheelMaxGlslVersion(String irisSource, String flywheelTemplate,
		CallbackInfoReturnable<String> cir) {
		cir.setReturnValue(IrisFlwGlslVersionCompat.restoreFlywheelMaxVersion("fragment", irisSource, cir.getReturnValue()));
	}
}
