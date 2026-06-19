package com.jozufozu.flywheel.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

public class StitchedSprite {
	private static final Map<ResourceLocation, List<StitchedSprite>> ALL = new HashMap<>();
	private static final Map<ResourceLocation, TextureAtlas> LOADED_ATLASES = new HashMap<>();

	protected final ResourceLocation atlasLocation;
	protected final ResourceLocation location;
	protected TextureAtlasSprite sprite;

	public StitchedSprite(ResourceLocation atlas, ResourceLocation location) {
		atlasLocation = atlas;
		this.location = location;
		ALL.computeIfAbsent(atlasLocation, $ -> new ArrayList<>()).add(this);

		TextureAtlas atlasInstance = LOADED_ATLASES.get(atlasLocation);
		if (atlasInstance != null)
			loadSprite(atlasInstance);
	}

	public StitchedSprite(ResourceLocation location) {
		this(InventoryMenu.BLOCK_ATLAS, location);
	}

	public static void onTextureStitchPost(TextureAtlas atlas) {
		ResourceLocation atlasLocation = atlas.location();
		LOADED_ATLASES.put(atlasLocation, atlas);
		List<StitchedSprite> sprites = ALL.get(atlasLocation);
		if (sprites != null) {
			for (StitchedSprite sprite : new ArrayList<>(sprites)) {
				sprite.loadSprite(atlas);
			}
		}
	}

	protected void loadSprite(TextureAtlas atlas) {
		sprite = atlas.getSprite(location);
	}

	public ResourceLocation getAtlasLocation() {
		return atlasLocation;
	}

	public ResourceLocation getLocation() {
		return location;
	}

	public TextureAtlasSprite get() {
		return sprite;
	}
}
