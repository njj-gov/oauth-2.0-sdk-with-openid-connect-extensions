package com.nimbusds.oauth2.sdk;


import java.net.MalformedURLException;
import java.net.URL;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.jcip.annotations.Immutable;

import net.minidev.json.JSONObject;

import com.nimbusds.oauth2.sdk.http.CommonContentTypes;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;


/**
 * OAuth 2.0 Token error response. This class is immutable.
 *
 * <p>Standard token errors:
 *
 * <ul>
 *     <li>{@link OAuth2Error#INVALID_REQUEST}
 *     <li>{@link OAuth2Error#INVALID_CLIENT}
 *     <li>{@link OAuth2Error#INVALID_GRANT}
 *     <li>{@link OAuth2Error#UNAUTHORIZED_CLIENT}
 *     <li>{@link OAuth2Error#UNSUPPORTED_GRANT_TYPE}
 *     <li>{@link OAuth2Error#INVALID_SCOPE}
 * </ul>
 *
 * <p>Example HTTP response:
 *
 * <pre>
 * HTTP/1.1 400 Bad Request
 * Content-Type: application/json
 * Cache-Control: no-store
 * Pragma: no-cache
 * 
 * {
 *  "error": "invalid_request"
 * }
 * </pre>
 *
 * <p>Related specifications:
 *
 * <ul>
 *     <li>OAuth 2.0 (RFC 6749), section 5.2.
 * </ul>
 *
 * @author Vladimir Dzhuvinov
 * @version $version$ (2013-01-16)
 */
@Immutable
public class TokenErrorResponse implements OAuth2ErrorResponse {


	/**
	 * The standard OAuth 2.0 errors for an Access Token error response.
	 */
	private static final Set<OAuth2Error> stdErrors = new HashSet<OAuth2Error>();
	
	
	static {
		stdErrors.add(OAuth2Error.INVALID_REQUEST);
		stdErrors.add(OAuth2Error.INVALID_CLIENT);
		stdErrors.add(OAuth2Error.INVALID_GRANT);
		stdErrors.add(OAuth2Error.UNAUTHORIZED_CLIENT);
		stdErrors.add(OAuth2Error.UNSUPPORTED_GRANT_TYPE);
		stdErrors.add(OAuth2Error.INVALID_SCOPE);
	}
	
	
	/**
	 * Gets the standard OAuth 2.0 errors for an Access Token error 
	 * response.
	 *
	 * @return The standard errors, as a read-only set.
	 */
	public static Set<OAuth2Error> getStandardErrors() {
	
		return Collections.unmodifiableSet(stdErrors);
	}
	
	
	/**
	 * The error.
	 */
	private final OAuth2Error error;
	
	
	/**
	 * Creates a new OAuth 2.0 Access Token error response.
	 *
	 * @param error The OAuth 2.0 error. Should match one of the 
	 *              {@link #getStandardErrors standard errors} for a token 
	 *              error response. Must not be {@code null}.
	 */
	public TokenErrorResponse(final OAuth2Error error) {
	
		if (error == null)
			throw new IllegalArgumentException("The OAuth 2.0 error must not be null");
			
		this.error = error;
	}
	

	@Override
	public OAuth2Error getError() {
	
		return error;
	}
	
	
	/**
	 * Returns the JSON object for this token error response.
	 *
	 * @return The JSON object for this token error response.
	 */
	public JSONObject toJSONObject() {
	
		JSONObject o = new JSONObject();

		o.put("error", error.getValue());

		if (error.getDescription() != null)
			o.put("error_description", error.getDescription());
		
		if (error.getURI() != null)
			o.put("error_uri", error.getURI().toString());
		
		return o;
	}
	
	
	@Override
	public HTTPResponse toHTTPResponse()
		throws SerializeException {
		
		// HTTP status 400
		HTTPResponse httpResponse = new HTTPResponse(HTTPResponse.SC_BAD_REQUEST);
		
		httpResponse.setContentType(CommonContentTypes.APPLICATION_JSON);
		httpResponse.setCacheControl("no-store");
		httpResponse.setPragma("no-cache");
		
		httpResponse.setContent(toJSONObject().toString());
		
		return httpResponse;
	}
	
	
	/**
	 * Parses an OAuth 2.0 Token Error response.
	 *
	 * @param httpResponse The HTTP response to parse. Must not be 
	 *                     {@code null}.
	 *
	 * @throws ParseException If the HTTP response cannot be parsed to a 
	 *                        valid OAuth 2.0 Token Error response.
	 */
	public static TokenErrorResponse parse(final HTTPResponse httpResponse)
		throws ParseException {
		
		httpResponse.ensureStatusCode(HTTPResponse.SC_BAD_REQUEST);

		// Cache-Control and Pragma headers are ignored
		
		JSONObject jsonObject = httpResponse.getContentAsJSONObject();
		
		OAuth2Error error = null;
		
		try {
			// Parse code
			String code = JSONObjectUtils.getString(jsonObject, "error");

			// Parse description
			String description = null;

			if (jsonObject.containsKey("error_description"))
				description = JSONObjectUtils.getString(jsonObject, "error_description");

			// Parse URI
			URL uri = null;

			if (jsonObject.containsKey("error_uri"))
				uri = new URL(JSONObjectUtils.getString(jsonObject, "error_uri"));


			error = new OAuth2Error(code, description, uri);
			
		} catch (ParseException e) {
		
			throw new ParseException("Missing or invalid token error response parameter: " + e.getMessage(), e);
			
		} catch (MalformedURLException e) {
		
			throw new ParseException("Invalid error URI: " + e.getMessage(), e);
		}
		
		return new TokenErrorResponse(error);
	}
}