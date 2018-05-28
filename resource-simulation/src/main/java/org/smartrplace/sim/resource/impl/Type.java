/**
 * Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartrplace.sim.resource.impl;

import java.util.Objects;

public class Type {

	public final String primaryType;
	public final String secondaryType;
	
	public Type(String primaryType, String secondaryType) {
		this.primaryType = Objects.requireNonNull(primaryType);
		this.secondaryType = secondaryType;
	}
	
	static final String PRIMARY_TEMPERATURE = "temperature";
	static final String PRIMARY_POWER = "power";
	static final String PRIMARY_CURRENT = "current";
	static final String PRIMARY_VOLTAGE = "voltage";
	static final String PRIMARY_REACTIVE_POWER = "reactive_power";
	
	static final String SECONDARY_TEMP_OUTSIDE = "outside";
	static final String SECONDARY_TEMP_INSIDE = "inside";
	static final String SECONDARY_POWER_PHASE = "subphase";		

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof Type))
			return false;
		final Type other = (Type) obj;
		return Objects.equals(this.primaryType, other.primaryType) && Objects.equals(this.secondaryType, other.secondaryType);
	}
	
	@Override
	public int hashCode() {
		return secondaryType != null ? Objects.hash(primaryType, secondaryType) : primaryType.hashCode();
	}
	
	@Override
	public String toString() {
		final StringBuilder sb =new StringBuilder();
		sb.append("Type [").append(primaryType);
		sb.append(',').append(' ').append(secondaryType != null ? secondaryType : "default");
		sb.append(']');
		return sb.toString();
	}
	
}
