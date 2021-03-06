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

package com.nimbusds.oauth2.sdk.http;


import javax.mail.internet.ContentType;
import javax.mail.internet.ParameterList;


/**
 * Common content types used in the OAuth 2.0 protocol and implementing 
 * applications. The character set all of content types is set to 
 * {@link #DEFAULT_CHARSET UTF-8}.
 */
@Deprecated
public final class CommonContentTypes {


	/**
	 * The default character set.
	 */
	@Deprecated
	public static final String DEFAULT_CHARSET = "UTF-8";


	/**
	 * The default content type parameter list.
	 */
	@Deprecated
	private static final ParameterList PARAM_LIST = new ParameterList();


	/**
	 * Content type {@code application/json}.
	 */
	@Deprecated
	public static final ContentType APPLICATION_JSON = new ContentType("application", "json", PARAM_LIST);
	
	
	/**
	 * Content type {@code application/jose}.
	 */
	@Deprecated
	public static final ContentType APPLICATION_JOSE = new ContentType("application", "jose", PARAM_LIST);
	
	
	/**
	 * Content type {@code application/jwt}.
	 */
	@Deprecated
	public static final ContentType APPLICATION_JWT = new ContentType("application", "jwt", PARAM_LIST);
	
	
	/**
	 * Content type {@code application/x-www-form-urlencoded}.
	 */
	@Deprecated
	public static final ContentType APPLICATION_URLENCODED = new ContentType("application", "x-www-form-urlencoded", PARAM_LIST);


	static {
		PARAM_LIST.set("charset", DEFAULT_CHARSET);
	}
}
