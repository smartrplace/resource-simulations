package org.smartrplace.sim.resource.config;

import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.prototypes.Configuration;

public interface ScheduledSimulationConfig extends Configuration {

	/**
	 * A reference
	 * @return
	 */
	SingleValueResource target();
	
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
