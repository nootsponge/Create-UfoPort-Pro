package com.simibubi.create.foundation.mixin.client;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.render.IrisFlwGlslVersionCompat;

@Mixin(Window.class)
public class OpenGlContextVersionMixin {
	private static final String DISABLE_PROPERTY = "create_ufoport.flywheel.disableOpenGl33ContextHint";
	private static boolean logged;

	@Inject(
		method = "<init>",
		at = @At(
			value = "INVOKE",
			target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J",
			shift = At.Shift.BEFORE,
			remap = false
		)
	)
	private void create_ufoport$requestOpenGl33Context(WindowEventHandler eventHandler, ScreenManager screenManager,
		DisplayData displayData, String preferredFullscreenVideoMode, String title, CallbackInfo ci) {
		if (Boolean.getBoolean(DISABLE_PROPERTY))
			return;

		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
		IrisFlwGlslVersionCompat.recordRequestedOpenGl33Context();

		if (!logged) {
			Create.LOGGER.warn("Create-UfoPort: requesting an OpenGL 3.3 context before window creation so IrisFLW can use its native GLSL 330 shader path.");
			logged = true;
		}
	}
}
