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
package org.springframework.hateoas.mvc;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.hateoas.Affordance;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Spring MVC-based representation of an {@link Affordance}.
 * 
 * @author Greg Turnquist
 */
@Data
public class SpringMvcAffordance implements Affordance {

	private static final Logger log = LoggerFactory.getLogger(SpringMvcAffordance.class);

	/**
	 * Request method verb associated with the Spring MVC controller method.
	 */
	private final RequestMethod requestMethod;

	/**
	 * Handle on the Spring MVC controller {@link Method}.
	 */
	private final Method method;

	/**
	 * {@literal boolean} indicator as to whether or not {@link Affordance} attributes are required.
	 */
	private final boolean required;

	/**
	 * {@link Map} of property names and their types associated with the incoming request body.
	 */
	private final Map<String, Class<?>> properties;

	/**
	 * Test constructor. Do NOT use this in production.
	 */
	public SpringMvcAffordance() {

		this.required = false;
		this.method = null;
		this.requestMethod = RequestMethod.POST;
		this.properties = null;
	}

	/**
	 * Construct a Spring MVC-based {@link Affordance} based on Spring MVC controller method and {@link RequestMethod}.
	 */
	public SpringMvcAffordance(Method method, Class<?> targetType, RequestMethod requestMethod) {

		this.requestMethod = requestMethod;
		this.method = method;
		this.required = determineRequired(requestMethod);
		this.properties = new TreeMap<String, Class<?>>();

		switch (requestMethod) {
			case POST:
			case PUT:
			case PATCH:
				determineAffordanceInputs(method);
				break;
			default:
		}
	}

	/**
	 * Based on the Spring MVC controller's {@link RequestMethod}, decided whether or not input attributes
	 * are required or not.
	 * 
	 * @param requestMethod
	 * @return
	 */
	private boolean determineRequired(RequestMethod requestMethod) {

		switch (requestMethod) {
			case POST:
			case PUT:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Look at the inputs for a Spring MVC controller method to decide the {@link Affordance}'s properties.
	 * 
	 * @param method
	 */
	private void determineAffordanceInputs(Method method) {


		log.debug("Gathering details about " + method.getDeclaringClass().getCanonicalName() + "." + method.getName());

		for (int i = 0; i < method.getParameterTypes().length; i++) {

			for (Annotation annotation : method.getParameterAnnotations()[i]) {

				if (annotation.annotationType().equals(RequestBody.class)) {

					log.debug("\tRequest body: " + method.getParameterTypes()[i].getCanonicalName() + "(");

					for (PropertyDescriptor descriptor : BeanUtils.getPropertyDescriptors(method.getParameterTypes()[i])) {

						if (!descriptor.getName().equals("class")) {
							log.debug("\t\t" + descriptor.getPropertyType().getCanonicalName() + " " + descriptor.getName());
							this.getProperties().put(descriptor.getName(), descriptor.getPropertyType());
						}
					}
					log.debug(")");
				}
			}
		}

		log.debug("Assembled " + this.toString());
	}

	@Override
	public String getMethodName() {
		return this.method.getName();
	}

	@Override
	public String getVerb() {
		return this.requestMethod.toString();
	}

	/**
	 * Helper method to generate a list of {@link Affordance}s based on a {@link Link}.
	 * 
	 * @param link
	 * @return
	 */
	public static List<Affordance> byLink(Link link) {
		return link.getAffordances();
	}

}
