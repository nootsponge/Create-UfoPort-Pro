package com.simibubi.create.foundation.mixin.client;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.BufferBuilder;

import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;

@Mixin(targets = "dev.engine_room.flywheel.lib.model.baked.FabricMeshEmitterManager", remap = false)
public abstract class IrisFlywheelFabricMeshEmitterManagerMixin implements BlockSensitiveBufferBuilder {
	@Shadow(remap = false)
	private BufferBuilder currentDelegate;

	@Unique
	private final Set<BlockSensitiveBufferBuilder> create$irisflw$activeDelegates =
		Collections.newSetFromMap(new IdentityHashMap<>());
	@Unique
	private int create$irisflw$currentBlock = -1;
	@Unique
	private byte create$irisflw$currentRenderType = -1;
	@Unique
	private byte create$irisflw$currentBlockEmission;
	@Unique
	private int create$irisflw$currentLocalPosX;
	@Unique
	private int create$irisflw$currentLocalPosY;
	@Unique
	private int create$irisflw$currentLocalPosZ;

	@Inject(method = "prepareForGeometry(Lnet/fabricmc/fabric/api/renderer/v1/material/RenderMaterial;)V",
		at = @At("TAIL"), remap = false, require = 0)
	private void create$irisflw$prepareForGeometry(RenderMaterial material, CallbackInfo ci) {
		if (currentDelegate instanceof BlockSensitiveBufferBuilder blockSensitive) {
			blockSensitive.beginBlock(create$irisflw$currentBlock, create$irisflw$currentRenderType,
				create$irisflw$currentBlockEmission, create$irisflw$currentLocalPosX, create$irisflw$currentLocalPosY,
				create$irisflw$currentLocalPosZ);
			create$irisflw$activeDelegates.add(blockSensitive);
		}
	}

	@Override
	public void beginBlock(int block, byte renderType, byte blockEmission, int localPosX, int localPosY, int localPosZ) {
		create$irisflw$currentBlock = block;
		create$irisflw$currentRenderType = renderType;
		create$irisflw$currentBlockEmission = blockEmission;
		create$irisflw$currentLocalPosX = localPosX;
		create$irisflw$currentLocalPosY = localPosY;
		create$irisflw$currentLocalPosZ = localPosZ;
	}

	@Override
	public void overrideBlock(int block) {
		create$irisflw$forEachDelegate(delegate -> delegate.overrideBlock(block));
	}

	@Override
	public void restoreBlock() {
		create$irisflw$forEachDelegate(BlockSensitiveBufferBuilder::restoreBlock);
	}

	@Override
	public void endBlock() {
		create$irisflw$currentBlock = -1;
		create$irisflw$currentRenderType = -1;
		create$irisflw$currentBlockEmission = 0;
		create$irisflw$currentLocalPosX = 0;
		create$irisflw$currentLocalPosY = 0;
		create$irisflw$currentLocalPosZ = 0;
		create$irisflw$activeDelegates.forEach(BlockSensitiveBufferBuilder::endBlock);
		create$irisflw$activeDelegates.clear();
	}

	@Override
	public void ignoreMidBlock(boolean ignore) {
		create$irisflw$forEachDelegate(delegate -> delegate.ignoreMidBlock(ignore));
	}

	@Unique
	private void create$irisflw$forEachDelegate(java.util.function.Consumer<BlockSensitiveBufferBuilder> consumer) {
		create$irisflw$activeDelegates.forEach(consumer);
		if (currentDelegate instanceof BlockSensitiveBufferBuilder blockSensitive
			&& !create$irisflw$activeDelegates.contains(blockSensitive)) {
			consumer.accept(blockSensitive);
		}
	}
}
