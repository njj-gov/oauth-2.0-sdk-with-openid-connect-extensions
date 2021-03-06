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


import junit.framework.TestCase;

import com.nimbusds.oauth2.sdk.http.HTTPResponse;


public class RequestObjectPOSTErrorResponseTest extends TestCase {
	
	
	public void testLifeCycle() throws ParseException {
		
		RequestObjectPOSTErrorResponse errorResponse = new RequestObjectPOSTErrorResponse(HTTPResponse.SC_UNAUTHORIZED);
		
		assertEquals(HTTPResponse.SC_UNAUTHORIZED, errorResponse.getHTTPStatusCode());
		
		assertNull(errorResponse.getErrorObject().getCode());
		assertNull(errorResponse.getErrorObject().getDescription());
		assertEquals(HTTPResponse.SC_UNAUTHORIZED, errorResponse.getErrorObject().getHTTPStatusCode());
		
		HTTPResponse httpResponse = errorResponse.toHTTPResponse();
		assertEquals(HTTPResponse.SC_UNAUTHORIZED, httpResponse.getStatusCode());
		assertNull(httpResponse.getEntityContentType());
		assertNull(httpResponse.getContent());
		
		errorResponse = RequestObjectPOSTErrorResponse.parse(httpResponse);
		
		assertEquals(HTTPResponse.SC_UNAUTHORIZED, errorResponse.getHTTPStatusCode());
		
		assertNull(errorResponse.getErrorObject().getCode());
		assertNull(errorResponse.getErrorObject().getDescription());
		assertEquals(HTTPResponse.SC_UNAUTHORIZED, errorResponse.getErrorObject().getHTTPStatusCode());
	}
	
	
	public void testParseRejectHTTP2xx() {
		
		for (int statusCode = 200; statusCode < 300; statusCode++) {
			try {
				RequestObjectPOSTErrorResponse.parse(new HTTPResponse(statusCode));
				fail();
			} catch (ParseException e) {
				assertEquals("Unexpected HTTP status code, must not be 2xx", e.getMessage());
			}
		}
	}
}
