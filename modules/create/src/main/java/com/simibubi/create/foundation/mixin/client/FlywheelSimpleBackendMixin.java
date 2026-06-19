package com.simibubi.create.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.foundation.render.FlywheelBackendSafety;

import dev.engine_room.flywheel.api.backend.Backend;

@Pseudo
@Mixin(targets = "dev.engine_room.flywheel.lib.backend.SimpleBackend", remap = false)
public class FlywheelSimpleBackendMixin {

	@Inject(method = "isSupported()Z", at = @At("HEAD"), cancellable = true, require = 0)
	private void create_ufoport$forceInstancingSupported(CallbackInfoReturnable<Boolean> cir) {
		if (!FlywheelBackendSafety.shouldForce())
			return;
		Backend backend = (Backend) (Object) this;
		if (FlywheelBackendSafety.isForced(backend)) {
			cir.setReturnValue(true);
			return;
		}
		if (FlywheelBackendSafety.isOff(backend))
			cir.setReturnValue(false);
	}
}
