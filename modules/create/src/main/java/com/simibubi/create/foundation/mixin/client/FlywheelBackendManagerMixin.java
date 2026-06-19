package com.simibubi.create.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.render.FlywheelBackendSafety;

import dev.engine_room.flywheel.api.backend.Backend;

@Pseudo
@Mixin(targets = "dev.engine_room.flywheel.impl.BackendManagerImpl", remap = false)
public class FlywheelBackendManagerMixin {
	@Shadow
	private static Backend backend;

	@Shadow
	public static Backend OFF_BACKEND;

	@Unique
	private static boolean create_ufoport$loggedShaderOff;
	@Unique
	private static boolean create_ufoport$loggedShaderIrisFlw;

	@Inject(method = "chooseBackend()V", at = @At("HEAD"), cancellable = true, require = 0)
	private static void create_ufoport$chooseShaderSafeBackend(CallbackInfo ci) {
		if (!FlywheelBackendSafety.shouldManageActiveShaders())
			return;

		Backend shaderSafeBackend = FlywheelBackendSafety.chooseForActiveShaders(OFF_BACKEND);
		if (shaderSafeBackend == null)
			return;

		backend = shaderSafeBackend;
		if (FlywheelBackendSafety.isForced(shaderSafeBackend)) {
			if (!create_ufoport$loggedShaderIrisFlw) {
				Create.LOGGER.warn("Create-UfoPort: active Iris shaderpack detected; forcing Flywheel 1.x backend to irisflw:iris_instancing.");
				create_ufoport$loggedShaderIrisFlw = true;
			}
		} else if (!create_ufoport$loggedShaderOff) {
			Create.LOGGER.warn("Create-UfoPort: active Iris shaderpack detected before Flywheel backend selection; forcing Flywheel 1.x backend to off.");
			create_ufoport$loggedShaderOff = true;
		}
		ci.cancel();
	}

	@Inject(method = "chooseBackend()V", at = @At("RETURN"), require = 0)
	private static void create_ufoport$guardShaderBackendChoice(CallbackInfo ci) {
		backend = create_ufoport$safeBackend();
	}

	@Unique
	private static Backend create_ufoport$safeBackend() {
		return FlywheelBackendSafety.sanitizeForActiveShaders(backend, OFF_BACKEND);
	}
}
