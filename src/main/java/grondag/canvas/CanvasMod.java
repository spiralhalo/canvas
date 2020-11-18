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

package grondag.canvas;

import grondag.canvas.apiimpl.Canvas;
import grondag.canvas.apiimpl.fluid.FluidHandler;
import grondag.canvas.mixinterface.RenderLayerExt;
import grondag.frex.api.fluid.FluidQuadSupplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.Configuration;

import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.render.RenderLayer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.InvalidateRenderStateCallback;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

//FEAT: item rendering
//FEAT: weather rendering
//FEAT: sky rendering
//FEAT: pbr textures
//PERF: disable animated textures when not in view
//PERF: improve light smoothing performance
//FEAT: colored lights
//FEAT: per chunk occlusion mesh - for sky shadow mask
//FEAT: per chunk depth mesh - addendum to occlusion mesh to render for depth pass - includes translucent cutout
//FEAT: first person dynamic light
//FEAT: weather uniforms
//FEAT: biome texture in shader

public class CanvasMod implements ClientModInitializer {
	public static final String MODID = "canvas";
	public static final Logger LOG = LogManager.getLogger("Canvas");
	public static KeyBinding VIEW_KEY = new KeyBinding("key.canvas.cycle_view", Character.valueOf('`'), "key.canvas.category");
	public static KeyBinding DECREMENT_A = new KeyBinding("key.canvas.decrement_a", Character.valueOf('-'), "key.canvas.category");
	public static KeyBinding INCREMENT_A = new KeyBinding("key.canvas.increment_a", Character.valueOf('='), "key.canvas.category");
	public static KeyBinding DECREMENT_B = new KeyBinding("key.canvas.decrement_b", Character.valueOf('['), "key.canvas.category");
	public static KeyBinding INCREMENT_B = new KeyBinding("key.canvas.increment_b", Character.valueOf(']'), "key.canvas.category");

	@Override
	public void onInitializeClient() {
		Configurator.init();
		RendererAccess.INSTANCE.registerRenderer(Canvas.INSTANCE);
		FluidQuadSupplier.setReloadHandler(FluidHandler.HANDLER);
		InvalidateRenderStateCallback.EVENT.register(Canvas.INSTANCE::reload);

		if (Configurator.debugNativeMemoryAllocation) {
			LOG.warn("Canvas is configured to enable native memory debug. This WILL cause slow performance and other issues.  Debug output will print at game exit.");
			Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
		}

		((RenderLayerExt) RenderLayer.getTranslucent()).canvas_blendMode(BlendMode.TRANSLUCENT);
		((RenderLayerExt) RenderLayer.getTripwire()).canvas_blendMode(BlendMode.TRANSLUCENT);
		((RenderLayerExt) RenderLayer.getSolid()).canvas_blendMode(BlendMode.SOLID);
		((RenderLayerExt) RenderLayer.getCutout()).canvas_blendMode(BlendMode.CUTOUT);
		((RenderLayerExt) RenderLayer.getCutoutMipped()).canvas_blendMode(BlendMode.CUTOUT_MIPPED);

		KeyBindingHelper.registerKeyBinding(VIEW_KEY);
		KeyBindingHelper.registerKeyBinding(DECREMENT_A);
		KeyBindingHelper.registerKeyBinding(INCREMENT_A);
		KeyBindingHelper.registerKeyBinding(DECREMENT_B);
		KeyBindingHelper.registerKeyBinding(INCREMENT_B);
	}
}
