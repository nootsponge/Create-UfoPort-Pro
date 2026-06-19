package com.simibubi.create.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.foundation.render.IrisFlwPerformanceTuning;

@Pseudo
@Mixin(targets = "top.leonx.irisflw.backend.IrisInstancedDrawManager", remap = false)
public class IrisFlwInstancedDrawManagerMixin {
	@Inject(
		method = "render(Ldev/engine_room/flywheel/backend/engine/LightStorage;Ldev/engine_room/flywheel/backend/engine/embed/EnvironmentStorage;)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void create_ufoport$skipIrisFlwShadowPass(CallbackInfo ci) {
		if (!IrisFlwPerformanceTuning.shouldSkipIrisFlwShadowPass())
			return;
		if (!IrisFlwPerformanceTuning.isRenderingIrisFlwShadowPass())
			return;

		IrisFlwPerformanceTuning.logIrisFlwShadowPassSkipped();
		ci.cancel();
	}
}
