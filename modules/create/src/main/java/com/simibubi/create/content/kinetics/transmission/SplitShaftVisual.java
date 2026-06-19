package com.simibubi.create.content.kinetics.transmission;

import java.util.ArrayList;
import java.util.function.Consumer;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.KineticVisualModels;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import com.simibubi.create.foundation.utility.Iterate;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.core.Direction;

public class SplitShaftVisual<T extends SplitShaftBlockEntity> extends KineticBlockEntityVisual<T> {
	protected final ArrayList<RotatingInstance> shafts;
	protected final Direction[] directions;

	public SplitShaftVisual(VisualizationContext context, T blockEntity, float partialTick) {
		super(context, blockEntity, partialTick);

		shafts = new ArrayList<>(2);
		directions = Iterate.directionsInAxis(rotationAxis());

		for (Direction direction : directions) {
			RotatingInstance shaft = instancerProvider().instancer(AllInstanceTypes.ROTATING,
					KineticVisualModels.partialFacing(AllPartialModels.SHAFT_HALF, direction))
				.createInstance()
				.setup(blockEntity, direction.getAxis(), blockEntity.getSpeed() * blockEntity.getRotationSpeedModifier(direction))
				.setPosition(getVisualPosition());
			shaft.setChanged();
			shafts.add(shaft);
		}
	}

	@Override
	public void update(float partialTick) {
		for (int i : Iterate.zeroAndOne) {
			Direction direction = directions[i];
			shafts.get(i)
				.setup(blockEntity, direction.getAxis(), blockEntity.getSpeed() * blockEntity.getRotationSpeedModifier(direction))
				.setChanged();
		}
	}

	@Override
	public void updateLight(float partialTick) {
		for (RotatingInstance shaft : shafts)
			relight(shaft);
	}

	@Override
	protected void _delete() {
		shafts.forEach(RotatingInstance::delete);
		shafts.clear();
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		shafts.forEach(consumer::accept);
	}
}
