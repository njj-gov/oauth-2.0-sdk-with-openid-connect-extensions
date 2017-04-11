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

package com.nimbusds.openid.connect.sdk;


import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.CommonContentTypes;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.JWTID;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.openid.connect.sdk.claims.LogoutTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.SessionID;
import junit.framework.TestCase;


public class BackChannelLogoutRequestTest extends TestCase {
	
	
	private static final RSAKey RSA_JWK;
	
	
	private static URI LOGOUT_ENDPOINT_URI = URI.create("https://rp.example.com/logout");
	
	
	private static URL LOGOUT_ENDPOINT_URL;
	
	
	static {
		try {
			KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
			gen.initialize(1024);
			KeyPair kp = gen.generateKeyPair();
			RSA_JWK = new RSAKey.Builder((RSAPublicKey)kp.getPublic())
				.privateKey((RSAPrivateKey)kp.getPrivate())
				.keyIDFromThumbprint()
				.build();
			
			LOGOUT_ENDPOINT_URL = LOGOUT_ENDPOINT_URI.toURL();
			
		} catch (NoSuchAlgorithmException | JOSEException | MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	private static JWTClaimsSet createLogoutTokenClaimsSet() {
		
		LogoutTokenClaimsSet claimsSet = new LogoutTokenClaimsSet(
			new Issuer(URI.create("https://c2id.com")),
			new Subject("alice"),
			new Audience("123").toSingleAudienceList(),
			new Date(),
			new JWTID(),
			new SessionID(UUID.randomUUID().toString()));
		
		try {
			return claimsSet.toJWTClaimsSet();
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	

	private static JWT createSignedLogoutToken() {
		
		
		SignedJWT jwt = new SignedJWT(
			new JWSHeader.Builder(JWSAlgorithm.RS256)
				.keyID(RSA_JWK.getKeyID())
				.build(),
			createLogoutTokenClaimsSet());
		
		try {
			jwt.sign(new RSASSASigner(RSA_JWK));
		} catch (JOSEException e) {
			throw new RuntimeException(e);
		}
		
		return jwt;
	}
	
	
	public void testLifeCycle()
		throws Exception {
		
		JWT logoutToken = createSignedLogoutToken();
		
		BackChannelLogoutRequest request = new BackChannelLogoutRequest(LOGOUT_ENDPOINT_URI, logoutToken);
		
		assertEquals(LOGOUT_ENDPOINT_URI, request.getEndpointURI());
		assertEquals(logoutToken, request.getLogoutToken());
		
		Map<String,String> params = request.toParameters();
		assertEquals(logoutToken.serialize(), params.get("logout_token"));
		assertEquals(1, params.size());
		
		HTTPRequest httpRequest = request.toHTTPRequest();
		
		assertEquals(HTTPRequest.Method.POST, httpRequest.getMethod());
		assertEquals(CommonContentTypes.APPLICATION_URLENCODED.toString(), httpRequest.getContentType().toString());
		params = httpRequest.getQueryParameters();
		assertEquals(logoutToken.serialize(), params.get("logout_token"));
		assertEquals(1, params.size());
		
		// Parse from HTTP request
		request = BackChannelLogoutRequest.parse(httpRequest);
		assertEquals(LOGOUT_ENDPOINT_URI, request.getEndpointURI());
		assertEquals(logoutToken.serialize(), request.getLogoutToken().serialize());
		
		// Parse from URI + parameters
		request = BackChannelLogoutRequest.parse(LOGOUT_ENDPOINT_URI, params);
		assertEquals(LOGOUT_ENDPOINT_URI, request.getEndpointURI());
		assertEquals(logoutToken.serialize(), request.getLogoutToken().serialize());
		
		// Parse from parameters
		request = BackChannelLogoutRequest.parse(params);
		assertNull(request.getEndpointURI());
		assertEquals(logoutToken.serialize(), request.getLogoutToken().serialize());
	}
	
	
	public void testParseMissingParams()
		throws Exception {
		
		HTTPRequest httpRequest = new HTTPRequest(HTTPRequest.Method.POST, LOGOUT_ENDPOINT_URL);
		httpRequest.setContentType(CommonContentTypes.APPLICATION_URLENCODED);
		
		try {
			BackChannelLogoutRequest.parse(httpRequest);
			fail();
		} catch (ParseException e) {
			assertEquals("Missing URI query string", e.getMessage());
		}
	}
	
	
	public void testParseInvalidJWT()
		throws Exception {
		
		HTTPRequest httpRequest = new HTTPRequest(HTTPRequest.Method.POST, LOGOUT_ENDPOINT_URL);
		httpRequest.setContentType(CommonContentTypes.APPLICATION_URLENCODED);
		httpRequest.setQuery("logout_token=ey...");
		
		try {
			BackChannelLogoutRequest.parse(httpRequest);
			fail();
		} catch (ParseException e) {
			assertEquals("Invalid logout token: Invalid unsecured/JWS/JWE header: Invalid JSON: Unexpected token  at position 1.", e.getMessage());
		}
	}
	
	
	public void testRejectPlainJWT_constructor()
		throws Exception {
		
		URI LOGOUT_ENDPOINT_URI = URI.create("https://rp.example.com/logout");
		
		PlainJWT jwt = new PlainJWT(createLogoutTokenClaimsSet());
		
		try {
			new BackChannelLogoutRequest(LOGOUT_ENDPOINT_URI, jwt);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("The logout token must not be unsecured (plain)", e.getMessage());
		}
	}
	
	
	public void testRejectPlainJWT_parse()
		throws Exception {
		
		PlainJWT jwt = new PlainJWT(createLogoutTokenClaimsSet());
		
		HTTPRequest httpRequest = new HTTPRequest(HTTPRequest.Method.POST, LOGOUT_ENDPOINT_URL);
		httpRequest.setContentType(CommonContentTypes.APPLICATION_URLENCODED);
		httpRequest.setQuery("logout_token=" + jwt.serialize());
		
		try {
			BackChannelLogoutRequest.parse(httpRequest);
			fail();
		} catch (ParseException e) {
			assertEquals("The logout token must not be unsecured (plain)", e.getMessage());
		}
	}
	
	
	public void testRejectGET()
		throws Exception {
		
		HTTPRequest httpRequest = new HTTPRequest(HTTPRequest.Method.GET, LOGOUT_ENDPOINT_URL);
		JWT logoutToken = createSignedLogoutToken();
		httpRequest.setQuery("logout_token=" + logoutToken.serialize());
		
		try {
			BackChannelLogoutRequest.parse(httpRequest);
			fail();
		} catch (ParseException e) {
			assertEquals("HTTP POST required", e.getMessage());
		}
	}
	
	
	public void testIgnoreMissingContentType()
		throws Exception {
		
		HTTPRequest httpRequest = new HTTPRequest(HTTPRequest.Method.POST, LOGOUT_ENDPOINT_URL);
		JWT logoutToken = createSignedLogoutToken();
		httpRequest.setQuery("logout_token=" + logoutToken.serialize());
		
		BackChannelLogoutRequest request = BackChannelLogoutRequest.parse(httpRequest);
		assertEquals(LOGOUT_ENDPOINT_URI, request.getEndpointURI());
		assertEquals(logoutToken.serialize(), request.getLogoutToken().serialize());
	}
	
	
	public void testIgnoreMismatchedContentType()
		throws Exception {
		
		HTTPRequest httpRequest = new HTTPRequest(HTTPRequest.Method.POST, LOGOUT_ENDPOINT_URL);
		httpRequest.setContentType(CommonContentTypes.APPLICATION_JSON);
		JWT logoutToken = createSignedLogoutToken();
		httpRequest.setQuery("logout_token=" + logoutToken.serialize());
		
		BackChannelLogoutRequest request = BackChannelLogoutRequest.parse(httpRequest);
		assertEquals(LOGOUT_ENDPOINT_URI, request.getEndpointURI());
		assertEquals(logoutToken.serialize(), request.getLogoutToken().serialize());
	}
}
