package com.nimbusds.oauth2.sdk;


import net.jcip.annotations.Immutable;


/**
 * Authorisation {@link Scope} token. This class is immutable.
 *
 * @author Vladimir Dzhuvinov
 * @version $version$ (2013-01-15)
 */
@Immutable
public final class ScopeToken extends Identifier {


	/**
	 * Enumeration of the {@link ScopeToken scope token} requirements 
	 * for matching authorisation requests.
	 */
	public static enum Requirement {
	
		/**
		 * The token must be present in the {@link Scope} parameter.
		 */
		REQUIRED,
		
		
		/**
		 * The token may be optionally included in the {@link Scope}
		 * parameter.
		 */
		OPTIONAL
	}


	/**
	 * Optional requirement.
	 */
	private final ScopeToken.Requirement requirement;


	/**
	 * Creates a new scope token.
	 *
	 * @param value       The scope token value. Must not be {@code null}
	 *                    or empty string.
	 * @param requirement The requirement, {@code null} if not specified.
	 */
	private ScopeToken(final String value, final ScopeToken.Requirement requirement) {
	
		super(value);

		this.requirement = requirement;
	}


	/**
	 * Creates a new scope token. The requirement is not specified.
	 *
	 * @param value The scope token value. Must not be {@code null} or
	 *              empty string.
	 */
	public ScopeToken(final String value) {
	
		this(value, null);
	}

		
	/**
	 * Gets the requirement of this scope token.
	 *
	 * @return The requirement, {@code null} if not specified.
	 */
	public Requirement getRequirement() {

		return requirement;
	}


	@Override
	public boolean equals(final Object object) {

		return object != null &&
		       object instanceof ScopeToken &&
		       this.toString().equals(object.toString());
	}
}