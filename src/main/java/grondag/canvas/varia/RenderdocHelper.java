/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package grondag.canvas.varia;

import java.util.function.Predicate;

import org.apache.logging.log4j.Logger;

import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.Minecraft;

import grondag.canvas.CanvasMod;
import grondag.canvas.mixinterface.MinecraftExt;

/**
 * Primitive renderdoc capture helper without using JNA and renderdoc API.
 * It ain't much but it's honest work.
 */
public class RenderdocHelper {
	private static final Logger LOG = CanvasMod.LOG;
	private static int captureId = 0;

	public static void startCapture(Predicate<Integer> condition) {
		if (condition == null) {
			throw new IllegalStateException("Renderdoc capture condition can't be null.");
		}

		if (!condition.test(captureId ++)) {
			return;
		}

		final Minecraft minecraft = Minecraft.getInstance();

		if (minecraft == null) {
			LOG.warn("Failed to initiate renderdoc capture because Minecraft is null.");
			return;
		}

		final Window window = ((MinecraftExt) minecraft).canvas_getWindow();

		if (window == null) {
			LOG.warn("Failed to initiate renderdoc capture because window is null.");
			return;
		}

		// Breakpoint here. Attach renderdoc to running instance.
		System.out.println("Preparation breakpoint.");

		// Breakpoint here. Do a "capture immediately."
		System.out.println("Capture breakpoint.");

		// Renderdoc captures between two buffer swaps, so this is necessary.
		window.updateDisplay();
	}
}
