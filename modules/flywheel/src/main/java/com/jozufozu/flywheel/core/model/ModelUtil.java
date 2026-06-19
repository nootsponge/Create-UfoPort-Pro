package com.jozufozu.flywheel.core.model;

import java.util.Collection;
import java.util.function.Supplier;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class ModelUtil {
	private static final ThreadLocal<ThreadLocalObjects> THREAD_LOCAL_OBJECTS = ThreadLocal.withInitial(ThreadLocalObjects::new);

	public static ShadeSeparatedBufferedData endAndCombine(BufferBuilder shadedBuilder, BufferBuilder unshadedBuilder) {
		MeshData shadedMesh = shadedBuilder.build();
		MeshData unshadedMesh = unshadedBuilder.build();

		try {
			int shadedVertexCount = vertexCount(shadedMesh);
			int unshadedVertexCount = vertexCount(unshadedMesh);
			int vertexCount = shadedVertexCount + unshadedVertexCount;

			MeshData.DrawState drawState = combinedDrawState(shadedMesh, unshadedMesh, vertexCount);
			MeshData combinedMesh = new MeshData(null, drawState);
			ByteBuffer combinedVertices = combineVertices(shadedMesh, unshadedMesh);

			return new ShadeSeparatedBufferedData.NativeImpl(
					combinedVertices,
					BufferUtils.createByteBuffer(0),
					combinedMesh,
					shadedVertexCount);
		} finally {
			if (shadedMesh != null) {
				shadedMesh.close();
			}
			if (unshadedMesh != null) {
				unshadedMesh.close();
			}
		}
	}

	private static int vertexCount(MeshData mesh) {
		return mesh == null ? 0 : mesh.drawState()
				.vertexCount();
	}

	private static MeshData.DrawState combinedDrawState(MeshData shadedMesh, MeshData unshadedMesh, int vertexCount) {
		MeshData.DrawState template = shadedMesh != null ? shadedMesh.drawState() : unshadedMesh != null ? unshadedMesh.drawState() : null;
		VertexFormat format = template != null ? template.format() : DefaultVertexFormat.BLOCK;
		VertexFormat.Mode mode = template != null ? template.mode() : VertexFormat.Mode.QUADS;
		VertexFormat.IndexType indexType = VertexFormat.IndexType.least(mode.indexCount(vertexCount));
		return new MeshData.DrawState(format, vertexCount, mode.indexCount(vertexCount), mode, indexType);
	}

	private static ByteBuffer combineVertices(MeshData shadedMesh, MeshData unshadedMesh) {
		int size = bufferSize(shadedMesh) + bufferSize(unshadedMesh);
		ByteBuffer combined = BufferUtils.createByteBuffer(size);
		appendVertices(combined, shadedMesh);
		appendVertices(combined, unshadedMesh);
		combined.flip();
		return combined;
	}

	private static int bufferSize(MeshData mesh) {
		return mesh == null ? 0 : mesh.vertexBuffer()
				.remaining();
	}

	private static void appendVertices(ByteBuffer target, MeshData mesh) {
		if (mesh == null) {
			return;
		}
		target.put(mesh.vertexBuffer()
				.duplicate());
	}

	public static ShadeSeparatedBufferedData getBufferedData(Bufferable bufferable) {
		ModelBlockRenderer blockRenderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
		ThreadLocalObjects objects = THREAD_LOCAL_OBJECTS.get();

		objects.begin();

		bufferable.bufferInto(objects.shadeSeparatingWrapper, blockRenderer, objects.random);

		return objects.end();
	}

	public static ShadeSeparatedBufferedData getBufferedData(BakedModel model, BlockState referenceState) {
		return new BakedModelBuilder(model).withReferenceState(referenceState)
				.build();
	}

	public static ShadeSeparatedBufferedData getBufferedData(BakedModel model, BlockState referenceState, PoseStack poseStack) {
		return new BakedModelBuilder(model).withReferenceState(referenceState)
				.withPoseStack(poseStack)
				.build();
	}

	public static ShadeSeparatedBufferedData getBufferedData(BlockAndTintGetter renderWorld, BakedModel model, BlockState referenceState, PoseStack poseStack) {
		return new BakedModelBuilder(model).withReferenceState(referenceState)
				.withPoseStack(poseStack)
				.withRenderWorld(renderWorld)
				.build();
	}

	public static ShadeSeparatedBufferedData getBufferedDataFromTemplate(BlockAndTintGetter renderWorld, RenderType layer, Collection<StructureTemplate.StructureBlockInfo> blocks) {
		return new WorldModelBuilder(layer).withRenderWorld(renderWorld)
				.withBlocks(blocks)
				.build();
	}

	public static ShadeSeparatedBufferedData getBufferedDataFromTemplate(BlockAndTintGetter renderWorld, RenderType layer, Collection<StructureTemplate.StructureBlockInfo> blocks, PoseStack poseStack) {
		return new WorldModelBuilder(layer).withRenderWorld(renderWorld)
				.withBlocks(blocks)
				.withPoseStack(poseStack)
				.build();
	}

	public static Supplier<PoseStack> rotateToFace(Direction facing) {
		return () -> {
			PoseStack stack = new PoseStack();
			TransformStack.cast(stack)
					.centre()
					.rotateToFace(facing.getOpposite())
					.unCentre();
			return stack;
		};
	}

	private static class ThreadLocalObjects {
		public final RandomSource random = RandomSource.create();
		public final ShadeSeparatingVertexConsumer shadeSeparatingWrapper = new ShadeSeparatingVertexConsumer();
		public BufferBuilder shadedBuilder = null;
		public BufferBuilder unshadedBuilder = null;
		public ByteBufferBuilder shadedBuilderBuffer = new ByteBufferBuilder(2048);
		public ByteBufferBuilder unshadedBuilderBuffer = new ByteBufferBuilder(2048);

		private void begin() {
			shadedBuilderBuffer.clear();
			unshadedBuilderBuffer.clear();
			this.shadedBuilder = new BufferBuilder(shadedBuilderBuffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
			this.unshadedBuilder = new BufferBuilder(unshadedBuilderBuffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
			this.shadeSeparatingWrapper.prepare(this.shadedBuilder, this.unshadedBuilder);
		}

		private ShadeSeparatedBufferedData end() {
			this.shadeSeparatingWrapper.clear();
			return ModelUtil.endAndCombine(shadedBuilder, unshadedBuilder);
		}
	}
}
