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
package org.smartrplace.sim.resource.config;

import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.prototypes.Configuration;

public interface ScheduledSimulationConfig extends Configuration {

	/**
	 * A reference
	 * @return
	 */
	SingleValueResource target();
	
	/**
	 * Optional. If present, a forecast is generated from the 
	 * simulation data, and written into the schedule subresource of {@link #target()}
	 * with the specified name
	 * @return
	 */
	StringResource forecastSchedule();
	
	/**
	 * Only relevant if {@link #forecastSchedule()} is set. 
	 * Defines the time span for which forecasts are generated. 
	 * @return
	 */
	TimeResource forecastHorizon();
	
	/**
	 * Optional. If present and true, the value of the resource will never
	 * decrease (given all simulation values are non-negative).
	 * @return
	 */
	BooleanResource additive();
	
	/**
	 * Optional; examples:
	 * <ul>
	 *   <li>temperature
	 *   <li>power
	 *   <li>power
	 *   <li>...
	 * </ul>
	 * @return
	 */
	StringResource typePrimary();
	
	/**
	 * Secondary type information, example
	 * <ul>
	 *   <li>outside (for temperature)
	 *   <li>inside (for temperature)
	 *   <li>subphase (for power)
	 * </ul>
	 * @return
	 */
	StringResource typeSecondary();
	
}
