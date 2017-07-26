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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.hateoas.AbstractJackson2MarshallingIntegrationTest;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.core.AnnotationRelProvider;
import org.springframework.hateoas.hal.SimplePojo;
import org.springframework.hateoas.hal.forms.Jackson2HalFormsModule.HalFormsHandlerInstantiator;
import org.springframework.hateoas.support.MappingUtils;

import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * @author Greg Turnquist
 */
public class Jackson2HalFormsIntegrationTest extends AbstractJackson2MarshallingIntegrationTest {

	@Before
	public void setUpModule() {

		mapper.registerModule(new Jackson2HalFormsModule());
		mapper.setHandlerInstantiator(new HalFormsHandlerInstantiator(
			new AnnotationRelProvider(), null, null, true));
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
	}

	@Test
	public void rendersSingleLinkAsObject() throws Exception {

		ResourceSupport resourceSupport = new ResourceSupport();
		resourceSupport.add(new Link("localhost"));

		assertThat(write(resourceSupport),
			is(MappingUtils.read(new ClassPathResource("single-link-reference.json", getClass()))));
	}

	@Test
	public void deserializeSingleLink() throws Exception {

		ResourceSupport expected = new ResourceSupport();
		expected.add(new Link("localhost"));

		assertThat(read(MappingUtils.read(new ClassPathResource("single-link-reference.json", getClass())),
			ResourceSupport.class), is(expected));
	}

	@Test
	public void rendersMultipleLinkAsArray() throws Exception {

		ResourceSupport resourceSupport = new ResourceSupport();
		resourceSupport.add(new Link("localhost"));
		resourceSupport.add(new Link("localhost2"));

		assertThat(write(resourceSupport),
			is(MappingUtils.read(new ClassPathResource("list-link-reference.json", getClass()))));
	}

	@Test
	public void deserializeMultipleLinks() throws Exception {

		ResourceSupport expected = new ResourceSupport();
		expected.add(new Link("localhost"));
		expected.add(new Link("localhost2"));

		assertThat(read(MappingUtils.read(new ClassPathResource("list-link-reference.json", getClass())),
			ResourceSupport.class), is(expected));
	}

	@Test
	public void rendersResource() throws Exception {

		Resource<SimplePojo> resource = new Resource<SimplePojo>(new SimplePojo("test1", 1), new Link("localhost"));

		assertThat(write(resource),
			is(MappingUtils.read(new ClassPathResource("simple-resource-unwrapped.json", getClass()))));
	}

	@Test
	public void deserializesResource() throws IOException {

		Resource<SimplePojo> expected = new Resource<SimplePojo>(new SimplePojo("test1", 1), new Link("localhost"));

		Resource<SimplePojo> result = mapper.readValue(
			MappingUtils.read(new ClassPathResource("simple-resource-unwrapped.json", getClass())),
			mapper.getTypeFactory().constructParametricType(Resource.class, SimplePojo.class));

		assertThat(result, is(expected));
	}

	@Test
	public void rendersSimpleResourcesAsEmbedded() throws Exception {

		List<String> content = new ArrayList<String>();
		content.add("first");
		content.add("second");

		Resources<String> resources = new Resources<String>(content);
		resources.add(new Link("localhost"));

		assertThat(write(resources),
			is(MappingUtils.read(new ClassPathResource("simple-embedded-resource-reference.json", getClass()))));
	}

	@Test
	public void deserializesSimpleResourcesAsEmbedded() throws Exception {

		List<String> content = new ArrayList<String>();
		content.add("first");
		content.add("second");

		Resources<String> expected = new Resources<String>(content);
		expected.add(new Link("localhost"));

		Resources<String> result = mapper.readValue(
			MappingUtils.read(new ClassPathResource("simple-embedded-resource-reference.json", getClass())),
			mapper.getTypeFactory().constructParametricType(Resources.class, String.class));

		assertThat(result, is(expected));

	}

	@Test
	public void rendersSingleResourceResourcesAsEmbedded() throws Exception {

		List<Resource<SimplePojo>> content = new ArrayList<Resource<SimplePojo>>();
		content.add(new Resource<SimplePojo>(new SimplePojo("test1", 1), new Link("localhost")));

		Resources<Resource<SimplePojo>> resources = new Resources<Resource<SimplePojo>>(content);
		resources.add(new Link("localhost"));

		assertThat(write(resources),
			is(MappingUtils.read(new ClassPathResource("single-embedded-resource-reference.json", getClass()))));
	}

	@Test
	public void deserializesSingleResourceResourcesAsEmbedded() throws Exception {

		List<Resource<SimplePojo>> content = new ArrayList<Resource<SimplePojo>>();
		content.add(new Resource<SimplePojo>(new SimplePojo("test1", 1), new Link("localhost")));

		Resources<Resource<SimplePojo>> expected = new Resources<Resource<SimplePojo>>(content);
		expected.add(new Link("localhost"));

		Resources<Resource<SimplePojo>> result = mapper.readValue(
			MappingUtils.read(new ClassPathResource("single-embedded-resource-reference.json", getClass())),
			mapper.getTypeFactory().constructParametricType(Resources.class,
				mapper.getTypeFactory().constructParametricType(Resource.class, SimplePojo.class)));

		assertThat(result, is(expected));
	}

}
