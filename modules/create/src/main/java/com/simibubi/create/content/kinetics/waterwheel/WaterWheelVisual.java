package com.simibubi.create.content.kinetics.waterwheel;

import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import com.simibubi.create.foundation.render.CachedBufferer;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBuilder;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;

public class WaterWheelVisual<T extends WaterWheelBlockEntity> extends KineticBlockEntityVisual<T> {
	private static final RendererReloadCache<WaterWheelModelKey, Model> MODEL_CACHE =
		new RendererReloadCache<>(WaterWheelVisual::createModel);

	protected final boolean large;
	protected BlockState lastMaterial;
	protected RotatingInstance rotatingModel;

	public WaterWheelVisual(VisualizationContext context, T blockEntity, boolean large, float partialTick) {
		super(context, blockEntity, partialTick);
		this.large = large;
		setupInstance();
	}

	public static <T extends WaterWheelBlockEntity> WaterWheelVisual<T> standard(VisualizationContext context,
		T blockEntity, float partialTick) {
		return new WaterWheelVisual<>(context, blockEntity, false, partialTick);
	}

	public static <T extends WaterWheelBlockEntity> WaterWheelVisual<T> large(VisualizationContext context,
		T blockEntity, float partialTick) {
		return new WaterWheelVisual<>(context, blockEntity, true, partialTick);
	}

	private void setupInstance() {
		lastMaterial = blockEntity.material;
		WaterWheelModelKey key = new WaterWheelModelKey(large, blockState, blockEntity.material);
		rotatingModel = instancerProvider().instancer(AllInstanceTypes.ROTATING, MODEL_CACHE.get(key))
			.createInstance();
		rotatingModel.setup(blockEntity)
			.setPosition(getVisualPosition())
			.setChanged();
	}

	@Override
	public void update(float partialTick) {
		if (lastMaterial != blockEntity.material) {
			rotatingModel.delete();
			setupInstance();
			return;
		}

		rotatingModel.setup(blockEntity)
			.setChanged();
	}

	@Override
	public void updateLight(float partialTick) {
		relight(rotatingModel);
	}

	@Override
	protected void _delete() {
		rotatingModel.delete();
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		consumer.accept(rotatingModel);
	}

	private static Model createModel(WaterWheelModelKey key) {
		BakedModel model = WaterWheelRenderer.generateModel(key);
		BlockState state = key.state();
		Direction direction;
		if (key.large()) {
			direction = Direction.fromAxisAndDirection(state.getValue(LargeWaterWheelBlock.AXIS),
				AxisDirection.POSITIVE);
		} else {
			direction = state.getValue(WaterWheelBlock.FACING);
		}

		PoseStack transform = CachedBufferer.rotateToFaceVertical(direction)
			.get();
		return new BakedModelBuilder(model)
			.poseStack(transform)
			.build();
	}
}
