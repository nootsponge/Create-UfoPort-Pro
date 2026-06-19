package com.jozufozu.flywheel.util.transform;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;

public interface TransformStack extends Transform<TransformStack>, TStack<TransformStack> {
	static TransformStack cast(PoseStack stack) {
		if (stack instanceof TransformStack transformStack) {
			return transformStack;
		}
		return new PoseStackAdapter(stack);
	}

	final class PoseStackAdapter implements TransformStack {
		private final PoseStack stack;

		private PoseStackAdapter(PoseStack stack) {
			this.stack = stack;
		}

		@Override
		public TransformStack mulPose(Matrix4f pose) {
			stack.mulPose(pose);
			return this;
		}

		@Override
		public TransformStack mulNormal(Matrix3f normal) {
			stack.last()
					.normal()
					.mul(normal);
			return this;
		}

		@Override
		public TransformStack multiply(Quaternionf quaternion) {
			stack.mulPose(quaternion);
			return this;
		}

		@Override
		public TransformStack scale(float factorX, float factorY, float factorZ) {
			stack.scale(factorX, factorY, factorZ);
			return this;
		}

		@Override
		public TransformStack pushPose() {
			stack.pushPose();
			return this;
		}

		@Override
		public TransformStack popPose() {
			stack.popPose();
			return this;
		}

		@Override
		public TransformStack translate(double x, double y, double z) {
			stack.translate(x, y, z);
			return this;
		}
	}
}
