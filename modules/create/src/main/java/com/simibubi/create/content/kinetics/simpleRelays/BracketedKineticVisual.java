package com.simibubi.create.content.kinetics.simpleRelays;

import java.util.function.Consumer;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.KineticVisualModels;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;

public class BracketedKineticVisual extends KineticBlockEntityVisual<BracketedKineticBlockEntity> {
	protected final boolean large;
	protected final RotatingInstance rotatingModel;
	protected RotatingInstance additionalShaft;

	public BracketedKineticVisual(VisualizationContext context, BracketedKineticBlockEntity blockEntity,
		float partialTick) {
		super(context, blockEntity, partialTick);
		large = ICogWheel.isLargeCog(blockState);

		rotatingModel = instancerProvider().instancer(AllInstanceTypes.ROTATING, getCogModel())
			.createInstance();
		setupRotatingModel(rotatingModel);

		if (large) {
			Axis axis = rotationAxis();
			Direction facing = Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE);
			additionalShaft = instancerProvider().instancer(AllInstanceTypes.ROTATING,
					KineticVisualModels.partialFacingVertical(AllPartialModels.COGWHEEL_SHAFT, facing))
				.createInstance();
			setupLargeCogShaft();
		}
	}

	private Model getCogModel() {
		if (!large) {
			if (ICogWheel.isSmallCog(blockState))
				return KineticVisualModels.partial(AllPartialModels.COGWHEEL);
			return KineticVisualModels.partial(AllPartialModels.SHAFT);
		}

		Direction facing = Direction.fromAxisAndDirection(rotationAxis(), AxisDirection.POSITIVE);
		return KineticVisualModels.partialFacingVertical(AllPartialModels.SHAFTLESS_LARGE_COGWHEEL, facing);
	}

	private void setupRotatingModel(RotatingInstance instance) {
		instance.setup(blockEntity)
			.setPosition(getVisualPosition());
		if (!large)
			instance.rotateToFace(Direction.UP, rotationAxis());
		instance.setChanged();
	}

	private void setupLargeCogShaft() {
		additionalShaft.setup(blockEntity)
			.setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(rotationAxis(), pos))
			.setPosition(getVisualPosition())
			.setChanged();
	}

	@Override
	public void update(float partialTick) {
		setupRotatingModel(rotatingModel);
		if (additionalShaft != null)
			setupLargeCogShaft();
	}

	@Override
	public void updateLight(float partialTick) {
		relight(rotatingModel);
		if (additionalShaft != null)
			relight(additionalShaft);
	}

	@Override
	protected void _delete() {
		rotatingModel.delete();
		if (additionalShaft != null)
			additionalShaft.delete();
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		consumer.accept(rotatingModel);
		if (additionalShaft != null)
			consumer.accept(additionalShaft);
	}
}
