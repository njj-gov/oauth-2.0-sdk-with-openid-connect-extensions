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


import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.util.JSONUtils;
import com.nimbusds.openid.connect.sdk.federation.policy.language.*;


/**
 * Values set (value) operation.
 *
 * <p>Example policy:
 *
 * <pre>
 * "require_auth_time" : { "value": true }
 * </pre>
 *
 * <p>Input:
 *
 * <pre>
 * "require_auth_time" : false
 * </pre>
 *
 * <p>Result:
 *
 * <pre>
 * "require_auth_time" : true
 * </pre>
 *
 * <p>Related specifications:
 *
 * <ul>
 *     <li>OpenID Connect Federation 1.0, section 4.1.5.
 * </ul>
 */
public class ValueOperation implements PolicyOperation,
	BooleanConfiguration, StringConfiguration, StringListConfiguration,
	UntypedOperation {
	
	
	public static final OperationName NAME = new OperationName("value");
	
	
	private AtomicBoolean isInit = new AtomicBoolean(false);
	
	
	private boolean booleanValue;
	
	
	private String stringValue;
	
	
	private List<String> stringListValue;
	
	
	@Override
	public OperationName getOperationName() {
		return NAME;
	}
	
	
	@Override
	public void configure(final boolean parameter) {
		isInit.set(true);
		this.booleanValue = parameter;
	}
	
	
	@Override
	public void configure(final String parameter) {
		isInit.set(true);
		this.stringValue = parameter;
	}
	
	
	@Override
	public void configure(final List<String> parameter) {
		isInit.set(true);
		this.stringListValue = parameter;
	}
	
	
	@Override
	public void parseConfiguration(final Object jsonEntity) throws ParseException {
		if (jsonEntity instanceof Boolean) {
			configure(JSONUtils.toBoolean(jsonEntity));
		} else if (jsonEntity instanceof String) {
			configure(JSONUtils.toString(jsonEntity));
		} else {
			configure(JSONUtils.toStringList(jsonEntity));
		}
	}
	
	
	@Override
	public boolean getBooleanConfiguration() {
		return booleanValue;
	}
	
	
	@Override
	public String getStringConfiguration() {
		return stringValue;
	}
	
	
	@Override
	public List<String> getStringListConfiguration() {
		return stringListValue;
	}
	
	
	@Override
	public PolicyOperation merge(final PolicyOperation other)
		throws PolicyViolationException {
		
		ValueOperation otherTyped = Utils.castForMerge(other, ValueOperation.class);
		
		if (! isInit.get() || ! otherTyped.isInit.get()) {
			throw new PolicyViolationException("The value operation is not initialized");
		}
		
		if (getStringListConfiguration() != null) {
			
			if (getStringListConfiguration().equals(otherTyped.getStringListConfiguration())) {
				
				ValueOperation copy = new ValueOperation();
				copy.configure(getStringListConfiguration());
				return copy;
			}
			
			throw new PolicyViolationException("Value mismatch");
			
		} else if (getStringConfiguration() != null) {
			
			if (getStringConfiguration().equals(otherTyped.getStringConfiguration())) {
				
				ValueOperation copy = new ValueOperation();
				copy.configure(getStringConfiguration());
				return copy;
			}
			
			throw new PolicyViolationException("Value mismatch");
			
		} else if (getBooleanConfiguration() == otherTyped.getBooleanConfiguration()) {
			
			ValueOperation copy = new ValueOperation();
			copy.configure(getBooleanConfiguration());
			return copy;
		} else {
			throw new PolicyViolationException("Value mismatch");
		}
	}
	
	
	@Override
	public Object apply(final Object value) {
		
		if (! isInit.get()) {
			throw new IllegalStateException("The policy is not initialized");
		}
		
		if (stringListValue != null) {
			return stringListValue;
		}
		if (stringValue != null) {
			return stringValue;
		}
		
		// boolean last because it cannot be null
		return booleanValue;
	}
}