package com.simibubi.create.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.foundation.render.FlywheelBackendSafety;
import com.simibubi.create.foundation.render.IrisFlwGlslVersionCompat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

@Mixin(Minecraft.class)
public class MinecraftOpenGlContextMixin {
	@Shadow
	public ClientLevel level;

	@Inject(method = "run", at = @At("HEAD"), require = 0)
	private void create_ufoport$recordOpenGlContext(CallbackInfo ci) {
		IrisFlwGlslVersionCompat.recordCurrentOpenGlContext("Minecraft.run");
	}

	@Inject(method = "runTick", at = @At("HEAD"), require = 0)
	private void create_ufoport$markIrisFlwWorldTick(CallbackInfo ci) {
		FlywheelBackendSafety.markIrisFlwWorldTick(level != null);
	}
}
