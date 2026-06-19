package com.simibubi.create.foundation.data;

import java.util.function.BiFunction;
import java.util.function.Predicate;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderRegistry;
import com.jozufozu.flywheel.backend.instancing.entity.EntityInstance;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.builders.EntityBuilder;
import com.tterrag.registrate.fabric.EnvExecutor;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import dev.engine_room.flywheel.lib.visualization.SimpleEntityVisualizer;
import net.fabricmc.api.EnvType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

@ParametersAreNonnullByDefault
public class CreateEntityBuilder<T extends Entity, P> extends EntityBuilder<T, P> {

	@Nullable
	private NonNullSupplier<BiFunction<MaterialManager, T, EntityInstance<? super T>>> instanceFactory;
	private Predicate<T> instanceRenderNormally;
	@Nullable
	private NonNullSupplier<SimpleEntityVisualizer.Factory<T>> visualFactory;
	private Predicate<T> visualRenderNormally;

	public static <T extends Entity, P> EntityBuilder<T, P> create(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, EntityType.EntityFactory<T> factory, MobCategory classification) {
		return (new CreateEntityBuilder<>(owner, parent, name, callback, factory, classification)).defaultLang();
	}

	public CreateEntityBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, EntityType.EntityFactory<T> factory, MobCategory classification) {
		super(owner, parent, name, callback, factory, classification/*, (mobCategory, tEntityFactory) -> FabricEntityTypeBuilder.create(mobCategory, tEntityFactory)*/);
	}

	public CreateEntityBuilder<T, P> instance(NonNullSupplier<BiFunction<MaterialManager, T, EntityInstance<? super T>>> instanceFactory) {
		return instance(instanceFactory, true);
	}

	public CreateEntityBuilder<T, P> instance(NonNullSupplier<BiFunction<MaterialManager, T, EntityInstance<? super T>>> instanceFactory, boolean renderNormally) {
		return instance(instanceFactory, be -> renderNormally);
	}

	public CreateEntityBuilder<T, P> instance(NonNullSupplier<BiFunction<MaterialManager, T, EntityInstance<? super T>>> instanceFactory, Predicate<T> renderNormally) {
		if (this.instanceFactory == null) {
			EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::registerInstance);
		}

		this.instanceFactory = instanceFactory;
		this.instanceRenderNormally = renderNormally;

		return this;
	}

	protected void registerInstance() {
		onRegister(entry ->
				InstancedRenderRegistry.configure(entry)
						.factory(instanceFactory.get())
						.skipRender(be -> !instanceRenderNormally.test(be))
						.apply()
		);
	}

	public CreateEntityBuilder<T, P> visual(NonNullSupplier<SimpleEntityVisualizer.Factory<T>> visualFactory) {
		return visual(visualFactory, true);
	}

	public CreateEntityBuilder<T, P> visual(NonNullSupplier<SimpleEntityVisualizer.Factory<T>> visualFactory, boolean renderNormally) {
		return visual(visualFactory, entity -> renderNormally);
	}

	public CreateEntityBuilder<T, P> visual(NonNullSupplier<SimpleEntityVisualizer.Factory<T>> visualFactory, Predicate<T> renderNormally) {
		if (this.visualFactory == null) {
			EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::registerVisualizer);
		}

		this.visualFactory = visualFactory;
		this.visualRenderNormally = renderNormally;

		return this;
	}

	protected void registerVisualizer() {
		onRegister(entry -> {
			var visualFactory = this.visualFactory;
			if (visualFactory != null) {
				SimpleEntityVisualizer.builder(entry)
					.factory(visualFactory.get())
					.skipVanillaRender(entity -> !visualRenderNormally.test(entity))
					.apply();
			}
		});
	}

}
