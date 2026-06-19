package com.simibubi.create.content.kinetics.base;

import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.utility.Color;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;

public abstract class KineticBlockEntityVisual<T extends KineticBlockEntity> extends AbstractBlockEntityVisual<T> {
	public KineticBlockEntityVisual(VisualizationContext context, T blockEntity, float partialTick) {
		super(context, blockEntity, partialTick);
	}

	protected Axis rotationAxis() {
		return rotationAxis(blockState);
	}

	public static float rotationOffset(BlockState state, Axis axis, Vec3i pos) {
		if (shouldOffset(axis, pos))
			return 22.5f;
		return ICogWheel.isLargeCog(state) ? 11.25f : 0;
	}

	public static boolean shouldOffset(Axis axis, Vec3i pos) {
		int x = axis == Axis.X ? 0 : pos.getX();
		int y = axis == Axis.Y ? 0 : pos.getY();
		int z = axis == Axis.Z ? 0 : pos.getZ();
		return (x + y + z) % 2 == 0;
	}

	public static Axis rotationAxis(BlockState blockState) {
		if (blockState.getBlock() instanceof IRotate iRotate)
			return iRotate.getRotationAxis(blockState);
		return Axis.Y;
	}

	public static void applyOverstressEffect(KineticBlockEntity blockEntity, RotatingInstance... instances) {
		float effect = blockEntity.effects.overStressedEffect;
		if (effect != 0) {
			boolean overstressed = effect > 0;
			Color color = overstressed ? Color.RED : Color.SPRING_GREEN;
			float weight = overstressed ? effect : -effect;
			for (RotatingInstance instance : instances)
				instance.setColor(Color.WHITE.mixWith(color, weight));
		} else {
			for (RotatingInstance instance : instances)
				instance.setColor(Color.WHITE);
		}

		for (RotatingInstance instance : instances)
			instance.setChanged();
	}
}
