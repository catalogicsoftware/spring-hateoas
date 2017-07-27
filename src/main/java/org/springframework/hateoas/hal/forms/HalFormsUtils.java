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
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpMethod;

/**
 * @author Greg Turnquist
 */
final class HalFormsUtils {

	public static <T> HalFormsDocument<T> toHalFormsDocument(Resources<T> resources) {

		return HalFormsDocument.<T>halFormsDocument()
			.resources(resources.getContent())
			.links(resources.getLinks())
			.templates(findTemplates(resources))
			.build();
	}

	public static <T> HalFormsDocument<T> toHalFormsDocument(Map<String, Object> embeddeds, Resources<T> resources,
															 PagedResources.PageMetadata metadata) {

		return HalFormsDocument.<T>halFormsDocument()
			.embedded(embeddeds)
			.pageMetadata(metadata)
			.links(resources.getLinks())
			.templates(findTemplates(resources))
			.build();
	}

	/**
	 * Transform a {@link Resource} into a {@link HalFormsDocument}.
	 *
	 * @param resource
	 * @return
	 */
	public static <T> HalFormsDocument<T> toHalFormsDocument(Resource<T> resource) {

		return HalFormsDocument.<T>halFormsDocument()
			.resource(resource.getContent())
			.links(resource.getLinks())
			.templates(findTemplates(resource))
			.build();
	}

	/**
	 * Transform a {@link ResourceSupport} into a {@link HalFormsDocument}.
	 *
	 * @param rs
	 * @return
	 */
	public static HalFormsDocument<Map<String, Object>> toHalFormsDocument(ResourceSupport rs) {

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

		return HalFormsDocument.<Map<String, Object>>halFormsDocument()
			.resource(content)
			.links(rs.getLinks())
			.templates(findTemplates(rs))
			.build();
	}

	private static Map<String, Template> findTemplates(ResourceSupport resource) {

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

		return templates;
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
