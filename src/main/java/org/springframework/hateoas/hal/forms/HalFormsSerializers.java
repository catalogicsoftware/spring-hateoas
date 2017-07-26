/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.hateoas.hal.forms;

import static org.springframework.hateoas.hal.forms.HalFormsUtils.*;

import java.io.IOException;
import java.util.Map;

import lombok.Data;

import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.hal.Jackson2HalModule;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

/**
 * @author Greg Turnquist
 */
public class HalFormsSerializers {

	static class HalFormsResourceSerializer extends ContainerSerializer<Resource<?>> implements ContextualSerializer {

		private final BeanProperty property;

		HalFormsResourceSerializer(BeanProperty property) {

			super(Resource.class, false);
			this.property = property;
		}

		HalFormsResourceSerializer() {
			this(null);
		}

		@Override
		public void serialize(Resource<?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {

			HalFormsDocument<?> doc = toHalFormsDocument(value);

			provider
				.findValueSerializer(HalFormsDocument.class, property)
				.serialize(doc, gen, provider);
		}

		@Override
		public JavaType getContentType() {
			return null;
		}

		@Override
		public JsonSerializer<?> getContentSerializer() {
			return null;
		}

		@Override
		public boolean hasSingleElement(Resource<?> resource) {
			return false;
		}

		@Override
		protected ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer typeSerializer) {
			return null;
		}

		@Override
		public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
			return new HalFormsResourceSerializer(property);
		}
	}

	static class HalFormsResourcesSerializer extends ContainerSerializer<Resources<?>> implements ContextualSerializer {

		private final BeanProperty property;
		private final Jackson2HalModule.EmbeddedMapper embeddedMapper;

		HalFormsResourcesSerializer(BeanProperty property, Jackson2HalModule.EmbeddedMapper embeddedMapper) {

			super(Resources.class, false);
			this.property = property;
			this.embeddedMapper = embeddedMapper;
		}

		HalFormsResourcesSerializer(Jackson2HalModule.EmbeddedMapper embeddedMapper) {
			this(null, embeddedMapper);
		}

		@Override
		public void serialize(Resources<?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {

			Map<String, Object> embeddeds = embeddedMapper.map(value.getContent());

			HalFormsDocument<?> doc = toHalFormsDocument(embeddeds, value);

			provider
				.findValueSerializer(HalFormsDocument.class, property)
				.serialize(doc, gen, provider);
		}

		@Override
		public JavaType getContentType() {
			return null;
		}

		@Override
		public JsonSerializer<?> getContentSerializer() {
			return null;
		}

		@Override
		public boolean hasSingleElement(Resources<?> resources) {
			return resources.getContent().size() == 1;
		}

		@Override
		protected ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer typeSerializer) {
			return null;
		}

		@Override
		public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
			return new HalFormsResourcesSerializer(property, embeddedMapper);
		}
	}

	@Data
	static class HalFormsEmbeddedWrapper extends ResourceSupport {

		private final Resources<?> value;

		public HalFormsEmbeddedWrapper(Resources<?> value) {
			this.value = value;
		}
	}
}
