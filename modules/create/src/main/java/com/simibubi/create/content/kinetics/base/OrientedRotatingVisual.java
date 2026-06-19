package com.simibubi.create.content.kinetics.base;

import java.util.function.Consumer;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class OrientedRotatingVisual<T extends KineticBlockEntity> extends KineticBlockEntityVisual<T> {
	protected final RotatingInstance rotatingModel;

	public OrientedRotatingVisual(VisualizationContext context, T blockEntity, float partialTick, Model model) {
		super(context, blockEntity, partialTick);
		rotatingModel = instancerProvider().instancer(AllInstanceTypes.ROTATING, model)
			.createInstance()
			.setup(blockEntity)
			.setPosition(getVisualPosition());
		rotatingModel.setChanged();
	}

	public static <T extends KineticBlockEntity> OrientedRotatingVisual<T> halfShaft(VisualizationContext context,
		T blockEntity, float partialTick) {
		Direction direction = blockEntity.getBlockState()
			.getValue(BlockStateProperties.FACING);
		return new OrientedRotatingVisual<>(context, blockEntity, partialTick,
			KineticVisualModels.partialFacing(AllPartialModels.SHAFT_HALF, direction));
	}

	public static <T extends KineticBlockEntity> OrientedRotatingVisual<T> horizontalHalfShaft(
		VisualizationContext context, T blockEntity, float partialTick) {
		Direction direction = blockEntity.getBlockState()
			.getValue(BlockStateProperties.HORIZONTAL_FACING)
			.getOpposite();
		return new OrientedRotatingVisual<>(context, blockEntity, partialTick,
			KineticVisualModels.partialFacing(AllPartialModels.SHAFT_HALF, direction));
	}

	public static <T extends DrillBlockEntity> OrientedRotatingVisual<T> drill(VisualizationContext context,
		T blockEntity, float partialTick) {
		Direction direction = blockEntity.getBlockState()
			.getValue(BlockStateProperties.FACING);
		return new OrientedRotatingVisual<>(context, blockEntity, partialTick,
			KineticVisualModels.partialFacing(AllPartialModels.DRILL_HEAD, direction));
	}

	public static <T extends PumpBlockEntity> OrientedRotatingVisual<T> pumpCog(VisualizationContext context,
		T blockEntity, float partialTick) {
		Direction direction = blockEntity.getBlockState()
			.getValue(BlockStateProperties.FACING);
		return new OrientedRotatingVisual<>(context, blockEntity, partialTick,
			KineticVisualModels.partialFacing(AllPartialModels.MECHANICAL_PUMP_COG, direction));
	}

	public static <T extends MillstoneBlockEntity> OrientedRotatingVisual<T> millstoneCog(VisualizationContext context,
		T blockEntity, float partialTick) {
		return new OrientedRotatingVisual<>(context, blockEntity, partialTick,
			KineticVisualModels.partial(AllPartialModels.MILLSTONE_COG));
	}

	@Override
	public void update(float partialTick) {
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
}
