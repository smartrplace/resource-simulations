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
