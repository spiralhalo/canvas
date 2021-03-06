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

package grondag.canvas.terrain.region;

import it.unimi.dsi.fastutil.Hash;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import grondag.canvas.render.CanvasWorldRenderer;
import grondag.canvas.terrain.util.HackedLong2ObjectMap;

public class RenderRegionStorage {
	public final RenderRegionPruner regionPruner;

	// Hat tip to JellySquid for the suggestion of using a hashmap
	// PERF: lock-free implementation
	private final HackedLong2ObjectMap<BuiltRenderRegion> regionMap = new HackedLong2ObjectMap<BuiltRenderRegion>(8192, Hash.VERY_FAST_LOAD_FACTOR, r -> r.close()) {
		@Override
		protected boolean shouldPrune(BuiltRenderRegion region) {
			return region.shouldPrune();
		}
	};

	private final HackedLong2ObjectMap<RegionChunkReference> chunkRefMap = new HackedLong2ObjectMap<RegionChunkReference>(2048, Hash.VERY_FAST_LOAD_FACTOR, r -> { }) {
		@Override
		protected boolean shouldPrune(RegionChunkReference item) {
			return item.isEmpty();
		}
	};

	private final CanvasWorldRenderer cwr;

	public RenderRegionStorage(CanvasWorldRenderer cwr, RenderRegionPruner regionPruner) {
		this.cwr = cwr;
		this.regionPruner = regionPruner;
	}

	private RegionChunkReference chunkRef(long packedOriginPos) {
		final long key = ChunkPos.toLong(BlockPos.unpackLongX(packedOriginPos) >> 4, BlockPos.unpackLongZ(packedOriginPos) >> 4);
		return chunkRefMap.computeIfAbsent(key, k -> new RegionChunkReference(cwr.getWorld(), key));
	}

	public void clear() {
		regionMap.clear();
		chunkRefMap.clear();
	}

	public void scheduleRebuild(int x, int y, int z, boolean urgent) {
		if ((y & 0xFFFFFF00) == 0) {
			final BuiltRenderRegion region = regionMap.get(BlockPos.asLong(x & 0xFFFFFFF0, y & 0xFFFFFFF0, z & 0xFFFFFFF0));

			if (region != null) {
				region.markForBuild(urgent);
			}
		}
	}

	public void updateCameraDistanceAndVisibilityInfo(long cameraChunkOrigin) {
		regionPruner.prepare(cameraChunkOrigin);
		regionMap.prune();
		chunkRefMap.prune();

		if (regionPruner.didInvalidateOccluder()) {
			regionPruner.occluder.invalidate();
		}

		regionPruner.post();
	}

	public int regionCount() {
		return regionMap.size();
	}

	private BuiltRenderRegion getOrCreateRegion(long packedOriginPos) {
		return regionMap.computeIfAbsent(packedOriginPos, k -> {
			return new BuiltRenderRegion(cwr, this, chunkRef(k), k);
		});
	}

	public BuiltRenderRegion getOrCreateRegion(int x, int y, int z) {
		return getOrCreateRegion(BlockPos.asLong(x & 0xFFFFFFF0, y & 0xFFFFFFF0, z & 0xFFFFFFF0));
	}

	public BuiltRenderRegion getOrCreateRegion(BlockPos pos) {
		return getOrCreateRegion(pos.getX(), pos.getY(), pos.getZ());
	}

	public BuiltRenderRegion getRegionIfExists(BlockPos pos) {
		return getRegionIfExists(pos.getX(), pos.getY(), pos.getZ());
	}

	public BuiltRenderRegion getRegionIfExists(int x, int y, int z) {
		return regionMap.get(BlockPos.asLong(x & 0xFFFFFFF0, y & 0xFFFFFFF0, z & 0xFFFFFFF0));
	}

	public boolean wasSeen(int x, int y, int z) {
		final BuiltRenderRegion r = getRegionIfExists(x, y, z);
		return r != null && r.wasRecentlySeen();
	}
}
