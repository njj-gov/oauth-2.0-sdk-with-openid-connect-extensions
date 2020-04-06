/*
 * oauth2-oidc-sdk
 *
 * Copyright 2012-2020, Connect2id Ltd and contributors.
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

package com.nimbusds.openid.connect.sdk.federation.policy;


import java.util.*;

import net.minidev.json.JSONAware;
import net.minidev.json.JSONObject;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyOperation;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyViolationException;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.DefaultPolicyOperationCombinationValidator;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.DefaultPolicyOperationFactory;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.PolicyOperationCombinationValidator;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.PolicyOperationFactory;


/**
 * Policy for a federation entity metadata.
 *
 * <p>Example:
 *
 * <pre>
 * {
 *     "scopes": {
 *         "subset_of": ["openid", "eduperson", "phone"],
 *         "superset_of": ["openid"],
 *         "default": ["openid", "eduperson"]},
 *     "id_token_signed_response_alg": {
 *         "one_of": ["ES256", "ES384", "ES512"]},
 *     "contacts": {
 *         "add": "helpdesk@federation.example.org"},
 *     "application_type": {"value": "web"}
 * }
 * </pre>
 *
 * <p>Related specifications:
 *
 * <ul>
 *     <li>OpenID Connect Federation 1.0, section 4.1.
 * </ul>
 */
public class MetadataPolicy implements JSONAware {
	
	
	/**
	 * The policy entries, keyed by metadata parameter name.
	 */
	private final Map<String,List<PolicyOperation>> entries = new LinkedHashMap<>();
	
	
	/**
	 * Puts a policy entry for a metadata parameter.
	 *
	 * @param parameterName   The parameter name. Must not be {@code null}.
	 * @param policyOperation The policy operation for the parameter,
	 *                        {@code null} if none.
	 */
	public void put(final String parameterName, final PolicyOperation policyOperation) {
		put(new MetadataPolicyEntry(parameterName, Collections.singletonList(policyOperation)));
	}
	
	
	/**
	 * Puts a policy entry for a metadata parameter.
	 *
	 * @param parameterName    The parameter name. Must not be {@code null}.
	 * @param policyOperations The ordered policy operations for the
	 *                         parameter, {@code null} if none.
	 */
	public void put(final String parameterName, final List<PolicyOperation> policyOperations) {
		put(new MetadataPolicyEntry(parameterName, policyOperations));
	}
	
	
	/**
	 * Puts a policy entry for a metadata parameter.
	 *
	 * @param entry The policy entry. Must not be {@code null}.
	 */
	public void put(final MetadataPolicyEntry entry) {
		entries.put(entry.getKey(), entry.getValue());
	}
	
	
	/**
	 * Gets the policy operations for the specified metadata parameter
	 * name.
	 *
	 * @param parameterName The parameter name. Must not be {@code null}.
	 *
	 * @return The ordered policy operations for the parameter,
	 *         {@code null} if none.
	 */
	public List<PolicyOperation> get(final String parameterName) {

		return entries.get(parameterName);
	}
	
	
	/**
	 * Gets the policy entry for the specified metadata parameter name.
	 *
	 * @param parameterName The parameter name. Must not be {@code null}.
	 *
	 * @return The policy entry for the parameter, {@code null} if none.
	 */
	public MetadataPolicyEntry getEntry(final String parameterName) {
		
		List<PolicyOperation> policyOperations = entries.get(parameterName);
		
		if (policyOperations == null) {
			return null;
		}
		
		return new MetadataPolicyEntry(parameterName, policyOperations);
	}
	
	
	/**
	 * Gets the policy entries set.
	 *
	 * @return The policy entries set.
	 */
	public Set<MetadataPolicyEntry> entrySet() {
		
		Set<MetadataPolicyEntry> set = new LinkedHashSet<>();
		for (Map.Entry<String,List<PolicyOperation>> en: entries.entrySet()) {
			set.add(new MetadataPolicyEntry(en.getKey(), en.getValue()));
		}
		return set;
	}
	
	
	/**
	 * Removes a policy entry.
	 *
	 * @param parameterName The parameter name. Must not be {@code null}.
	 *
	 * @return The ordered policy operations for the removed parameter,
	 *         {@code null} if not found.
	 */
	public List<PolicyOperation> remove(final String parameterName) {
		
		return entries.remove(parameterName);
	}
	
	
	/**
	 * Returns a JSON object representation of this metadata policy.
	 *
	 * @return The JSON object.
	 */
	public Map<String,Object> toJSONObject() {
		
		Map<String,Object> jsonObject = new LinkedHashMap<>();
		
		for (MetadataPolicyEntry en: entrySet()) {
			jsonObject.put(en.getKey(), en.toJSONObject());
		}
		
		return jsonObject;
	}
	
	
	@Override
	public String toJSONString() {
		return JSONObject.toJSONString(toJSONObject());
	}
	
	
	/**
	 * Parses a policy for a federation entity metadata. This method is
	 * intended for policies including non-standard
	 * {@link PolicyOperation}s.
	 *
	 * @param policySpec           The JSON object for the policy
	 *                             specification. Must not be {@code null}.
	 * @param factory              The policy operation factory. Must not
	 *                             be {@code null}.
	 * @param combinationValidator The policy operation combination
	 *                             validator. Must not be {@code null}.
	 *
	 * @return The metadata policy.
	 *
	 * @throws ParseException           On JSON parsing exception.
	 * @throws PolicyViolationException On a policy violation.
	 */
	public static MetadataPolicy parse(final Map<String,Object> policySpec,
					   final PolicyOperationFactory factory,
					   final PolicyOperationCombinationValidator combinationValidator)
		throws ParseException, PolicyViolationException {
		
		MetadataPolicy metadataPolicy = new MetadataPolicy();
		
		for (String parameterName: policySpec.keySet()) {
			Map<String,Object> entrySpec = getJSONObject(policySpec, parameterName);
			metadataPolicy.put(MetadataPolicyEntry.parse(parameterName, entrySpec, factory, combinationValidator));
		}
		
		return metadataPolicy;
	}
	
	
	private static Map<String,Object> getJSONObject(final Map<String,Object> o, final String key)
		throws ParseException {
		
		Object value = o.get(key);
		
		if (value == null) {
			throw new ParseException("JSON object member with key \"" + key + "\" has null value");
		}
		
		if (value instanceof Map) {
			return (Map<String,Object>)value;
		}
		
		throw new ParseException("Unexpected type of JSON object member with key \"" + key + "\"");
	}
	
	
	/**
	 * Parses a policy for a federation entity metadata. This method is
	 * intended for policies with standard {@link PolicyOperation}s only.
	 * Uses the default {@link DefaultPolicyOperationFactory policy
	 * operation} and {@link DefaultPolicyOperationCombinationValidator
	 * policy combination validator} factories.
	 *
	 * @param policySpec The JSON object string for the policy
	 *                   specification. Must not be {@code null}.
	 *
	 * @return The metadata policy.
	 *
	 * @throws ParseException           On JSON parsing exception.
	 * @throws PolicyViolationException On a policy violation.
	 */
	public static MetadataPolicy parse(final Map<String,Object> policySpec)
		throws ParseException, PolicyViolationException {
		
		return parse(policySpec,
			MetadataPolicyEntry.DEFAULT_POLICY_OPERATION_FACTORY,
			MetadataPolicyEntry.DEFAULT_POLICY_COMBINATION_VALIDATOR);
	}
	
	
	/**
	 * Parses a policy for a federation entity metadata. This method is
	 * intended for policies including non-standard
	 * {@link PolicyOperation}s.
	 *
	 * @param policySpec           The JSON object for the policy
	 *                             specification. Must not be {@code null}.
	 * @param factory              The policy operation factory. Must not
	 *                             be {@code null}.
	 * @param combinationValidator The policy operation combination
	 *                             validator. Must not be {@code null}.
	 *
	 * @return The metadata policy.
	 *
	 * @throws ParseException           On JSON parsing exception.
	 * @throws PolicyViolationException On a policy violation.
	 */
	public static MetadataPolicy parse(final String policySpec,
					   final PolicyOperationFactory factory,
					   final PolicyOperationCombinationValidator combinationValidator)
		throws ParseException, PolicyViolationException {
		
		return parse(JSONObjectUtils.parseKeepingOrder(policySpec), factory, combinationValidator);
	}
	
	
	/**
	 * Parses a policy for a federation entity metadata. This method is
	 * intended for policies with standard {@link PolicyOperation}s only.
	 * Uses the default {@link DefaultPolicyOperationFactory policy
	 * operation} and {@link DefaultPolicyOperationCombinationValidator
	 * policy combination validator} factories.
	 *
	 * @param policySpec The JSON object string for the policy
	 *                   specification. Must not be {@code null}.
	 *
	 * @return The metadata policy.
	 *
	 * @throws ParseException           On JSON parsing exception.
	 * @throws PolicyViolationException On a policy violation.
	 */
	public static MetadataPolicy parse(final String policySpec)
		throws ParseException, PolicyViolationException {
		
		return parse(policySpec,
			MetadataPolicyEntry.DEFAULT_POLICY_OPERATION_FACTORY,
			MetadataPolicyEntry.DEFAULT_POLICY_COMBINATION_VALIDATOR);
	}
}