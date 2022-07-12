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

package grondag.canvas.pipeline.config.util;

import java.util.Map;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import grondag.canvas.pipeline.config.option.BooleanConfigEntry;
import grondag.canvas.pipeline.config.option.FloatConfigEntry;
import grondag.canvas.pipeline.config.option.IntConfigEntry;

public class DynamicPropertyMap {
	private final ConfigContext ctx;

	public final Map<String, Dynamic<Integer>> dynamicInts = new Object2ObjectOpenHashMap<>();
	public final Map<String, Dynamic<Float>> dynamicFloats = new Object2ObjectOpenHashMap<>();
	public final Map<String, Dynamic<Boolean>> dynamicBooleans = new Object2ObjectOpenHashMap<>();

	DynamicPropertyMap(ConfigContext context) {
		ctx = context;
	}

	public boolean getBoolean(JsonObject config, String key, boolean defaultVal) {
		if (dynamicBooleans.containsKey(key)) {
			return dynamicBooleans.get(key).value();
		}

		final Dynamic<Boolean> resolver = deserialize(Boolean.class, config, key, defaultVal);

		if (resolver != null) {
			dynamicBooleans.put(key, resolver);
		}

		return defaultVal;
	}

	public int getInt(JsonObject config, String key, int defaultVal) {
		if (dynamicInts.containsKey(key)) {
			return dynamicInts.get(key).value();
		}

		final Dynamic<Integer> resolver = deserialize(Integer.class, config, key, defaultVal);

		if (resolver != null) {
			dynamicInts.put(key, resolver);
		}

		return defaultVal;
	}

	public float getFloat (JsonObject config, String key, float defaultVal) {
		if (dynamicFloats.containsKey(key)) {
			return dynamicFloats.get(key).value();
		}

		final Dynamic<Float> resolver = deserialize(Float.class, config, key, defaultVal);

		if (resolver != null) {
			dynamicFloats.put(key, resolver);
		}

		return defaultVal;
	}

	private <ForType> Dynamic<ForType> deserialize(Class<ForType> clazz, JsonObject config, String key, ForType defaultVal) {
		final JsonElement element = config.get(key);

		if (element instanceof JsonPrimitive primitive) {
			if (primitive.getValue() instanceof CharSequence stringVal) {
				return createResolver(clazz, stringVal.toString());
			}
		} else if (element instanceof JsonObject obj) {
			return deserializeMap(clazz, key, obj, defaultVal);
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private <ForType> Dynamic<ForType> createResolver(Class<ForType> clazz, String optionKey) {
		if (clazz.equals(Boolean.class)) {
			return (Dynamic<ForType>) new BooleanResolver(optionKey);
		}

		if (clazz.equals(Integer.class)) {
			return (Dynamic<ForType>) new IntResolver(optionKey);
		}

		if (clazz.equals(Float.class)) {
			return (Dynamic<ForType>) new FloatResolver(optionKey);
		}

		return null;
	}

	private <ForType> MapResolver<ForType> deserializeMap(Class<ForType> clazz, String key, JsonObject obj, ForType defaultVal) {
		final ForType suppliedDefault = obj.get(clazz, "default");
		final JsonObject optionMap = obj.getObject("optionMap");

		if (optionMap != null) {
			final String optionKey = optionMap.get(String.class, "option");
			final JsonArray jsonMap = optionMap.get(JsonArray.class, "map");

			if (optionKey != null && jsonMap != null) {
				return new MapResolver<>(clazz, optionKey, jsonMap, suppliedDefault == null ? defaultVal : suppliedDefault);
			}
		}

		return null;
	}

	private static abstract class Dynamic<T> {
		public abstract T value();
	}

	private class MapResolver<ToType> extends Dynamic<ToType> {
		private final Dynamic<Object> source;
		private final Map<Object, ToType> map;
		private final ToType defaultVal;

		private MapResolver(Class<ToType> toType, String optionKey, JsonArray jsonMap, ToType defaultVal) {
			this.source = new WildcardResolver(optionKey);
			this.defaultVal = defaultVal;
			map = new Object2ObjectOpenHashMap<>(jsonMap.size());

			for (JsonElement element : jsonMap) {
				if (element instanceof JsonObject obj) {
					final var condition = obj.get(Object.class, "condition");
					final var value = obj.get(toType, "value");

					if (condition != null && value != null) {
						map.put(condition, value);
					}
				}
			}
		}

		@Override
		public ToType value() {
			return map.getOrDefault(source.value(), defaultVal);
		}
	}

	private class WildcardResolver extends Dynamic<Object> {
		private final String optionKey;

		private WildcardResolver(String optionKey) {
			this.optionKey = optionKey;
		}

		@Override
		public Object value() {
			var enumOption = ctx.enumConfigEntries.dependOn(optionKey);

			if (enumOption != null) {
				return enumOption.value();
			}

			var booleanOption = ctx.booleanConfigEntries.dependOn(optionKey);

			if (booleanOption != null) {
				return booleanOption.value();
			}

			var intOption = ctx.intConfigEntries.dependOn(optionKey);

			if (intOption != null) {
				return intOption.value();
			}

			var floatOption = ctx.floatConfigEntries.dependOn(optionKey);

			if (floatOption != null) {
				return floatOption.value();
			}

			return null;
		}
	}

	private class BooleanResolver extends Dynamic<Boolean> {
		private final NamedDependency<BooleanConfigEntry> optionEntry;

		private BooleanResolver(String option) {
			optionEntry = ctx.booleanConfigEntries.dependOn(option);
		}

		@Override
		public Boolean value() {
			final var option = optionEntry.value();

			if (option == null) {
				return null;
			} else {
				return option.value();
			}
		}
	}

	private class IntResolver extends Dynamic<Integer> {
		private final NamedDependency<IntConfigEntry> optionEntry;

		private IntResolver(String option) {
			optionEntry = ctx.intConfigEntries.dependOn(option);
		}

		@Override
		public Integer value() {
			final var option = optionEntry.value();

			if (option == null) {
				return null;
			} else {
				return option.value();
			}
		}
	}

	private class FloatResolver extends Dynamic<Float> {
		private final NamedDependency<FloatConfigEntry> optionEntry;

		private FloatResolver(String option) {
			optionEntry = ctx.floatConfigEntries.dependOn(option);
		}

		@Override
		public Float value() {
			final var option = optionEntry.value();

			if (option == null) {
				return null;
			} else {
				return option.value();
			}
		}
	}
}
