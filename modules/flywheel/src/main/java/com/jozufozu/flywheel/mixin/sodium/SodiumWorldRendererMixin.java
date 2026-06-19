package com.jozufozu.flywheel.mixin.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jozufozu.flywheel.event.RenderLayerEvent;
import com.jozufozu.flywheel.fabric.event.FlywheelEvents;
import com.mojang.blaze3d.vertex.PoseStack;

import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;

@Mixin(value = SodiumWorldRenderer.class, remap = false)
public class SodiumWorldRendererMixin {
	@Inject(method = "drawChunkLayer", at = @At("TAIL"))
	private void flywheel$renderLayer(RenderType type, ChunkRenderMatrices matrices, double camX, double camY,
			double camZ, CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return;
		}

		PoseStack stack = new PoseStack();

		FlywheelEvents.RENDER_LAYER.invoker()
				.handleEvent(new RenderLayerEvent(level, type, stack, minecraft.renderBuffers(), camX, camY, camZ,
						matrices.modelView(), matrices.projection()));
	}
}
