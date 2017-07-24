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

import static com.fasterxml.jackson.annotation.JsonInclude.*;
import static org.springframework.hateoas.hal.Jackson2HalModule.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.hal.forms.HalFormsDeserializers.HalFormsDocumentDeserializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Representation of a HAL-Forms document.
 * 
 * @author Dietrich Schulten
 * @author Greg Turnquist
 */
@Data
@Builder(builderMethodName = "halFormsDocument")
@JsonPropertyOrder({ "resource", "resources", "links", "templates" })
@JsonDeserialize(using = HalFormsDocumentDeserializer.class)
public class HalFormsDocument {

	/**
	 * Attribute used to store single instance values.
	 */
	@JsonUnwrapped
	@JsonInclude(Include.NON_NULL)
	private Object resource;

	/**
	 * Attributes used to store collections.
	 */
	@JsonProperty("_embedded")
	@JsonInclude(Include.NON_NULL)
	private Object resources;

	@Singular private List<Link> links;

	@Singular private Map<String, Template> templates;

	HalFormsDocument(Object resource, Object resources, List<Link> links, Map<String, Template> templates) {

		this.resource = resource;
		this.resources = resources;
		this.links = links;
		this.templates = templates;
	}

	HalFormsDocument() {
		this(null, null, new ArrayList<Link>(), new HashMap<String, Template>());
	}

	@JsonProperty("_links")
	@JsonInclude(Include.NON_EMPTY)
	@JsonSerialize(using = HalLinkListSerializer.class)
	@JsonDeserialize(using = HalLinkListDeserializer.class)
	public List<Link> getLinks() {
		return this.links;
	}

	@JsonProperty("_templates")
	@JsonInclude(Include.NON_EMPTY)
	public Map<String, Template> getTemplates() {
		return this.templates;
	}

	@JsonIgnore
	public Template getTemplate() {
		return getTemplate(Template.DEFAULT_KEY);
	}

	@JsonIgnore
	public Template getTemplate(String key) {
		return this.templates.get(key);
	}

	/**
	 * General purpose way to retrieve stored data. See {@link #getResource()} or {@link #getResources()} if you know the
	 * data type you seek and wish to be more specific.
	 *
	 * @return
	 */
	@JsonIgnore
	public Object getContent() {

		if (this.resources != null) {
			return this.resources;
		} else {
			return this.resource;
		}
	}
}
