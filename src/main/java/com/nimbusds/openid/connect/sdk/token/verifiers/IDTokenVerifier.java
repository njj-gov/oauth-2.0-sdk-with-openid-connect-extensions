package com.nimbusds.openid.connect.sdk.token.verifiers;


import java.net.MalformedURLException;
import java.net.URL;

import com.nimbusds.jose.*;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWEKeySelector;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jwt.*;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTClaimsVerifier;
import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.jwt.*;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformation;
import net.jcip.annotations.ThreadSafe;


/**
 * Verifier of ID tokens issued by an OpenID Provider (OP).
 *
 * <p>Supported ID tokens:
 *
 * <ul>
 *     <li>ID tokens signed (JWS) with the OP's RSA or EC key, require the
 *         OP public JWK set (provided by value or URL) to verify them.
 *     <li>ID tokens authenticated with a JWS HMAC, require the client's secret
 *         to verify them.
 *     <li>Unsecured (plain) ID tokens received
 *
 * </ul>
 */
@ThreadSafe
public class IDTokenVerifier {


	/**
	 * The expected ID token issuer.
	 */
	private final Issuer expectedIssuer;


	/**
	 * The requesting client.
	 */
	private final ClientID clientID;


	/**
	 * The JWS key selector.
	 */
	private final JWSKeySelector jwsKeySelector;


	/**
	 * The JWE key selector.
	 */
	private final JWEKeySelector jweKeySelector;


	/**
	 * Creates a new verifier for unsecured (plain) ID tokens.
	 *
	 * @param expectedIssuer
	 * @param clientID
	 */
	public IDTokenVerifier(final Issuer expectedIssuer,
			       final ClientID clientID) {

		this(expectedIssuer, clientID, (JWSKeySelector) null, (JWEKeySelector) null);
	}


	/**
	 * Creates a new verifier for RSA or EC signed ID tokens.
	 *
	 * @param expectedIssuer
	 * @param clientID
	 * @param expectedJWSAlg
	 * @param jwkSetSource
	 */
	public IDTokenVerifier(final Issuer expectedIssuer,
			       final ClientID clientID,
			       final JWSAlgorithm expectedJWSAlg,
			       final JWKSetSource jwkSetSource) {

		this(expectedIssuer, clientID, new SignatureKeySelector(expectedIssuer, expectedJWSAlg, jwkSetSource),  null);
	}


	/**
	 * Creates a new verifier for HMAC protected ID tokens.
	 *
	 * @param expectedIssuer
	 * @param clientID
	 * @param expectedJWSAlg
	 * @param clientSecret
	 */
	public IDTokenVerifier(final Issuer expectedIssuer,
			       final ClientID clientID,
			       final JWSAlgorithm expectedJWSAlg,
			       final Secret clientSecret) {

		this(expectedIssuer, clientID, new ClientSecretSelector(expectedIssuer, expectedJWSAlg, clientSecret), null);
	}


	/**
	 * Creates a new ID token verifier.
	 *
	 * @param expectedIssuer
	 * @param clientID
	 * @param jwsKeySelector
	 */
	public IDTokenVerifier(final Issuer expectedIssuer,
			       final ClientID clientID,
			       final JWSKeySelector jwsKeySelector,
			       final JWEKeySelector jweKeySelector) {
		this.expectedIssuer = expectedIssuer;
		this.clientID = clientID;
		this.jwsKeySelector = jwsKeySelector;
		this.jweKeySelector = jweKeySelector;
	}


	/**
	 * Returns the expected ID token issuer.
	 *
	 * @return The ID token issuer.
	 */
	public Issuer getExpectedIssuer() {
		return expectedIssuer;
	}


	/**
	 * Returns the client ID (the expected ID token audience).
	 *
	 * @return The client ID.
	 */
	public ClientID getClientID() {
		return clientID;
	}


	/**
	 * Returns the configured JWS key selector for signed ID token
	 * verification.
	 *
	 * @return The JWS key selector, {@code null} if none.
	 */
	public JWSKeySelector getJWSKeySelector() {
		return jwsKeySelector;
	}


	/**
	 * Returns the configured JWE key selector for encrypted ID token
	 * decryption.
	 *
	 * @return The JWE key selector, {@code null}.
	 */
	public JWEKeySelector getJWEKeySelector() {
		return jweKeySelector;
	}


	public IDTokenClaimsSet verify(final JWT idToken, final Nonce expectedNonce)
		throws BadJOSEException, JOSEException {

		if (idToken instanceof PlainJWT) {
			return verify((PlainJWT)idToken, expectedNonce);
		} else if (idToken instanceof SignedJWT) {
			return verify((SignedJWT) idToken, expectedNonce);
		} else if (idToken instanceof EncryptedJWT) {
			return verify((EncryptedJWT) idToken, expectedNonce);
		} else {
			throw new JOSEException("Unexpected JWT type: " + idToken.getClass());
		}
	}


	private IDTokenClaimsSet verify(final PlainJWT idToken, final Nonce expectedNonce)
		throws BadJOSEException, JOSEException {

		if (jwsKeySelector != null) {
			throw new BadJWTException("Signed ID token expected");
		}

		JWTClaimsSet jwtClaimsSet;

		try {
			jwtClaimsSet = idToken.getJWTClaimsSet();
		} catch (java.text.ParseException e) {
			throw new BadJWTException(e.getMessage(), e);
		}

		JWTClaimsVerifier claimsVerifier = new IDTokenClaimsVerifier(expectedIssuer, clientID, expectedNonce);
		claimsVerifier.verify(jwtClaimsSet);
		return toIDTokenClaimsSet(jwtClaimsSet);
	}


	private IDTokenClaimsSet verify(final SignedJWT idToken, final Nonce expectedNonce)
		throws BadJOSEException, JOSEException {

		if (jwsKeySelector == null) {
			throw new BadJWTException("Verification of signed JWTs not configured");
		}

		ConfigurableJWTProcessor jwtProcessor = new DefaultJWTProcessor();
		jwtProcessor.setJWSKeySelector(jwsKeySelector);
		jwtProcessor.setJWTClaimsVerifier(new IDTokenClaimsVerifier(expectedIssuer, clientID, expectedNonce));
		JWTClaimsSet jwtClaimsSet = jwtProcessor.process(idToken, null);
		return toIDTokenClaimsSet(jwtClaimsSet);
	}


	private IDTokenClaimsSet verify(final EncryptedJWT idToken, final Nonce expectedNonce)
		throws BadJOSEException, JOSEException {

		if (jweKeySelector == null) {
			throw new BadJWTException("Decryption of JWTs not configured");
		}
		if (jwsKeySelector == null) {
			throw new BadJWTException("Verification of signed JWTs not configured");
		}

		ConfigurableJWTProcessor jwtProcessor = new DefaultJWTProcessor();
		jwtProcessor.setJWSKeySelector(jwsKeySelector);
		jwtProcessor.setJWEKeySelector(jweKeySelector);
		jwtProcessor.setJWTClaimsVerifier(new IDTokenClaimsVerifier(expectedIssuer, clientID, expectedNonce));

		JWTClaimsSet jwtClaimsSet = jwtProcessor.process(idToken, null);

		return toIDTokenClaimsSet(jwtClaimsSet);
	}


	private static IDTokenClaimsSet toIDTokenClaimsSet(final JWTClaimsSet jwtClaimsSet)
		throws JOSEException {

		try {
			return new IDTokenClaimsSet(jwtClaimsSet);
		} catch (ParseException e) {
			// Claims set must be verified at this point
			throw new JOSEException(e.getMessage(), e);
		}
	}


	private static JWSKeySelector createJWSKeySelector(final OIDCProviderMetadata opMetadata,
							   final OIDCClientInformation clientInfo)
		throws GeneralException {

		final Issuer expectedIssuer = opMetadata.getIssuer();
		final ClientID clientID = clientInfo.getID();
		final JWSAlgorithm expectedJWSAlg = clientInfo.getOIDCMetadata().getIDTokenJWSAlg();

		if (opMetadata.getIDTokenJWSAlgs() == null) {
			throw new GeneralException("Missing OpenID Provider id_token_signing_alg_values_supported parameter");
		}

		if (! opMetadata.getIDTokenJWSAlgs().contains(expectedJWSAlg)) {
			throw new GeneralException("The OpenID Provider doesn't support " + expectedJWSAlg + " ID tokens");
		}

		if (Algorithm.NONE.equals(expectedJWSAlg)) {
			// Skip creation of JWS key selector, plain ID tokens expected
			return null;

		} else if (JWSAlgorithm.Family.RSA.contains(expectedJWSAlg) || JWSAlgorithm.Family.EC.contains(expectedJWSAlg)) {

			JWKSetSource jwkSetSource;

			if (clientInfo.getOIDCMetadata().getJWKSet() != null) {
				// The JWK set is specified by value
				jwkSetSource = new StaticSingletonJWKSetSource(clientID, clientInfo.getOIDCMetadata().getJWKSet());
			} else if (clientInfo.getOIDCMetadata().getJWKSetURI() != null) {
				// The JWK set is specified by URL reference
				URL jwkSetURL;
				try {
					jwkSetURL = clientInfo.getOIDCMetadata().getJWKSetURI().toURL();
				} catch (MalformedURLException e) {
					throw new GeneralException("Invalid jwk set URI: " + e.getMessage(), e);
				}
				jwkSetSource = new RemoteSingletonJWKSetSource(clientID, jwkSetURL, null);
			} else {
				throw new GeneralException("Missing JWK set source");
			}

			return new SignatureKeySelector(expectedIssuer, expectedJWSAlg, jwkSetSource);

		} else if (JWSAlgorithm.Family.HMAC_SHA.contains(expectedJWSAlg)) {

			Secret clientSecret = clientInfo.getSecret();
			if (clientSecret == null) {
				throw new GeneralException("Missing client secret");
			}

			return new ClientSecretSelector(expectedIssuer, expectedJWSAlg, clientSecret);

		} else {
			throw new GeneralException("Unsupported JWS algorithm: " + expectedJWSAlg);
		}
	}


	private static JWEKeySelector createJWEKeySelector(final OIDCProviderMetadata opMetadata,
							   final OIDCClientInformation clientInfo,
							   final JWKSetSource clientJWKSetSource)
		throws GeneralException {

		final JWEAlgorithm expectedJWEAlg = clientInfo.getOIDCMetadata().getIDTokenJWEAlg();
		final EncryptionMethod expectedJWEEnc = clientInfo.getOIDCMetadata().getIDTokenJWEEnc();

		if (expectedJWEAlg == null) {
			// Encrypted ID tokens not expected
			return null;
		}

		if (expectedJWEEnc == null) {
			throw new GeneralException("Missing required ID token JWE encryption method for " + expectedJWEAlg);
		}

		if (opMetadata.getIDTokenJWEAlgs() == null || ! opMetadata.getIDTokenJWEAlgs().contains(expectedJWEAlg)) {
			throw new GeneralException("The OpenID Provider doesn't support " + expectedJWEAlg + " ID tokens");
		}

		if (opMetadata.getIDTokenJWEEncs() == null || ! opMetadata.getIDTokenJWEEncs().contains(expectedJWEEnc)) {
			throw new GeneralException("The OpenID Provider doesn't support " + expectedJWEAlg + " / " + expectedJWEEnc + " ID tokens");
		}

		return new JWEDecryptionKeySelector(clientInfo.getID(), expectedJWEAlg, expectedJWEEnc, clientJWKSetSource);
	}


	/**
	 * Creates a new ID token verifier for the specified OpenID Provider
	 * metadata and OpenID Relying Party registration.
	 *
	 * @param opMetadata The OpenID Provider metadata. Must not be
	 *                   {@code null}.
	 * @param clientInfo The OpenID Relying Party registration. Must not be
	 *                   {@code null}.
	 *
	 * @return The ID token verifier.
	 *
	 * @throws GeneralException If the supplied OpenID Provider metadata or
	 *                          Relying Party metadata are missing a
	 *                          required parameter or inconsistent.
	 */
	public static IDTokenVerifier create(final OIDCProviderMetadata opMetadata,
					     final OIDCClientInformation clientInfo)
		throws GeneralException {

		// Create JWS key selector, unless id_token alg = none
		final JWSKeySelector jwsKeySelector = createJWSKeySelector(opMetadata, clientInfo);

		// Create JWE key selector if encrypted ID tokens are expected
		final JWEKeySelector jweKeySelector = createJWEKeySelector(opMetadata, clientInfo, null);

		return new IDTokenVerifier(opMetadata.getIssuer(), clientInfo.getID(), jwsKeySelector, jweKeySelector);
	}
}
