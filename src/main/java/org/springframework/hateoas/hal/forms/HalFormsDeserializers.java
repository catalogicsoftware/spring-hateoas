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

import static org.springframework.hateoas.hal.forms.HalFormsDocument.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.hateoas.Resource;
import org.springframework.hateoas.hal.Jackson2HalModule.HalLinkListDeserializer;
import org.springframework.hateoas.hal.forms.HalFormsDocument.*;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.ContainerDeserializerBase;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * @author Greg Turnquist
 */
public class HalFormsDeserializers {

	static class HalFormsResourceDeserializer extends ContainerDeserializerBase<Resource<?>> implements ContextualDeserializer {

		private JavaType contentType;

		HalFormsResourceDeserializer(JavaType contentType) {

			super(contentType);
			this.contentType = contentType;
		}

		HalFormsResourceDeserializer() {
			this(null);
		}
		
		@Override
		public Resource<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {

			HalFormsDocument doc = p.getCodec().readValue(p, HalFormsDocument.class);
			
			return new Resource<Object>(doc.getResource(), doc.getLinks());
		}

		@Override
		public JavaType getContentType() {
			return null;
		}

		@Override
		public JsonDeserializer<Object> getContentDeserializer() {
			return null;
		}

		@Override
		public JsonDeserializer<?> createContextual(DeserializationContext deserializationContext, BeanProperty property) throws JsonMappingException {

			JavaType vc = property.getType().getContentType();
			HalFormsResourceDeserializer des = new HalFormsResourceDeserializer(vc);
			return des;
		}
	}

	static class HalFormsResourcesDeserializer extends ContainerDeserializerBase<List<Object>> implements ContextualDeserializer {

		private JavaType contentType;

		HalFormsResourcesDeserializer(JavaType contentType) {

			super(contentType);
			this.contentType = contentType;
		}

		HalFormsResourcesDeserializer() {
			this(TypeFactory.defaultInstance().constructCollectionLikeType(List.class, Object.class));
		}

		@Override
		public List<Object> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

			List<Object> result = new ArrayList<Object>();
			JsonDeserializer<Object> deser = ctxt.findRootValueDeserializer(contentType);
			Object object;

			// links is an object, so we parse till we find its end.
			while (!JsonToken.END_OBJECT.equals(jp.nextToken())) {

				if (!JsonToken.FIELD_NAME.equals(jp.getCurrentToken())) {
					throw new JsonParseException("Expected relation name", jp.getCurrentLocation());
				}

				if (JsonToken.START_ARRAY.equals(jp.nextToken())) {
					while (!JsonToken.END_ARRAY.equals(jp.nextToken())) {
						object = deser.deserialize(jp, ctxt);
						result.add(object);
					}
				} else {
					object = deser.deserialize(jp, ctxt);
					result.add(object);
				}
			}

			return result;
		}

		@Override
		public JavaType getContentType() {
			return this.contentType;
		}

		@Override
		public JsonDeserializer<Object> getContentDeserializer() {
			return null;
		}

		@Override
		public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {

			if (property != null) {
				JavaType vc = property.getType().getContentType();
				return new HalFormsResourcesDeserializer(vc);
			} else {
				return new HalFormsResourcesDeserializer(ctxt.getContextualType());
			}
		}
	}

	/**
	 * Deserialize an entire <a href="https://rwcbook.github.io/hal-forms/">HAL-Forms</a> document.
	 */
	static class HalFormsDocumentDeserializer extends JsonDeserializer<HalFormsDocument<?>> implements ContextualDeserializer {

		private final HalLinkListDeserializer linkDeser = new HalLinkListDeserializer();

		private JavaType contentType;

		HalFormsDocumentDeserializer(JavaType contentType) {
			this.contentType = contentType;
		}

		HalFormsDocumentDeserializer() {
			this(null);
		}

		@Override
		public HalFormsDocument deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException {

			HalFormsDocumentBuilder halFormsDocumentBuilder = halFormsDocument();

			while (!JsonToken.END_OBJECT.equals(jp.nextToken())) {

				if (!JsonToken.FIELD_NAME.equals(jp.getCurrentToken())) {
					throw new JsonParseException(jp, "Expected property ", jp.getCurrentLocation());
				}

				jp.nextToken();

				if ("_links".equals(jp.getCurrentName())) {
					halFormsDocumentBuilder.links(this.linkDeser.deserialize(jp, ctxt));
				} else if ("_templates".equals(jp.getCurrentName())) {
					TypeReference<Map<String, Template>> type = new TypeReference<Map<String, Template>>() {};
					halFormsDocumentBuilder.templates(jp.getCodec().<Map<? extends String, ? extends Template>> readValue(jp, type));
				} else if ("_embedded".equals(jp.getCurrentName())) {

					ObjectMapper mapper = (ObjectMapper) jp.getCodec();
					JavaType targetType = mapper.getTypeFactory().constructParametricType(
						Map.class,
						mapper.getTypeFactory().constructSimpleType(String.class, new JavaType[0]),
						this.contentType);

					Object resources = mapper.readValue(jp, targetType);

					System.out.println(resources);
//					halFormsDocumentBuilder.
				}
			}

			return halFormsDocumentBuilder.build();
		}

		/**
		 * Method called to see if a different (or differently configured) deserializer
		 * is needed to deserialize values of specified property.
		 * Note that instance that this method is called on is typically shared one and
		 * as a result method should <b>NOT</b> modify this instance but rather construct
		 * and return a new instance. This instance should only be returned as-is, in case
		 * it is already suitable for use.
		 *
		 * @param ctxt Deserialization context to access configuration, additional
		 * deserializers that may be needed by this deserializer
		 * @param property Method, field or constructor parameter that represents the property
		 * (and is used to assign deserialized value).
		 * Should be available; but there may be cases where caller can not provide it and
		 * null is passed instead (in which case impls usually pass 'this' deserializer as is)
		 * @return Deserializer to use for deserializing values of specified property;
		 * may be this instance or a new instance.
		 * @throws JsonMappingException
		 */
		@Override
		public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {

			if (property != null) {
				JavaType vc = property.getType().getContentType();
				return new HalFormsDocumentDeserializer(vc);
			} else {
				return new HalFormsDocumentDeserializer(ctxt.getContextualType());
			}
		}
	}

	/**
	 * Deserialize an object of HAL-Forms {@link Template}s into a {@link List} of {@link Template}s.
	 */
	static class HalFormsTemplateListDeserializer extends ContainerDeserializerBase<Map<String, Template>> {

		public HalFormsTemplateListDeserializer() {
			super(TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Template.class));
		}

		/**
		 * Accessor for declared type of contained value elements; either exact
		 * type, or one of its supertypes.
		 */
		@Override
		public JavaType getContentType() {
			return null;
		}

		/**
		 * Accesor for deserializer use for deserializing content values.
		 */
		@Override
		public JsonDeserializer<Object> getContentDeserializer() {
			return null;
		}

		/**
		 * Method that can be called to ask implementation to deserialize
		 * JSON content into the value type this serializer handles.
		 * Returned instance is to be constructed by method itself.
		 * <p>
		 * Pre-condition for this method is that the parser points to the
		 * first event that is part of value to deserializer (and which
		 * is never JSON 'null' literal, more on this below): for simple
		 * types it may be the only value; and for structured types the
		 * Object start marker or a FIELD_NAME.
		 * </p>
		 * The two possible input conditions for structured types result
		 * from polymorphism via fields. In the ordinary case, Jackson
		 * calls this method when it has encountered an OBJECT_START,
		 * and the method implementation must advance to the next token to
		 * see the first field name. If the application configures
		 * polymorphism via a field, then the object looks like the following.
		 * <pre>
		 *      {
		 *          "@class": "class name",
		 *          ...
		 *      }
		 *  </pre>
		 * Jackson consumes the two tokens (the <tt>@class</tt> field name
		 * and its value) in order to learn the class and select the deserializer.
		 * Thus, the stream is pointing to the FIELD_NAME for the first field
		 * after the @class. Thus, if you want your method to work correctly
		 * both with and without polymorphism, you must begin your method with:
		 * <pre>
		 *       if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
		 *         jp.nextToken();
		 *       }
		 *  </pre>
		 * This results in the stream pointing to the field name, so that
		 * the two conditions align.
		 * Post-condition is that the parser will point to the last
		 * event that is part of deserialized value (or in case deserialization
		 * fails, event that was not recognized or usable, which may be
		 * the same event as the one it pointed to upon call).
		 * Note that this method is never called for JSON null literal,
		 * and thus deserializers need (and should) not check for it.
		 *
		 * @param jp Parsed used for reading JSON content
		 * @param ctxt Context that can be used to access information about
		 * this deserialization activity.
		 * @return Deserialized value
		 */
		@Override
		public Map<String, Template> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

			Map<String, Template> result = new HashMap<String, Template>();
			String relation;
			Template template;

			// links is an object, so we parse till we find its end.
			while (!JsonToken.END_OBJECT.equals(jp.nextToken())) {

				if (!JsonToken.FIELD_NAME.equals(jp.getCurrentToken())) {
					throw new JsonParseException(jp, "Expected relation name", jp.getCurrentLocation());
				}

				// save the relation in case the link does not contain it
				relation = jp.getText();

				if (JsonToken.START_OBJECT.equals(jp.nextToken())) {
					while (!JsonToken.END_OBJECT.equals(jp.nextToken())) {
						template = jp.readValueAs(Template.class);
						template.setKey(relation);
						result.put(relation, template);
					}
				} else {
					template = jp.readValueAs(Template.class);
					template.setKey(relation);
					result.put(relation, template);
				}
			}

			return result;
		}
	}

	/**
	 * Deserialize a {@link MediaType} embedded inside a HAL-Forms document.
	 */
	static class MediaTypesDeserializer extends ContainerDeserializerBase<List<MediaType>> {

		private static final long serialVersionUID = -7218376603548438390L;

		public MediaTypesDeserializer() {
			super(TypeFactory.defaultInstance().constructCollectionLikeType(List.class, MediaType.class));
		}

		/*
		 * (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.deser.std.ContainerDeserializerBase#getContentType()
		 */
		@Override
		public JavaType getContentType() {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.deser.std.ContainerDeserializerBase#getContentDeserializer()
		 */
		@Override
		public JsonDeserializer<Object> getContentDeserializer() {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
		 */
		@Override
		public List<MediaType> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			return MediaType.parseMediaTypes(p.getText());
		}
	}
}
