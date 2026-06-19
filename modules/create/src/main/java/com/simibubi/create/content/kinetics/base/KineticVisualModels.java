package com.simibubi.create.content.kinetics.base;

import com.jozufozu.flywheel.core.PartialModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.render.CachedBufferer;

import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBuilder;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import net.minecraft.core.Direction;

public class KineticVisualModels {
	private static final RendererReloadCache<PartialKey, Model> PARTIALS =
		new RendererReloadCache<>(KineticVisualModels::createPartial);

	private KineticVisualModels() {
	}

	public static Model partial(PartialModel partial) {
		return PARTIALS.get(new PartialKey(partial, Direction.UP, Transform.NONE));
	}

	public static Model partialFacing(PartialModel partial, Direction facing) {
		return PARTIALS.get(new PartialKey(partial, facing, Transform.FACE));
	}

	public static Model partialFacingVertical(PartialModel partial, Direction facing) {
		return PARTIALS.get(new PartialKey(partial, facing, Transform.FACE_VERTICAL));
	}

	private static Model createPartial(PartialKey key) {
		if (key.transform() == Transform.NONE)
			return new BakedModelBuilder(key.partial()
				.get()).build();

		PoseStack transform = switch (key.transform()) {
			case FACE -> CachedBufferer.rotateToFace(key.direction())
				.get();
			case FACE_VERTICAL -> CachedBufferer.rotateToFaceVertical(key.direction())
				.get();
			case NONE -> throw new IllegalStateException("Unhandled transform");
		};

		return new BakedModelBuilder(key.partial()
			.get()).poseStack(transform)
			.build();
	}

	private enum Transform {
		NONE,
		FACE,
		FACE_VERTICAL
	}

	private record PartialKey(PartialModel partial, Direction direction, Transform transform) {
	}
}
