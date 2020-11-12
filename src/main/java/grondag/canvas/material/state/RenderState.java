/*
 * Copyright 2019, 2020 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package grondag.canvas.material.state;

import com.mojang.blaze3d.systems.RenderSystem;
import grondag.canvas.Configurator;
import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.material.property.BinaryMaterialState;
import grondag.canvas.material.property.MaterialDecal;
import grondag.canvas.material.property.MaterialDepthTest;
import grondag.canvas.material.property.MaterialFog;
import grondag.canvas.material.property.MaterialTarget;
import grondag.canvas.material.property.MaterialTextureState;
import grondag.canvas.material.property.MaterialTransparency;
import grondag.canvas.material.property.MaterialWriteMask;
import grondag.canvas.render.CanvasFrameBufferHacks;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.texture.SpriteInfoTexture;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;

/**
 * Primitives with the same state have the same vertex encoding,
 * same uniform state and same GL draw state. Analogous to RenderLayer<p>
 *
 * Also serves as the key for vertex collection. Primitives with the same state
 * can share the same draw call and should be packed contiguously in the buffer.<p>
 *
 * Primitives must have the same sorting requirements, which for all but the translucent
 * collection keys means there is no sorting. Translucent primitives that require sorting
 * all belong to a small handful of collectors.<p>
 *
 * Vertex data with different state can share the same buffer and should be
 * packed in glState, uniformState order for best performance.
 */
public final class RenderState extends AbstractRenderState {
	protected RenderState(long bits) {
		super(nextIndex++, bits);
	}

	public void enable() {
		if (active == this) {
			return;
		}

		if (active == null) {
			// same for all, so only do 1X
			RenderSystem.shadeModel(GL11.GL_SMOOTH);
			target.enable();
			// NB: must be after frame-buffer target switch
			if (Configurator.enableBloom) CanvasFrameBufferHacks.startExtrasCapture();
		} else if (active.target != target) {
			if (Configurator.enableBloom) CanvasFrameBufferHacks.endExtrasCapture();
			target.enable();
			if (Configurator.enableBloom) CanvasFrameBufferHacks.startExtrasCapture();
		}

		active = this;

		texture.enable(blur);
		transparency.enable();
		depthTest.enable();
		writeMask.enable();
		fog.enable();
		decal.enable();

		CULL_STATE.setEnabled(cull);
		LIGHTMAP_STATE.setEnabled(enableLightmap);
		LINE_STATE.setEnabled(lines);

		shader.activate();
		shader.setAtlasInfo(texture.atlasInfo());
	}

	public void enableWithOrigin(int x, int y, int z) {
		enable();
		shader.setModelOrigin(x, y, z);
	}

	private static final BinaryMaterialState CULL_STATE = new BinaryMaterialState(RenderSystem::enableCull, RenderSystem::disableCull);

	private static final BinaryMaterialState LIGHTMAP_STATE = new BinaryMaterialState(
		() -> MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable(),
		() -> MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable());

	private static final BinaryMaterialState LINE_STATE = new BinaryMaterialState(
		() -> RenderSystem.lineWidth(Math.max(2.5F, MinecraftClient.getInstance().getWindow().getFramebufferWidth() / 1920.0F * 2.5F)),
		() -> RenderSystem.lineWidth(1.0F));

	public static void disable() {
		if (active == null) {
			return;
		}

		active = null;

		// NB: must be before frame-buffer target switch
		if (Configurator.enableBloom) CanvasFrameBufferHacks.endExtrasCapture();

		CanvasVertexFormat.disableDirect();
		GlProgram.deactivate();
		RenderSystem.shadeModel(GL11.GL_FLAT);
		SpriteInfoTexture.disable();
		MaterialDecal.disable();
		MaterialTransparency.disable();
		MaterialDepthTest.disable();
		MaterialWriteMask.disable();
		MaterialFog.disable();
		CULL_STATE.disable();
		LIGHTMAP_STATE.disable();
		LINE_STATE.disable();
		MaterialTextureState.disable();
		RenderSystem.color4f(1f, 1f, 1f, 1f);
		RenderSystem.disableAlphaTest();
		RenderSystem.defaultAlphaFunc();

		MaterialTarget.disable();
	}

	public static final int MAX_COUNT = 4096;
	static int nextIndex = 0;
	static final RenderState[] STATES = new RenderState[MAX_COUNT];
	static final Long2ObjectOpenHashMap<RenderState> MAP = new Long2ObjectOpenHashMap<>(4096, Hash.VERY_FAST_LOAD_FACTOR);

	private static RenderState active = null;

	public static final RenderState MISSING = new RenderState(0);

	static {
		STATES[0] = MISSING;
	}

	public static RenderState fromIndex(int index) {
		return STATES[index];
	}
}
