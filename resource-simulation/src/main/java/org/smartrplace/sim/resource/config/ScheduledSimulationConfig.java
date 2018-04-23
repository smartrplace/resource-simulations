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
