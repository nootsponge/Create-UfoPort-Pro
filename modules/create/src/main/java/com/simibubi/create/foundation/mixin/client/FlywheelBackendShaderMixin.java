package com.simibubi.create.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "dev.engine_room.flywheel.backend.Backends", remap = false)
public class FlywheelBackendShaderMixin {

	@Redirect(method = { "lambda$static$1()Z", "lambda$static$4()Z" }, at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/lib/util/ShadersModHelper;isShaderPackInUse()Z"), require = 0)
	private static boolean create_ufoport$allowBackendWithShaders() {
		return false;
	}
}
