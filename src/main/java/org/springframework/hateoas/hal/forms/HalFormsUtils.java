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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.hateoas.Affordance;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpMethod;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Greg Turnquist
 */
final class HalFormsUtils {

	public static HalFormsDocument toHalFormsDocument(Object object, ObjectMapper objectMapper) {

		if (object == null) {
			return null;
		}

		if (object instanceof HalFormsDocument) {
			return (HalFormsDocument) object;
		}

		if (object instanceof Resources) {
			return toHalFormsDocument((Resources<?>) object);
		} else if (object instanceof Resource) {
			return toHalFormsDocument((Resource<?>) object);
		} else if (object instanceof ResourceSupport) {
			return toHalFormsDocument((ResourceSupport) object);
		} else { // bean
			throw new RuntimeException("Don't know how to convert a " + object.getClass().getSimpleName() +
				" to " + HalFormsDocument.class.getSimpleName());
		}
	}

	private static HalFormsDocument toHalFormsDocument(Resources<?> resources) {

		Map<String, Template> templates = new HashMap<String, Template>();

		if (resources.hasLink(Link.REL_SELF)) {
			for (Affordance affordance : resources.getLink(Link.REL_SELF).getAffordances()) {

				validate(resources, affordance);

				Template template = new Template();
				template.setHttpMethod(HttpMethod.valueOf(affordance.getVerb()));

				List<Property> properties = new ArrayList<Property>();

				for (Map.Entry<String, Class<?>> entry : affordance.getProperties().entrySet()) {
					properties.add(new Property(entry.getKey(), null, null, null, null, false, affordance.isRequired(), false));
				}

				template.setProperties(properties);

				if (templates.isEmpty()) {
					templates.put("default", template);
				} else {
					templates.put(affordance.getMethodName(), template);
				}
			}
		}

		Object content;

		/**
		 * Resources in the Jackson2HalForms module are converted into a Map and inserted into a single element collection.
		 */
		if (resources.getContent().size() == 1 && resources.getContent().iterator().next() instanceof Map) {
			content = resources.getContent().iterator().next();
		} else {
			content = resources.getContent();
		}

		return HalFormsDocument.halFormsDocument()
			.resources(content)
			.links(resources.getLinks())
			.templates(templates)
			.build();
	}

	/**
	 * Transform a {@link Resource} into a {@link HalFormsDocument}.
	 *
	 * @param resource
	 * @return
	 */
	private static HalFormsDocument toHalFormsDocument(Resource<?> resource) {

		Map<String, Template> templates = new HashMap<String, Template>();

		if (resource.hasLink(Link.REL_SELF)) {
			for (Affordance affordance : resource.getLink(Link.REL_SELF).getAffordances()) {

				validate(resource, affordance);

				Template template = new Template();
				template.setHttpMethod(HttpMethod.valueOf(affordance.getVerb()));

				List<Property> properties = new ArrayList<Property>();

				for (Map.Entry<String, Class<?>> entry : affordance.getProperties().entrySet()) {
					properties.add(new Property(entry.getKey(), null, null, null, null, false, affordance.isRequired(), false));
				}

				template.setProperties(properties);

				if (templates.isEmpty()) {
					templates.put("default", template);
				} else {
					templates.put(affordance.getMethodName(), template);
				}
			}
		}

		return HalFormsDocument.halFormsDocument()
			.resource(resource.getContent())
			.links(resource.getLinks())
			.templates(templates)
			.build();
	}

	/**
	 * Transform a {@link ResourceSupport} into a {@link HalFormsDocument}.
	 *
	 * @param rs
	 * @return
	 */
	private static HalFormsDocument toHalFormsDocument(ResourceSupport rs) {

		Map<String, Object> content = new HashMap<String, Object>();

		Set<String> propertiesToIgnore = new HashSet<String>();
		propertiesToIgnore.add("class");
		propertiesToIgnore.add("id");
		propertiesToIgnore.add("links");

		for (PropertyDescriptor descriptor : getPropertyDescriptors(rs)) {
			if (!propertiesToIgnore.contains(descriptor.getName())) {
				try {
					content.put(descriptor.getName(), descriptor.getReadMethod().invoke(rs));
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				} catch (InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			}
		}

		return HalFormsDocument.halFormsDocument()
			.resource(content)
			.links(rs.getLinks())
			.templates(new HashMap<String, Template>())
			.build();
	}

	/**
	 * Verify that the resource's self link and the affordance's URI have the same relative path.
	 * 
	 * @param resource
	 * @param affordance
	 */
	private static void validate(ResourceSupport resource, Affordance affordance) {

		try {
			Link selfLink = resource.getLink(Link.REL_SELF);

			URI uri = new URI(selfLink.expand().getHref());

			if (!uri.getPath().equals(affordance.getUri())) {
				throw new IllegalStateException("Affordance's URI " + affordance.getUri() + " doesn't match self link " + uri.getPath() + " as expected in HAL-FORMS");
			}
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Look up Java bean properties for a given bean
	 *
	 * @param bean
	 * @return
	 */
	private static PropertyDescriptor[] getPropertyDescriptors(Object bean) {
		try {
			return Introspector.getBeanInfo(bean.getClass()).getPropertyDescriptors();
		} catch (IntrospectionException e) {
			throw new RuntimeException("failed to get property descriptors of bean " + bean, e);
		}
	}
}
