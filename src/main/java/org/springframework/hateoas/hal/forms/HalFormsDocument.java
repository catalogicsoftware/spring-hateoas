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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;

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
@JsonPropertyOrder({ "resource", "resources", "embedded", "links", "templates", "metadata" })
//@JsonDeserialize(using = HalFormsDocumentDeserializer.class)
public class HalFormsDocument<T> {

	@JsonUnwrapped
	@JsonInclude(Include.NON_NULL)
	private T resource;

	@JsonIgnore
	private Collection<T> resources;

	@JsonProperty("_embedded")
	@JsonInclude(Include.NON_NULL)
	private Map<String, Object> embedded;

	@JsonProperty("page")
	@JsonInclude(Include.NON_NULL)
	private PagedResources.PageMetadata pageMetadata;

	@Singular private List<Link> links;

	@Singular private Map<String, Template> templates;

	HalFormsDocument(T resource, Collection<T> resources, Map<String, Object> embedded,
					 PagedResources.PageMetadata pageMetadata, List<Link> links, Map<String, Template> templates) {

		this.resource = resource;
		this.resources = resources;
		this.embedded = embedded;
		this.pageMetadata = pageMetadata;
		this.links = links;
		this.templates = templates;
	}

	HalFormsDocument() {
		this(null, null, null, null, new ArrayList<Link>(), new HashMap<String, Template>());
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

}
