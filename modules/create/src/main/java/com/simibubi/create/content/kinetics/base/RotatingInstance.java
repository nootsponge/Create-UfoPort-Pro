package com.simibubi.create.content.kinetics.base;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.simibubi.create.foundation.utility.Color;

import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.lib.instance.ColoredLitOverlayInstance;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;

public class RotatingInstance extends ColoredLitOverlayInstance {
	public static final float SPEED_MULTIPLIER = 6;

	public byte rotationAxisX;
	public byte rotationAxisY;
	public byte rotationAxisZ;
	public float x;
	public float y;
	public float z;
	public float rotationalSpeed;
	public float rotationOffset;
	public final Quaternionf rotation = new Quaternionf();

	public RotatingInstance(InstanceType<? extends RotatingInstance> type, InstanceHandle handle) {
		super(type, handle);
	}

	public static int colorFromBE(KineticBlockEntity blockEntity) {
		float effect = blockEntity.effects.overStressedEffect;
		if (effect > 0)
			return Color.WHITE.copy()
				.mixWith(Color.RED, effect)
				.getRGB();
		if (effect < 0)
			return Color.WHITE.copy()
				.mixWith(Color.SPRING_GREEN, -effect)
				.getRGB();
		return Color.WHITE.getRGB();
	}

	public RotatingInstance setup(KineticBlockEntity blockEntity) {
		var blockState = blockEntity.getBlockState();
		var axis = KineticBlockEntityVisual.rotationAxis(blockState);
		return setup(blockEntity, axis, blockEntity.getSpeed());
	}

	public RotatingInstance setup(KineticBlockEntity blockEntity, Axis axis) {
		return setup(blockEntity, axis, blockEntity.getSpeed());
	}

	public RotatingInstance setup(KineticBlockEntity blockEntity, float speed) {
		var blockState = blockEntity.getBlockState();
		var axis = KineticBlockEntityVisual.rotationAxis(blockState);
		return setup(blockEntity, axis, speed);
	}

	public RotatingInstance setup(KineticBlockEntity blockEntity, Axis axis, float speed) {
		var blockState = blockEntity.getBlockState();
		var pos = blockEntity.getBlockPos();
		return setRotationAxis(axis)
			.setRotationalSpeed(speed * SPEED_MULTIPLIER)
			.setRotationOffset(KineticBlockEntityVisual.rotationOffset(blockState, axis, pos)
				+ blockEntity.getRotationAngleOffset(axis))
			.setColor(blockEntity);
	}

	public RotatingInstance rotateToFace(Direction.Axis axis) {
		Direction orientation = Direction.get(Direction.AxisDirection.POSITIVE, axis);
		return rotateToFace(orientation);
	}

	public RotatingInstance rotateToFace(Direction from, Direction.Axis axis) {
		Direction orientation = Direction.get(Direction.AxisDirection.POSITIVE, axis);
		return rotateToFace(from, orientation);
	}

	public RotatingInstance rotateToFace(Direction orientation) {
		return rotateTo(0, 1, 0, orientation.getStepX(), orientation.getStepY(), orientation.getStepZ());
	}

	public RotatingInstance rotateToFace(Direction from, Direction orientation) {
		return rotateTo(from.getStepX(), from.getStepY(), from.getStepZ(), orientation.getStepX(),
			orientation.getStepY(), orientation.getStepZ());
	}

	public RotatingInstance rotateTo(float fromX, float fromY, float fromZ, float toX, float toY, float toZ) {
		rotation.rotateTo(fromX, fromY, fromZ, toX, toY, toZ);
		return this;
	}

	public RotatingInstance setRotationAxis(Direction.Axis axis) {
		Direction orientation = Direction.get(Direction.AxisDirection.POSITIVE, axis);
		return setRotationAxis(orientation.step());
	}

	public RotatingInstance setRotationAxis(Vector3f axis) {
		return setRotationAxis(axis.x(), axis.y(), axis.z());
	}

	public RotatingInstance setRotationAxis(float rotationAxisX, float rotationAxisY, float rotationAxisZ) {
		this.rotationAxisX = (byte) (rotationAxisX * 127);
		this.rotationAxisY = (byte) (rotationAxisY * 127);
		this.rotationAxisZ = (byte) (rotationAxisZ * 127);
		return this;
	}

	public RotatingInstance setPosition(Vec3i pos) {
		return setPosition(pos.getX(), pos.getY(), pos.getZ());
	}

	public RotatingInstance setPosition(Vector3f pos) {
		return setPosition(pos.x(), pos.y(), pos.z());
	}

	public RotatingInstance setPosition(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}

	public RotatingInstance setColor(KineticBlockEntity blockEntity) {
		colorRgb(colorFromBE(blockEntity));
		return this;
	}

	public RotatingInstance setColor(Color color) {
		color(color.getRed(), color.getGreen(), color.getBlue());
		return this;
	}

	public RotatingInstance setRotationalSpeed(float rotationalSpeed) {
		this.rotationalSpeed = rotationalSpeed;
		return this;
	}

	public RotatingInstance setRotationOffset(float rotationOffset) {
		this.rotationOffset = rotationOffset;
		return this;
	}
}
