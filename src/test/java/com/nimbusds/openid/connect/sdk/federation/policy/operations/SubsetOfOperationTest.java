/*
 * oauth2-oidc-sdk
 *
 * Copyright 2012-2016, Connect2id Ltd and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.nimbusds.openid.connect.sdk.federation.policy.operations;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.policy.language.OperationName;


public class SubsetOfOperationTest extends TestCase {
	
	
	public void testSubset() {
		
		List<String> param = Arrays.asList("code", "code token", "code id_token");
		
		SubsetOfOperation operation = new SubsetOfOperation();
		assertEquals(new OperationName("subset_of"), operation.getOperationName());
		operation.configure(param);
		assertEquals(param, operation.getStringListConfiguration());
		
		List<String> stringList = Arrays.asList("code", "code id_token token", "code id_token");
		
		assertEquals(Arrays.asList("code", "code id_token"), operation.apply(stringList));
	}
	
	
	public void testSubsetIdentical() {
		
		List<String> param = Arrays.asList("code", "code token", "code id_token");
		
		SubsetOfOperation operation = new SubsetOfOperation();
		assertEquals(new OperationName("subset_of"), operation.getOperationName());
		operation.configure(param);
		assertEquals(param, operation.getStringListConfiguration());
		
		assertEquals(param, operation.apply(param));
	}
	
	
	public void testSubsetEmptyValue() {
		
		List<String> param = Arrays.asList("code", "code token", "code id_token");
		
		SubsetOfOperation operation = new SubsetOfOperation();
		assertEquals(new OperationName("subset_of"), operation.getOperationName());
		operation.configure(param);
		assertEquals(param, operation.getStringListConfiguration());
		
		assertEquals(Collections.emptyList(), operation.apply(Collections.<String>emptyList()));
	}
	
	
	public void testSubsetNullValue() {
		
		List<String> param = Arrays.asList("code", "code token", "code id_token");
		
		SubsetOfOperation operation = new SubsetOfOperation();
		assertEquals(new OperationName("subset_of"), operation.getOperationName());
		operation.configure(param);
		assertEquals(param, operation.getStringListConfiguration());
		
		assertEquals(Collections.emptyList(), operation.apply(null));
	}
	
	
	public void testNotInit() {
		
		try {
			new SubsetOfOperation().apply(null);
			fail();
		} catch (IllegalStateException e) {
			assertEquals("The policy is not initialized", e.getMessage());
		}
	}
	
	
	public void testParseConfiguration()
		throws ParseException {
		
		List<String> param = Arrays.asList("code", "code token", "code id_token");
		
		SubsetOfOperation operation = new SubsetOfOperation();
		operation.parseConfiguration((Object)param);
		assertEquals(param, operation.getStringListConfiguration());
	}
}
