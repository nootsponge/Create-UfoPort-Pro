package com.simibubi.create.content.kinetics.fan;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

import java.util.function.Consumer;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.KineticVisualModels;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public class FanVisual extends KineticBlockEntityVisual<EncasedFanBlockEntity> {
	protected final RotatingInstance shaft;
	protected final RotatingInstance fan;
	protected final Direction direction;
	protected final Direction opposite;

	public FanVisual(VisualizationContext context, EncasedFanBlockEntity blockEntity, float partialTick) {
		super(context, blockEntity, partialTick);

		direction = blockState.getValue(FACING);
		opposite = direction.getOpposite();

		shaft = instancerProvider().instancer(AllInstanceTypes.ROTATING,
				KineticVisualModels.partialFacing(AllPartialModels.SHAFT_HALF, opposite))
			.createInstance();
		fan = instancerProvider().instancer(AllInstanceTypes.ROTATING,
				KineticVisualModels.partialFacing(AllPartialModels.ENCASED_FAN_INNER, opposite))
			.createInstance();

		shaft.setup(blockEntity)
			.setPosition(getVisualPosition())
			.setChanged();
		fan.setup(blockEntity, getFanSpeed())
			.setPosition(getVisualPosition())
			.setChanged();
	}

	private float getFanSpeed() {
		float speed = blockEntity.getSpeed() * 5;
		if (speed > 0)
			speed = Mth.clamp(speed, 80, 64 * 20);
		if (speed < 0)
			speed = Mth.clamp(speed, -64 * 20, -80);
		return speed;
	}

	@Override
	public void update(float partialTick) {
		shaft.setup(blockEntity)
			.setChanged();
		fan.setup(blockEntity, getFanSpeed())
			.setChanged();
	}

	@Override
	public void updateLight(float partialTick) {
		relight(pos.relative(opposite), shaft);
		relight(pos.relative(direction), fan);
	}

	@Override
	protected void _delete() {
		shaft.delete();
		fan.delete();
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		consumer.accept(shaft);
		consumer.accept(fan);
	}
}
