package org.smartrplace.sim.resource.impl;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.smartrplace.sim.resource.config.ScheduledSimulationConfig;

public class ConfigPattern extends ResourcePattern<ScheduledSimulationConfig> {

	public ConfigPattern(Resource match) {
		super(match);
	}

	public final SingleValueResource target = model.target();
	
	@ChangeListener(valueListener=true)
	@Existence(required=CreateMode.OPTIONAL)
	public final StringResource typePrimary = model.typePrimary();
	
	@ChangeListener(valueListener=true)
	@Existence(required=CreateMode.OPTIONAL)
	public final StringResource typeSecondary = model.typeSecondary();
	
	@ChangeListener(valueListener=true)
	@Existence(required=CreateMode.OPTIONAL)
	public final StringResource forecast = model.forecastSchedule();
	
	@Existence(required=CreateMode.OPTIONAL)
	public final TimeResource forecastHorizon = model.forecastHorizon();
	
	@Existence(required=CreateMode.OPTIONAL)
	public final BooleanResource additive = model.additive();
	
}
