package com.simibubi.create.content.kinetics.simpleRelays.encased;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.KineticVisualModels;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import com.simibubi.create.foundation.utility.Iterate;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class EncasedCogVisual<T extends KineticBlockEntity> extends KineticBlockEntityVisual<T> {
	protected final boolean large;
	protected final RotatingInstance rotatingModel;
	protected final List<RotatingInstance> shafts = new ArrayList<>(2);

	public static <T extends KineticBlockEntity> EncasedCogVisual<T> small(VisualizationContext context, T blockEntity,
		float partialTick) {
		return new EncasedCogVisual<>(context, blockEntity, false, partialTick);
	}

	public static <T extends KineticBlockEntity> EncasedCogVisual<T> large(VisualizationContext context, T blockEntity,
		float partialTick) {
		return new EncasedCogVisual<>(context, blockEntity, true, partialTick);
	}

	public EncasedCogVisual(VisualizationContext context, T blockEntity, boolean large, float partialTick) {
		super(context, blockEntity, partialTick);
		this.large = large;

		Axis axis = blockState.getValue(BlockStateProperties.AXIS);
		Direction facing = Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE);
		rotatingModel = instancerProvider().instancer(AllInstanceTypes.ROTATING,
				KineticVisualModels.partialFacingVertical(
					large ? AllPartialModels.SHAFTLESS_LARGE_COGWHEEL : AllPartialModels.SHAFTLESS_COGWHEEL, facing))
			.createInstance();
		setupCog();
		setupShafts();
	}

	private void setupCog() {
		rotatingModel.setup(blockEntity)
			.setPosition(getVisualPosition())
			.setChanged();
	}

	private void setupShafts() {
		Block block = blockState.getBlock();
		if (!(block instanceof IRotate def))
			return;

		for (Direction direction : Iterate.directionsInAxis(rotationAxis())) {
			if (!def.hasShaftTowards(blockEntity.getLevel(), blockEntity.getBlockPos(), blockState, direction))
				continue;
			RotatingInstance shaft = instancerProvider().instancer(AllInstanceTypes.ROTATING,
					KineticVisualModels.partialFacing(AllPartialModels.SHAFT_HALF, direction))
				.createInstance();
			setupShaft(shaft);
			shafts.add(shaft);
		}
	}

	private void setupShaft(RotatingInstance shaft) {
		shaft.setup(blockEntity)
			.setPosition(getVisualPosition());
		if (large)
			shaft.setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(rotationAxis(), pos));
		shaft.setChanged();
	}

	@Override
	public void update(float partialTick) {
		setupCog();
		shafts.forEach(this::setupShaft);
	}

	@Override
	public void updateLight(float partialTick) {
		relight(rotatingModel);
		shafts.forEach(this::relight);
	}

	@Override
	protected void _delete() {
		rotatingModel.delete();
		shafts.forEach(RotatingInstance::delete);
		shafts.clear();
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		consumer.accept(rotatingModel);
		shafts.forEach(consumer::accept);
	}
}
