package com.simibubi.create.foundation.render;

import net.minecraft.world.level.LevelAccessor;

public final class FlywheelVisualHelper {
	private FlywheelVisualHelper() {
	}

	public static boolean isVisualizationActive(LevelAccessor level) {
		if (level == null || !level.isClientSide())
			return false;

		try {
			Class<?> backendManager = Class.forName("dev.engine_room.flywheel.impl.BackendManagerImpl");
			Object active = backendManager.getMethod("isBackendOn")
				.invoke(null);
			return Boolean.TRUE.equals(active);
		} catch (Throwable ignored) {
			return false;
		}
	}
}
