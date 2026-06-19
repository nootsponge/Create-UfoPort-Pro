package com.simibubi.create.content.kinetics.base;

import java.util.function.Consumer;

import com.simibubi.create.AllBlocks;
import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class SingleAxisRotatingVisual<T extends KineticBlockEntity> extends KineticBlockEntityVisual<T> {
	protected final RotatingInstance rotatingModel;

	public SingleAxisRotatingVisual(VisualizationContext context, T blockEntity, float partialTick) {
		this(context, blockEntity, partialTick, Models.block(blockEntity.getBlockState()));
	}

	public SingleAxisRotatingVisual(VisualizationContext context, T blockEntity, float partialTick, Model model) {
		this(context, blockEntity, partialTick, Direction.UP, model);
	}

	public SingleAxisRotatingVisual(VisualizationContext context, T blockEntity, float partialTick, Direction from,
		Model model) {
		super(context, blockEntity, partialTick);
		rotatingModel = instancerProvider().instancer(AllInstanceTypes.ROTATING, model)
			.createInstance()
			.rotateToFace(from, rotationAxis())
			.setup(blockEntity)
			.setPosition(getVisualPosition());
		rotatingModel.setChanged();
	}

	public static <T extends KineticBlockEntity> SingleAxisRotatingVisual<T> shaft(VisualizationContext context,
		T blockEntity, float partialTick) {
		BlockState shaft = AllBlocks.SHAFT.getDefaultState()
			.setValue(ShaftBlock.AXIS, rotationAxis(blockEntity.getBlockState()));
		return new SingleAxisRotatingVisual<>(context, blockEntity, partialTick, Models.block(shaft));
	}

	public static <T extends KineticBlockEntity> SimpleBlockEntityVisualizer.Factory<T> of(PartialModel partial) {
		return (context, blockEntity, partialTick) ->
			new SingleAxisRotatingVisual<>(context, blockEntity, partialTick, KineticVisualModels.partial(partial));
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
