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

package com.nimbusds.oauth2.sdk;


import java.net.URI;

import junit.framework.TestCase;

import com.nimbusds.oauth2.sdk.http.HTTPResponse;


public class PushedAuthorizationResponseTest extends TestCase {
	
	
	public void testParseSuccess() throws ParseException {
		
		URI requestURI = URI.create("urn:ietf:params:oauth:request_uri:tioteej8");
		long lifetime = 3600L;
		
		PushedAuthorizationSuccessResponse response = new PushedAuthorizationSuccessResponse(requestURI, lifetime);
		
		HTTPResponse httpResponse = response.toHTTPResponse();
		assertEquals(201, httpResponse.getStatusCode());
		
		response = PushedAuthorizationResponse.parse(httpResponse).toSuccessResponse();
		assertEquals(requestURI, response.getRequestURI());
		assertEquals(lifetime, response.getLifetime());
	}
	
	// Be lenient on HTTP 200
	public void testParseSuccess200() throws ParseException {
		
		URI requestURI = URI.create("urn:ietf:params:oauth:request_uri:tioteej8");
		long lifetime = 3600L;
		
		PushedAuthorizationSuccessResponse response = new PushedAuthorizationSuccessResponse(requestURI, lifetime);
		
		HTTPResponse httpResponse = response.toHTTPResponse();
		assertEquals(201, httpResponse.getStatusCode());
		
		HTTPResponse modifiedHTTPResponse = new HTTPResponse(200);
		modifiedHTTPResponse.setEntityContentType(httpResponse.getEntityContentType());
		modifiedHTTPResponse.setContent(httpResponse.getContent());
		
		response = PushedAuthorizationResponse.parse(modifiedHTTPResponse).toSuccessResponse();
		assertEquals(requestURI, response.getRequestURI());
		assertEquals(lifetime, response.getLifetime());
	}
	
	
	public void testParseError() throws ParseException {
		
		PushedAuthorizationErrorResponse response = new PushedAuthorizationErrorResponse(new ErrorObject(null, null, 400));
		
		HTTPResponse httpResponse = response.toHTTPResponse();
		
		response = PushedAuthorizationResponse.parse(httpResponse).toErrorResponse();
		assertFalse(response.indicatesSuccess());
		assertTrue(response.getErrorObject().toParameters().isEmpty());
	}
}
