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
		if (secondaryType != null)
			sb.append(',').append(' ').append(secondaryType);
		sb.append(']');
		return sb.toString();
	}
	
}
