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

package com.nimbusds.oauth2.sdk.auth;


import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLSocketFactory;

import net.jcip.annotations.Immutable;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.util.MultivaluedMapUtils;
import com.nimbusds.oauth2.sdk.util.StringUtils;
import com.nimbusds.oauth2.sdk.util.URLUtils;


/**
 * PKI mutual TLS client authentication at the Token endpoint. The client
 * certificate is PKI bound, as opposed to
 * {@link SelfSignedTLSClientAuthentication self_signed_tls_client_auth} which
 * relies on a self-signed certificate. Implements
 * {@link ClientAuthenticationMethod#TLS_CLIENT_AUTH}.
 *
 * <p>Related specifications:
 *
 * <ul>
 *     <li>OAuth 2.0 Mutual TLS Client Authentication and Certificate Bound
 *         Access Tokens (RFC 8705), section 2.1.
 * </ul>
 */
@Immutable
public class PKITLSClientAuthentication extends TLSClientAuthentication {
	
	
	/**
	 * The client X.509 certificate subject DN.
	 */
	private final String certSubjectDN;
	
	
	/**
	 * Creates a new PKI mutual TLS client authentication. This constructor
	 * is intended for an outgoing token request.
	 *
	 * @param clientID         The client identifier. Must not be
	 *                         {@code null}.
	 * @param sslSocketFactory The SSL socket factory to use for the
	 *                         outgoing HTTPS request and to present the
	 *                         client certificate(s), {@code null} to use
	 *                         the default one.
	 */
	public PKITLSClientAuthentication(final ClientID clientID,
					  final SSLSocketFactory sslSocketFactory) {
		
		super(ClientAuthenticationMethod.TLS_CLIENT_AUTH, clientID, sslSocketFactory);
		certSubjectDN = null;
	}
	
	
	/**
	 * Creates a new PKI mutual TLS client authentication. This constructor
	 * is intended for a received token request.
	 *
	 * @param clientID      The client identifier. Must not be
	 *                      {@code null}.
	 * @param certSubjectDN The subject DN of the received validated client
	 *                      X.509 certificate. Must not be {@code null}.
	 * @deprecated This constructor does set the certificate
	 */
	@Deprecated
	public PKITLSClientAuthentication(final ClientID clientID,
					  final String certSubjectDN) {
		
		super(ClientAuthenticationMethod.TLS_CLIENT_AUTH, clientID, (X509Certificate) null);
		
		if (certSubjectDN == null) {
			throw new IllegalArgumentException("The X.509 client certificate subject DN must not be null");
		}
		this.certSubjectDN = certSubjectDN;
	}
	
	
	/**
	 * Creates a new PKI mutual TLS client authentication. This constructor
	 * is intended for a received token request.
	 *
	 * @param clientID    The client identifier. Must not be {@code null}.
	 * @param certificate The validated client X.509 certificate from the
	 *                    received HTTPS request. Must not be {@code null}.
	 */
	public PKITLSClientAuthentication(final ClientID clientID,
					  final X509Certificate certificate) {
		
		super(ClientAuthenticationMethod.TLS_CLIENT_AUTH, clientID, certificate);
		
		if (certificate == null) {
			throw new IllegalArgumentException("The X.509 client certificate must not be null");
		}
		this.certSubjectDN = certificate.getSubjectX500Principal().getName();
	}
	
	
	/**
	 * Gets the subject DN of the received validated client X.509
	 * certificate.
	 *
	 * @return The subject DN.
	 */
	public String getClientX509CertificateSubjectDN() {
		
		return certSubjectDN;
	}
	
	
	/**
	 * Parses a PKI mutual TLS client authentication from the specified
	 * HTTP request.
	 *
	 * @param httpRequest The HTTP request to parse. Must not be
	 *                    {@code null} and must include a validated client
	 *                    X.509 certificate.
	 *
	 * @return The PKI mutual TLS client authentication.
	 *
	 * @throws ParseException If the {@code client_id} or client X.509
	 *                        certificate is missing.
	 */
	public static PKITLSClientAuthentication parse(final HTTPRequest httpRequest)
		throws ParseException {
		
		String query = httpRequest.getQuery();
		
		if (query == null) {
			throw new ParseException("Missing HTTP POST request entity body");
		}
		
		Map<String,List<String>> params = URLUtils.parseParameters(query);
		
		String clientIDString = MultivaluedMapUtils.getFirstValue(params, "client_id");
		
		if (StringUtils.isBlank(clientIDString)) {
			throw new ParseException("Missing client_id parameter");
		}
		
		if (httpRequest.getClientX509Certificate() == null) {
			throw new ParseException("Missing client X.509 certificate");
		}
		
		return new PKITLSClientAuthentication(
			new ClientID(clientIDString),
			httpRequest.getClientX509Certificate()
		);
	}
}
