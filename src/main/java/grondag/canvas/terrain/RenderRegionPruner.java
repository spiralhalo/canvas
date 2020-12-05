/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.terrain;

import java.util.function.Predicate;

import net.minecraft.util.math.BlockPos;

import grondag.canvas.terrain.occlusion.TerrainOccluder;

public class RenderRegionPruner implements Predicate<BuiltRenderRegion> {
	private boolean invalidateOccluder = false;
	private int occluderVersion = 0;
	private int cameraChunkX;
	private int cameraChunkY;
	private int cameraChunkZ;
	private int maxSquaredChunkDistance;

	public void prepare(TerrainOccluder occluder, long cameraChunkOrigin) {
		invalidateOccluder = false;
		cameraChunkX = BlockPos.unpackLongX(cameraChunkOrigin) >> 4;
		cameraChunkY = BlockPos.unpackLongY(cameraChunkOrigin) >> 4;
		cameraChunkZ = BlockPos.unpackLongZ(cameraChunkOrigin) >> 4;
		occluderVersion = occluder.version();
		maxSquaredChunkDistance = occluder.maxSquaredChunkDistance();
	}

	public int occluderVersion() {
		return occluderVersion;
	}

	public int cameraChunkX() {
		return cameraChunkX;
	}

	public int cameraChunkY() {
		return cameraChunkY;
	}

	public int cameraChunkZ() {
		return cameraChunkZ;
	}

	public boolean didInvalidateOccluder() {
		return invalidateOccluder;
	}

	public void invalidateOccluder() {
		invalidateOccluder = true;
	}

	@Override
	public boolean test(BuiltRenderRegion r) {
		if (!r.updateCameraDistance(this)) {
			r.close();
			return true;
		} else {
			return false;
		}
	}

	public int maxSquaredChunkDistance() {
		return maxSquaredChunkDistance;
	}
}
