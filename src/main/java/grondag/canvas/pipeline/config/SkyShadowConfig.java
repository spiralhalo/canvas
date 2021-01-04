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

package grondag.canvas.pipeline.config;

import blue.endless.jankson.JsonObject;

import grondag.canvas.pipeline.config.util.AbstractConfig;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.NamedDependency;

public class SkyShadowConfig extends AbstractConfig {
	public final NamedDependency<FramebufferConfig> framebuffer;

	SkyShadowConfig (ConfigContext ctx, JsonObject config) {
		super(ctx);
		framebuffer = ctx.frameBuffers.dependOn(config, "framebuffer");
	}

	@Override
	public boolean validate() {
		boolean valid = true;
		valid &= framebuffer.validate("Invalid pipeline config - shadow framebuffer target missing or invalid.");
		return valid;
	}
}