package com.simibubi.create.content.kinetics.gearbox;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.KineticVisualModels;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import com.simibubi.create.foundation.utility.Iterate;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class GearboxVisual extends KineticBlockEntityVisual<GearboxBlockEntity> {
	protected final EnumMap<Direction, RotatingInstance> shafts;
	protected Direction sourceFacing;

	public GearboxVisual(VisualizationContext context, GearboxBlockEntity blockEntity, float partialTick) {
		super(context, blockEntity, partialTick);

		shafts = new EnumMap<>(Direction.class);
		updateSourceFacing();

		final Axis boxAxis = blockState.getValue(BlockStateProperties.AXIS);

		for (Direction direction : Iterate.directions) {
			final Axis axis = direction.getAxis();
			if (boxAxis == axis)
				continue;

			RotatingInstance shaft = instancerProvider().instancer(AllInstanceTypes.ROTATING,
					KineticVisualModels.partialFacing(AllPartialModels.SHAFT_HALF, direction))
				.createInstance()
				.setup(blockEntity, axis, getSpeed(direction))
				.setPosition(getVisualPosition());
			shaft.setChanged();
			shafts.put(direction, shaft);
		}
	}

	private float getSpeed(Direction direction) {
		float speed = blockEntity.getSpeed();

		if (speed != 0 && sourceFacing != null) {
			if (sourceFacing.getAxis() == direction.getAxis())
				speed *= sourceFacing == direction ? 1 : -1;
			else if (sourceFacing.getAxisDirection() == direction.getAxisDirection())
				speed *= -1;
		}
		return speed;
	}

	protected void updateSourceFacing() {
		if (blockEntity.hasSource()) {
			BlockPos source = blockEntity.source.subtract(pos);
			sourceFacing = Direction.getNearest(source.getX(), source.getY(), source.getZ());
		} else {
			sourceFacing = null;
		}
	}

	@Override
	public void update(float partialTick) {
		updateSourceFacing();
		for (Map.Entry<Direction, RotatingInstance> entry : shafts.entrySet()) {
			Direction direction = entry.getKey();
			entry.getValue()
				.setup(blockEntity, direction.getAxis(), getSpeed(direction))
				.setChanged();
		}
	}

	@Override
	public void updateLight(float partialTick) {
		for (RotatingInstance shaft : shafts.values())
			relight(shaft);
	}

	@Override
	protected void _delete() {
		shafts.values()
			.forEach(RotatingInstance::delete);
		shafts.clear();
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		shafts.values()
			.forEach(consumer::accept);
	}
}
