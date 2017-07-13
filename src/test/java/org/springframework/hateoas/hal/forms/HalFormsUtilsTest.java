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

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.core.Is.is;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.hateoas.AbstractJackson2MarshallingIntegrationTest;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.core.AnnotationRelProvider;
import org.springframework.hateoas.hal.forms.Jackson2HalFormsModule.HalFormsHandlerInstantiator;

import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * @author Greg Turnquist
 */
public class HalFormsUtilsTest extends AbstractJackson2MarshallingIntegrationTest {

	@Before
	public void setUpModule() {

		mapper.registerModule(new Jackson2HalFormsModule());
		mapper.setHandlerInstantiator(new HalFormsHandlerInstantiator(
			new AnnotationRelProvider(), null, null, true));
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
	}

	@Test
	public void resourceSupportShouldTransform() {

		ResourceSupport expected = new ResourceSupport();
		expected.add(new Link("localhost"));
		expected.add(new Link("localhost2"));

		HalFormsDocument doc = (HalFormsDocument) HalFormsUtils.toHalFormsDocument(expected, mapper);

		assertThat(doc, is(notNullValue()));
		assertThat(doc.getLinks().size(), is(2));
		assertThat(doc.getTemplates().size(), is(0));
	}

	@Test
	public void extensionOfResourceSupportShouldCopyInAllPropertiesLikeAMap() {

		EmployeeResource expected = new EmployeeResource("Frodo");
		expected.add(new Link("localhost"));
		expected.add(new Link("localhost2"));

		HalFormsDocument doc = HalFormsUtils.toHalFormsDocument(expected, mapper);

		assertThat(doc, is(notNullValue()));
		assertThat(doc.getLinks().size(), is(2));
		assertThat(doc.getTemplates().size(), is(0));
		
		assertThat(doc.getContent(), is(notNullValue()));

		Map<String, Object> content = (Map<String, Object>) doc.getContent();
		assertThat(content.containsKey("name"), is(true));
		assertThat((String) content.get("name"), is("Frodo"));
	}

	@Test
	public void resourceShouldTransform() {

		Resource<Employee> employee = new Resource<Employee>(new Employee("Frodo", "ring bearer"));
		employee.add(new Link("localhost"));
		employee.add(new Link("localhost2"));

		HalFormsDocument doc = HalFormsUtils.toHalFormsDocument(employee, mapper);

		assertThat(doc, is(notNullValue()));
		assertThat(doc.getLinks().size(), is(2));
		assertThat(doc.getTemplates().size(), is(0));

		assertThat(doc.getContent(), is(notNullValue()));

		Employee insertedEmployee = (Employee) doc.getContent();

		assertThat(insertedEmployee.getName(), is("Frodo"));
		assertThat(insertedEmployee.getRole(), is("ring bearer"));
	}

	@Test
	public void simpleResourcesShouldTransform() {

		List<Employee> employees = Collections.singletonList(new Employee("Frodo", "ring bearer"));
		Resources<Employee> employeeResources = new Resources<Employee>(employees);
		employeeResources.add(new Link("localhost/employees").withRel("employees"));

		HalFormsDocument doc = HalFormsUtils.toHalFormsDocument(employeeResources, mapper);
		
		assertThat(doc, is(notNullValue()));
		assertThat(doc.getLinks().size(), is(1));
		assertThat(doc.getTemplates().size(), is(0));

		assertThat(doc.getContent(), is(notNullValue()));

		List<Employee> insertedEmployees = new ArrayList<Employee>((Collection<Employee>) doc.getContent());

		assertThat(insertedEmployees.size(), is(1));
		assertThat(insertedEmployees.get(0).getName(), is("Frodo"));
		assertThat(insertedEmployees.get(0).getRole(), is("ring bearer"));
	}

	@Test
	public void resourcesOfResourceShouldTransform() {

		List<Resource<Employee>> employees = Collections.singletonList(
			new Resource<Employee>(
				new Employee("Frodo", "ring bearer"),
				new Link("localhost").withSelfRel()));
		Resources<Resource<Employee>> employeeResources = new Resources<Resource<Employee>>(employees);
		employeeResources.add(new Link("localhost/employees").withRel("employees"));

		HalFormsDocument doc = HalFormsUtils.toHalFormsDocument(employeeResources, mapper);

		assertThat(doc, is(notNullValue()));
		assertThat(doc.getLinks().size(), is(1));
		assertThat(doc.getTemplates().size(), is(0));

		assertThat(doc.getContent(), is(notNullValue()));

		ArrayList<Resource<Employee>> insertedEmployees =
			new ArrayList<Resource<Employee>>((Collection<Resource<Employee>>) doc.getContent());

		assertThat(insertedEmployees.size(), is(1));
		assertThat(insertedEmployees.get(0).getContent().getName(), is("Frodo"));
		assertThat(insertedEmployees.get(0).getContent().getRole(), is("ring bearer"));
	}
}
